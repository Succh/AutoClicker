package com.succh.unifeed.ui.theme

/**
 * 将时间戳格式化为中文相对时间，例如「2小时前」「3天前」。
 *
 * @param timestamp 毫秒时间戳；0 或负数视为无效，返回空串
 * @param now 当前时间，便于单测注入
 */
fun formatRelativeTimeZh(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    if (timestamp <= 0L) return ""
    val diff = now - timestamp
    if (diff < 0L) return "刚刚"

    val seconds = diff / 1000L
    val minutes = seconds / 60L
    val hours = minutes / 60L
    val days = hours / 24L

    return when {
        seconds < 60L -> "刚刚"
        minutes < 60L -> "${minutes}分钟前"
        hours < 24L -> "${hours}小时前"
        days < 7L -> "${days}天前"
        days < 30L -> "${days / 7L}周前"
        days < 365L -> "${days / 30L}个月前"
        else -> "${days / 365L}年前"
    }
}
