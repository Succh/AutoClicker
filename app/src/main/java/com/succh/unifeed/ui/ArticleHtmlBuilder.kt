package com.succh.unifeed.ui

import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 将 RSS 条目正文转成带样式的 HTML，供 ReaderScreen 的 WebView 渲染。
 * 保留 <img> 图片，修复 data-src 等懒加载属性，纯文本自动转义。
 */
object ArticleHtmlBuilder {

    private val CSS = """
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-size: 17px; line-height: 1.7; color: #1c1c1e; background: #fff; padding: 16px 16px 40px; word-wrap: break-word; }
img { max-width: 100%; height: auto; display: block; margin: 16px auto; border-radius: 8px; }
a { color: #007aff; text-decoration: none; }
p { margin: 12px 0; }
h1, h2, h3, h4 { margin: 20px 0 10px 0; }
pre, code { font-family: Menlo, Consolas, monospace; font-size: 14px; background: #f5f5f7; border-radius: 6px; }
pre { padding: 16px; overflow-x: auto; margin: 16px 0; }
code { padding: 2px 6px; }
blockquote { border-left: 4px solid #e5e5ea; margin: 16px 0; padding: 8px 16px; color: #636366; }
table { width: 100%; border-collapse: collapse; margin: 16px 0; }
th, td { border: 1px solid #e5e5ea; padding: 8px 12px; }
ul, ol { margin: 12px 0; padding-left: 24px; }
.meta { font-size: 14px; color: #636366; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid #e5e5ea; }
""".trimIndent()

    fun build(title: String, author: String?, link: String?, publishedAt: Long, content: String): String {
        val meta = buildString {
            if (!author.isNullOrBlank()) append(escape(author))
            if (publishedAt > 0L) {
                if (isNotEmpty()) append(" · ")
                append(formatFullTime(publishedAt))
            }
            if (!link.isNullOrBlank()) {
                if (isNotEmpty()) append(" · ")
                append("<a href='").append(escape(link)).append("'>").append(escape(link)).append("</a>")
            }
        }
        val body = normalize(content)
        return """<!DOCTYPE html>
<html lang='zh'><head>
<meta charset='UTF-8'>
<meta name='viewport' content='width=device-width, initial-scale=1.0'>
<style>$CSS</style>
</head><body>
<div class='meta'>$meta</div>
$body
</body></html>"""
    }

    private fun normalize(html: String): String {
        if (html.isBlank()) return "<p>（无正文内容）</p>"
        return try {
            if (html.trimStart().startsWith("<")) {
                val doc = Jsoup.parse(html)
                doc.select("script,style,ins,iframe,noscript,form").remove()
                doc.select("img").forEach { img ->
                    if (img.attr("src").isBlank()) {
                        for (k in listOf("data-src", "data-original", "data-lazy-src", "data-url", "data-actualsrc")) {
                            val v = img.attr(k)
                            if (v.isNotBlank()) {
                                img.attr("src", v)
                                break
                            }
                        }
                    }
                }
                doc.body().html()
            } else {
                "<p>${escape(html)}</p>"
            }
        } catch (_: Exception) {
            "<p>${escape(html)}</p>"
        }
    }

    private fun escape(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "\u0026quot;")
            .replace("'", "&#39;")
            .replace("\n", "<br>")
    }

    private fun formatFullTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
        } catch (_: Exception) {
            ""
        }
    }
}
