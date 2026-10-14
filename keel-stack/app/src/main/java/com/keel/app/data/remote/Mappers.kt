package com.keel.app.data.remote

import com.keel.app.data.AppNotice
import com.keel.app.data.Item
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 状态枚举 -> 中文文案，仅在服务端没下发 statusText 时兜底 */
object ItemStatus {
    private val text = mapOf("TODO" to "待处理", "DOING" to "进行中", "DONE" to "已完成")
    fun of(status: String): String = text[status] ?: status
}

/** 优先级枚举 -> 中文文案 */
object ItemPriority {
    private val text = mapOf("LOW" to "低", "NORMAL" to "普通", "HIGH" to "高")
    fun of(priority: String): String = text[priority] ?: priority
}

/** "2026-10-07 14:32:00" -> "今天 14:32" / "8月30日 18:20" */
private fun formatTime(raw: String): String {
    if (raw.isBlank()) return ""
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val date = parser.parse(raw) ?: return@runCatching raw
        val clock = SimpleDateFormat("HH:mm", Locale.CHINA).format(date)
        val calendar = Calendar.getInstance()
        val today = calendar.clone() as Calendar
        val target = calendar.clone() as Calendar
        target.time = date
        val sameDay = today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
        if (sameDay) "今天 $clock"
        else "${target.get(Calendar.MONTH) + 1}月${target.get(Calendar.DAY_OF_MONTH)}日 $clock"
    }.getOrDefault(raw)
}

/* ------------------------------------------------------------ DTO -> 模型 */

fun ItemDto.toModel(): Item = Item(
    id = id,
    title = title,
    category = category,
    content = content,
    coverUrl = coverUrl,
    status = status,
    statusText = statusText.ifBlank { ItemStatus.of(status) },
    allowNext = allowNext ?: emptyList(),
    priority = priority,
    published = published,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun NotificationDto.toModel(): AppNotice = AppNotice(
    id = id,
    title = title,
    content = content,
    important = level == "IMPORTANT",
    date = formatTime(createdAt),
    link = link
)
