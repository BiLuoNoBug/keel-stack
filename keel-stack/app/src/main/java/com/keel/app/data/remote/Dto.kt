package com.keel.app.data.remote

/** 服务端统一响应包装：{ code, message, data }，code = 0 表示成功 */
data class ApiResponse<T>(
    val code: Int = -1,
    val message: String = "",
    val data: T? = null
) {
    val ok: Boolean get() = code == 0
}

/** 分页结果 */
data class PageDto<T>(
    val list: List<T>? = null,
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20
)

/* ------------------------------------------------------------ 示例模块：条目 item */

data class ItemDto(
    val id: Int = 0,
    val title: String = "",
    val category: String = "",
    val content: String = "",
    val coverUrl: String? = null,
    val status: String = "TODO",
    val statusText: String = "",
    val allowNext: List<String>? = null,
    val priority: String = "NORMAL",
    val published: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

/** 按状态计数（控制台看板 / 首页概览） */
data class StatusCountDto(
    val status: String = "",
    val statusText: String = "",
    val count: Int = 0
)

/** 新增 / 编辑条目的请求体，字段与后端 itemInput 校验规则对齐 */
data class ItemBody(
    val title: String,
    val category: String = "默认分类",
    val content: String = "",
    val coverUrl: String? = null,
    val priority: String = "NORMAL",
    val published: Boolean = true,
    val sortOrder: Int = 0
)

/** 状态流转请求体 */
data class StatusRequest(val status: String)

/* ------------------------------------------------------------ 图片上传 */

data class MediaDto(
    val id: Int = 0,
    val filename: String = "",
    val url: String = "",
    val mime: String = "",
    val size: Long = 0,
    val refCount: Int = 0,
    val createdAt: String = ""
)

/* ------------------------------------------------------------ 站内消息 */

data class NotificationDto(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val link: String = "",
    val level: String = "INFO",
    val target: String = "ALL",
    val pushed: Boolean = false,
    val pushedAt: String? = null,
    val pushCount: Int = 0,
    val createdAt: String = ""
)

data class NotificationListDto(val list: List<NotificationDto>? = null)

/** 发消息请求体（目标值域：ALL / CLIENT / ADMIN） */
data class NotificationBody(
    val title: String,
    val content: String,
    val level: String = "INFO",
    val target: String = "CLIENT"
)

/* ------------------------------------------------------------ 版本更新 */

data class ReleaseDto(
    val id: Int = 0,
    val versionCode: Int = 0,
    val versionName: String = "",
    val changelog: String = "",
    val downloadUrl: String = "",
    val forceUpdate: Boolean = false,
    val published: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class UpdateCheckDto(
    val hasUpdate: Boolean = false,
    val currentVersionCode: Int = 0,
    val latest: ReleaseDto? = null,
    val release: ReleaseDto? = null
)
