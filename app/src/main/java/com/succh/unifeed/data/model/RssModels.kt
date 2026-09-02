package com.succh.unifeed.data.model

// 解析后的 RSS 频道
 data class ParsedFeed(
    val title: String,
    val url: String,
    val siteUrl: String? = null,
    val description: String? = null,
    val entries: List<ParsedEntry>
)

// 解析后的文章条目
 data class ParsedEntry(
    val guid: String,
    val title: String,
    val link: String? = null,
    val content: String? = null,
    val summary: String? = null,
    val author: String? = null,
    val publishedAt: Long = System.currentTimeMillis()
)
