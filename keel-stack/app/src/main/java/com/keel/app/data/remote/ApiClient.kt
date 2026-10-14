package com.keel.app.data.remote

import com.keel.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 服务端地址管理。
 *
 * 默认值来自 BuildConfig.DEFAULT_API_HOST —— 编译时注入，换地址可用
 * -PapiHost=... 覆盖，App 内「服务地址」页也能改（改完重建 Retrofit）。
 *
 * 真机连本机 server 时，可用 `adb reverse tcp:8080 tcp:8080`
 * 把手机的 127.0.0.1:8080 映射到电脑。
 */
object ApiClient {

    val DEFAULT_HOST: String = BuildConfig.DEFAULT_API_HOST

    @Volatile
    private var host: String = DEFAULT_HOST

    @Volatile
    private var retrofit: Retrofit? = null

    val api: ApiService
        get() = instance().create(ApiService::class.java)

    fun currentHost(): String = host

    /** 上传图片，返回后端给的相对地址；mime 必须是具体类型（如 image/jpeg），不能用通配 */
    suspend fun uploadImage(bytes: ByteArray, filename: String, mime: String = "image/jpeg"): String? {
        val part = MultipartBody.Part.createFormData(
            "file", filename, bytes.toRequestBody(mime.toMediaTypeOrNull())
        )
        return api.uploadImage(part).data?.url
    }

    /** 切换服务地址后重建 Retrofit */
    fun updateHost(newHost: String) {
        val normalized = newHost.trim().trimEnd('/').ifBlank { DEFAULT_HOST }
        if (normalized == host) return
        host = normalized
        retrofit = null
    }

    private fun instance(): Retrofit = retrofit ?: synchronized(this) {
        retrofit ?: build().also { retrofit = it }
    }

    /** 复用同一个 OkHttp：接口、上传、下载共用连接池 */
    private val okHttp: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("x-device-id", "app")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    /**
     * 下载文件（更新包）到本地，边下边回调进度。
     * onProgress 在 IO 线程触发，调用方需自己切回主线程再更新 UI 状态。
     */
    suspend fun download(url: String, target: File, onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }) {
        withContext(Dispatchers.IO) {
            // 大文件放宽读取超时
            val client = okHttp.newBuilder().readTimeout(60, TimeUnit.SECONDS).build()
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("空响应")
                val total = body.contentLength()
                target.parentFile?.mkdirs()
                var read = 0L
                val buffer = ByteArray(16 * 1024)
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            read += n
                            onProgress(read, total)
                        }
                    }
                }
            }
        }
    }

    private fun build(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("$host/api/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

/** 后端下发的是 /uploads/xxx.jpg 这样的相对路径，这里补成可加载的绝对地址 */
object MediaUrl {
    fun of(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return when {
            path.startsWith("http://") || path.startsWith("https://") -> path
            path.startsWith("/") -> ApiClient.currentHost() + path
            else -> ApiClient.currentHost() + "/" + path
        }
    }
}
