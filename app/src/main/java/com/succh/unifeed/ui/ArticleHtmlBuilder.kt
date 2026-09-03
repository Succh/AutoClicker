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
<meta name='referrer' content='no-referrer'>
<style>${buildCss(prefs.theme, prefs.fontSize, prefs.lineHeight, prefs.showImages, prefs.serifFont, prefs.justifyText)}</style>
</head><body>
<div class='meta'>$meta</div>
$body
$orig
</body></html>"""
    }

    /**
     * 正文 HTML 归一化：去脚本/样式，图片/链接相对地址补全为绝对地址。
     */
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

            // 1) 图片：懒加载属性补全 → src；各种相对地址 → 绝对地址；加 referrerpolicy 防防盗链
            doc.select("img").forEach { img ->
                if (img.attr("src").isBlank()) {
                    for (k in listOf("data-src", "data-original", "data-lazy-src", "data-url", "data-actualsrc", "data-lazy", "data-echo", "data-thumb", "data-srcset", "data-ks-lazyload")) {
                        val v = img.attr(k)
                        if (v.isNotBlank()) {
                            // 懒加载值可能是 srcset 格式（含逗号空格），只取第一个 URL
                            img.attr("src", v.substringBefore(" ").substringBefore(","))
                            break
                        }
                    }
                }
                var src = img.attr("src")
                src = absolutize(src, baseUrl)
                if (src.isNotBlank()) img.attr("src", src)
                // 移除 srcset 避免 WebView 加载模糊/缩放变体，简化加载
                img.removeAttr("srcset")
                img.removeAttr("data-src")
                img.removeAttr("data-original")
                // 不设 loading=lazy：WebView 以 data: 加载时懒加载可能不触发
                img.removeAttr("loading")
                // 防盗链：不发送 Referer，绕过图床 Referer 校验
                img.attr("referrerpolicy", "no-referrer")
            }

            // 2) 链接：相对地址 → 绝对地址
            doc.select("a[href]").forEach { a ->
                val href = absolutize(a.attr("href"), baseUrl)
                if (href.isNotBlank()) a.attr("href", href)
            }

            doc.body().html()
        } catch (_: Exception) {
            "<p>${escape(html)}</p>"
        }
    }

    /**
     * 把各种形态的相对地址转成绝对地址：
     *  - "//host/path" → "https://host/path"
     *  - "/path"、"path"、"./path"、"../path" → 基于 baseUrl 解析
     *  - 已是 http/https/data/blob 等绝对地址 → 原样返回
     */
    private fun absolutize(url: String, baseUrl: String?): String {
        if (url.isBlank()) return ""
        if (url.startsWith("//")) return "https:$url"
        val hasScheme = url.startsWith("http://") || url.startsWith("https://") ||
            url.startsWith("data:") || url.startsWith("blob:") || url.startsWith("about:") ||
            url.startsWith("file:") || url.startsWith("ftp:") || url.startsWith("mailto:") ||
            url.startsWith("tel:") || url.startsWith("javascript:") || url.startsWith("#")
        if (hasScheme) return url
        if (baseUrl.isNullOrBlank()) return url
        return resolve(baseUrl, url)
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
