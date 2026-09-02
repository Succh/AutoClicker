package com.succh.unifeed.data.rss

import com.succh.unifeed.data.model.ParsedEntry
import com.succh.unifeed.data.model.ParsedFeed
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.concurrent.TimeUnit

/**
 * 轻量级 RSS/Atom 解析器
 * 基于 XmlPullParser，无需额外依赖
 */
class RssParser(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()) {

    companion object {
        private const val MAX_ENTRIES_PER_FEED = 500
    }

    /**
     * 抓取并解析 RSS/Atom 订阅源
     */
    suspend fun fetch(url: String): ParsedFeed {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "UniFeed/1.0 (RSS Reader; +https://github.com/Succh/AutoClicker)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }
                val body = response.body?.string() ?: throw Exception("Empty body")
                parse(body, url)
            }
        }
    }

    /**
     * 从 XML 字符串解析订阅源
     */
    fun parse(xml: String, sourceUrl: String): ParsedFeed {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var channelTitle = ""
        var channelLink = ""
        var channelDesc = ""
        var isAtom = false
        val entries = mutableListOf<ParsedEntry>()

        var currentEntry: ParsedEntry? = null
        var currentText = StringBuilder()
        var inItem = false
        var inChannel = false
        var tagName = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_DOCUMENT -> {
                    // 尝试嗅探 feed 类型
                }

                XmlPullParser.START_TAG -> {
                    tagName = parser.name
                    currentText = StringBuilder()

                    if (tagName == "feed" && !isAtom && channelTitle.isEmpty()) {
                        isAtom = true
                    }
                    if (tagName == "item" || tagName == "entry") {
                        inItem = true
                        currentEntry = ParsedEntry(guid = "", title = "")
                    }
                    if (tagName == "channel") inChannel = true
                }

                XmlPullParser.TEXT -> {
                    currentText.append(parser.text ?: "")
                }

                XmlPullParser.END_TAG -> {
                    val text = currentText.toString().trim()
                    val tagname = parser.name

                    if (inItem && currentEntry != null) {
                        when (tagname) {
                            "title" -> if (currentEntry!!.title.isEmpty()) currentEntry = currentEntry!!.copy(title = text)
                            "link" -> if (currentEntry!!.link == null) currentEntry = currentEntry!!.copy(link = text)
                            "guid", "id" -> if (currentEntry!!.guid.isEmpty()) currentEntry = currentEntry!!.copy(guid = text)
                            "description", "summary", "content" -> if (currentEntry!!.content == null) currentEntry = currentEntry!!.copy(content = text)
                            "pubDate", "published", "updated" -> if (currentEntry!!.publishedAt == 0L) {
                                currentEntry = currentEntry!!.copy(publishedAt = parseDate(text))
                            }
                            "author", "creator" -> if (currentEntry!!.author == null) currentEntry = currentEntry!!.copy(author = text)
                            "item", "entry" -> {
                                if (currentEntry!!.guid.isEmpty()) currentEntry = currentEntry!!.copy(guid = currentEntry!!.link ?: sourceUrl + "#" + currentEntry!!.title)
                                entries.add(currentEntry!!)
                                currentEntry = null
                                inItem = false
                            }
                        }
                    } else {
                        when (tagname) {
                            "title" -> if (inChannel && channelTitle.isEmpty()) channelTitle = text
                            "link" -> if (inChannel && channelLink.isEmpty()) channelLink = text
                            "description", "subtitle" -> if (inChannel && channelDesc.isEmpty()) channelDesc = text
                            "channel" -> inChannel = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (channelTitle.isEmpty()) {
            // 可能是 Atom feed 的 <title> 在 <feed> 下，此时 inChannel 为 false
            // 已通过根级 title 兜底
        }

        return ParsedFeed(
            title = channelTitle.ifEmpty { sourceUrl },
            url = sourceUrl,
            siteUrl = channelLink.ifEmpty { null },
            description = channelDesc.ifEmpty { null },
            entries = entries.take(MAX_ENTRIES_PER_FEED)
        )
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        return try {
            // RFC 1123 / RFC 3339 / ISO 8601 尝试
            val formats = listOf(
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "EEE, dd MMM yyyy HH:mm:ss zzz"
            )
            for (format in formats) {
                try {
                    return java.text.SimpleDateFormat(format, java.util.Locale.US)
                        .parse(dateStr)
                        ?.time ?: System.currentTimeMillis()
                } catch (_: Exception) {}
            }
            // 数字时间戳
            dateStr.toLongOrNull()?.let { return it * 1000 }
            System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
