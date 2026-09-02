package com.succh.unifeed.ui

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 将 RSS 条目正文转成带样式的 HTML，供 ReaderScreen 的 WebView 渲染。
 * 支持阅读主题 / 字号 / 行距 / 图片开关 / 衬线字体 / 两端对齐等个性化设置。
 */
object ArticleHtmlBuilder {

    private data class Palette(
        val bg: String, val fg: String, val muted: String,
        val border: String, val codeBg: String, val link: String
    )

    private fun palette(theme: ReaderTheme): Palette = when (theme) {
        ReaderTheme.LIGHT -> Palette("#ffffff", "#1c1c1e", "#8a8a8e", "#e5e5ea", "#f5f5f7", "#007aff")
        ReaderTheme.SEPIA -> Palette("#faf6ef", "#3d3a34", "#8a8378", "#e2dccb", "#f0ead9", "#8b5e3c")
        ReaderTheme.DARK -> Palette("#1c1c1e", "#e8e8ed", "#98989f", "#3a3a3c", "#2c2c2e", "#4da3ff")
    }

    private fun buildCss(
        theme: ReaderTheme,
        fontSize: Float,
        lineHeight: Float,
        showImages: Boolean,
        serif: Boolean,
        justify: Boolean
    ): String {
        val p = palette(theme)
        val fontFamily = if (serif) {
            "Georgia, 'Times New Roman', 'Songti SC', SimSun, serif"
        } else {
            "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', sans-serif"
        }
        val textAlign = if (justify) "text-align: justify;" else ""
        val imgRule = if (showImages) {
            "img { max-width: 100%; height: auto; display: block; margin: 16px auto; border-radius: 8px; }"
        } else {
            "img { display: none !important; }"
        }
        return """
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: $fontFamily; font-size: ${fontSize}px; line-height: $lineHeight; color: ${p.fg}; background: ${p.bg}; padding: 16px 16px 40px; word-wrap: break-word; $textAlign -webkit-text-size-adjust: 100%; }
$imgRule
a { color: ${p.link}; text-decoration: none; }
p { margin: 12px 0; }
h1, h2, h3, h4 { margin: 20px 0 10px 0; line-height: 1.4; }
pre, code { font-family: Menlo, Consolas, monospace; font-size: 0.85em; background: ${p.codeBg}; border-radius: 6px; color: ${p.fg}; }
pre { padding: 16px; overflow-x: auto; margin: 16px 0; white-space: pre-wrap; word-wrap: break-word; }
code { padding: 2px 6px; }
blockquote { border-left: 4px solid ${p.border}; margin: 16px 0; padding: 8px 16px; color: ${p.muted}; }
table { width: 100%; border-collapse: collapse; margin: 16px 0; }
th, td { border: 1px solid ${p.border}; padding: 8px 12px; }
ul, ol { margin: 12px 0; padding-left: 24px; }
hr { border: none; border-top: 1px solid ${p.border}; margin: 24px 0; }
.meta { font-size: 14px; color: ${p.muted}; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid ${p.border}; line-height: 1.6; }
.orig { display: block; margin: 32px auto 8px; padding: 10px 20px; text-align: center; border: 1px solid ${p.border}; border-radius: 20px; color: ${p.link}; font-size: 15px; }
""".trimIndent()
    }

    fun build(
        title: String,
        author: String?,
        link: String?,
        publishedAt: Long,
        content: String,
        prefs: ReaderPrefs
    ): String {
        val meta = buildString {
            if (!author.isNullOrBlank()) append(escape(author))
            if (publishedAt > 0L) {
                if (isNotEmpty()) append(" · ")
                append(formatFullTime(publishedAt))
            }
        }
        val body = normalize(content, link)
        val orig = if (!link.isNullOrBlank()) {
            "<a class='orig' href='${escape(link)}'>阅读原文 ↗</a>"
        } else ""
        return """<!DOCTYPE html>
<html lang='zh'><head>
<meta charset='UTF-8'>
<meta name='viewport' content='width=device-width, initial-scale=1.0'>
<style>${buildCss(prefs.theme, prefs.fontSize, prefs.lineHeight, prefs.showImages, prefs.serifFont, prefs.justifyText)}</style>
</head><body>
<div class='meta'>$meta</div>
$body
$orig
</body></html>"""
    }

    private fun normalize(html: String, baseUrl: String?): String {
        if (html.isBlank()) return "<p>（无正文内容）</p>"
        return try {
            val doc: Document
            if (html.trimStart().startsWith("<")) {
                doc = Jsoup.parse(html)
            } else {
                return "<p>${escape(html)}</p>"
            }
            doc.select("script,style,ins,iframe,noscript,form,button,input").remove()

            // 1) 图片：补全懒加载属性 → src；相对地址 → 绝对地址
            doc.select("img").forEach { img ->
                if (img.attr("src").isBlank()) {
                    for (k in listOf("data-src", "data-original", "data-lazy-src", "data-url", "data-actualsrc", "data-lazy", "data-echo", "data-thumb")) {
                        val v = img.attr(k)
                        if (v.isNotBlank()) {
                            img.attr("src", v)
                            break
                        }
                    }
                }
                var src = img.attr("src")
                if (src.startsWith("//")) src = "https:$src"
                else if (src.startsWith("/") && !baseUrl.isNullOrBlank()) {
                    src = resolve(baseUrl, src)
                }
                if (src.isNotBlank()) img.attr("src", src)
                // 移除 srcset 避免 WebView 加载模糊/缩放变体，简化加载
                img.removeAttr("srcset")
                img.attr("loading", "lazy")
            }

            // 2) 链接：相对地址 → 绝对地址
            doc.select("a[href]").forEach { a ->
                var href = a.attr("href")
                if (href.startsWith("//")) href = "https:$href"
                else if (href.startsWith("/") && !baseUrl.isNullOrBlank()) {
                    href = resolve(baseUrl, href)
                }
                if (href.isNotBlank()) a.attr("href", href)
            }

            doc.body().html()
        } catch (_: Exception) {
            "<p>${escape(html)}</p>"
        }
    }

    private fun resolve(baseUrl: String, path: String): String {
        return try {
            val base = URI(baseUrl)
            val resolved = base.resolve(path)
            resolved.toString()
        } catch (_: Exception) {
            path
        }
    }

    private fun escape(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&#34;")
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