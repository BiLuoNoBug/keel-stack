package com.keel.app.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 客户端接口（App 用）。后端不做登录，用 x-device-id 区分设备。
 * 契约全部挂在 /api/app/** 下，统一返回 { code, message, data }。
 */
interface ApiService {

    /* ---------------------------------------------------- 示例模块：条目 item */

    @GET("app/items")
    suspend fun items(
        @Query("category") category: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("status") status: String? = null,
        @Query("published") published: Boolean? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): ApiResponse<PageDto<ItemDto>>

    @GET("app/items/{id}")
    suspend fun item(@Path("id") id: Int): ApiResponse<ItemDto>

    @POST("app/items")
    suspend fun createItem(@Body body: ItemBody): ApiResponse<ItemDto>

    @PUT("app/items/{id}")
    suspend fun updateItem(@Path("id") id: Int, @Body body: ItemBody): ApiResponse<ItemDto>

    @PATCH("app/items/{id}/status")
    suspend fun updateItemStatus(@Path("id") id: Int, @Body body: StatusRequest): ApiResponse<ItemDto>

    @DELETE("app/items/{id}")
    suspend fun deleteItem(@Path("id") id: Int): ApiResponse<Unit>

    @GET("app/items/categories")
    suspend fun itemCategories(): ApiResponse<List<String>>

    @GET("app/items/status/count")
    suspend fun itemStatusCount(): ApiResponse<List<StatusCountDto>>

    /* ---------------------------------------------------- 图片上传 */

    @Multipart
    @POST("app/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): ApiResponse<MediaDto>

    /* ---------------------------------------------------- 站内消息与版本 */

    /** 取值域与服务端一致：ALL / CLIENT / ADMIN；App 侧固定传 CLIENT */
    @GET("app/notifications")
    suspend fun notifications(@Query("target") target: String): ApiResponse<NotificationListDto>

    @POST("app/notifications")
    suspend fun createNotification(@Body body: NotificationBody): ApiResponse<NotificationDto>

    @GET("app/releases/latest")
    suspend fun checkUpdate(@Query("versionCode") versionCode: Int): ApiResponse<UpdateCheckDto>
}
