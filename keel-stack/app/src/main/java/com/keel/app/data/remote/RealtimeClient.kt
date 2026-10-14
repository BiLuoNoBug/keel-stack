package com.keel.app.data.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URI

/**
 * 服务端的实时通道。
 *
 * 连上后 emit('subscribe', { role, deviceId }) 订阅房间；两端各自关心的事件：
 *   item:changed / item:status —— 条目增删改与状态流转，列表自动刷新
 *   notification:new          —— 站内消息，App 收到后弹顶部横幅
 *   release:new               —— 有新版本，主动查一次更新
 */
object RealtimeClient {

    private const val TAG = "RealtimeClient"

    interface Listener {
        fun onConnected()
        fun onDisconnected()
        /** event 为 item:changed / item:status / notification:new / release:new */
        fun onEvent(event: String, payload: JSONObject?)
    }

    private var socket: Socket? = null
    private var listener: Listener? = null
    private var role: String = "client"

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * socket.io 的回调跑在自建的 EventThread 上，
     * 而 Compose 的 mutableStateOf 必须在主线程写，否则会抛
     * "Reading a state that was created after the snapshot was taken"。
     */
    private fun post(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    val isConnected: Boolean get() = socket?.connected() == true

    fun bind(listener: Listener) {
        this.listener = listener
    }

    fun connect(role: String, host: String) {
        this.role = role
        if (socket?.connected() == true) return
        disconnect()

        runCatching {
            val options = IO.Options().apply {
                // 兼容性优先：先用 HTTP 长轮询建连，服务端支持再升级到 websocket。
                // 只写 websocket 时，部分路由器 / 运营商网络会把升级请求拦掉，
                // 表面看网络是通的，但实时推送一条都收不到。
                transports = arrayOf("polling", "websocket")
                reconnection = true
                forceNew = true
            }
            val created = IO.socket(URI.create(host), options)

            created.on(Socket.EVENT_CONNECT) {
                Log.i(TAG, "已连接 $host")
                post {
                    listener?.onConnected()
                    created.emit("subscribe", JSONObject().put("role", role).put("deviceId", "app"))
                }
            }
            created.on(Socket.EVENT_DISCONNECT) { args ->
                Log.w(TAG, "连接断开：${args.firstOrNull()}")
                post { listener?.onDisconnected() }
            }
            created.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.w(TAG, "连接失败：${args.firstOrNull()}")
            }

            listOf("item:changed", "item:status", "notification:new", "release:new").forEach { event ->
                created.on(event) { args ->
                    val payload = args.firstOrNull() as? JSONObject
                    Log.i(TAG, "收到事件 $event")
                    post { listener?.onEvent(event, payload) }
                }
            }

            socket = created
            created.connect()
        }.onFailure { Log.w(TAG, "初始化失败：${it.message}") }
    }

    fun disconnect() {
        runCatching { socket?.disconnect(); socket?.off() }
        socket = null
    }
}
