package com.keel.app.data

/**
 * 双角色壳：同一台设备可以在「客户端 / 管理端」之间切换。
 * 角色决定主题主色、底部导航与订阅的实时房间，数据仍是同一份。
 */
enum class Role { CLIENT, ADMIN }

/**
 * 示例业务模型：条目（item）。
 *
 * 字段与服务端 items 表 / ItemDto 一一对应；status 只存英文枚举，
 * 中文文案用服务端下发的 statusText，允许的下一步用 allowNext 驱动按钮。
 */
data class Item(
    val id: Int,
    val title: String,
    val category: String,
    val content: String = "",
    val coverUrl: String? = null,
    val status: String = "TODO",
    val statusText: String = "",
    /** 当前状态合法的下一步状态列表，来自服务端状态机 */
    val allowNext: List<String> = emptyList(),
    val priority: String = "NORMAL",
    val published: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

/** 后台控制台推过来的站内消息 */
data class AppNotice(
    val id: Int,
    val title: String,
    val content: String,
    val important: Boolean = false,
    val date: String = "",
    /** 关联的业务标识（例如条目的 id），非空时点通知可以直接跳到对应页面 */
    val link: String = ""
)

/** 版本更新信息 */
data class UpdateInfo(
    val hasUpdate: Boolean,
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val downloadUrl: String,
    val forceUpdate: Boolean
)
