package com.succh.unifeed.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.succh.unifeed.data.db.entity.Entry
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    entry: Entry,
    onBack: () -> Unit,
    onToggleStar: (Entry, Boolean) -> Unit,
    onOpenBrowser: (Entry) -> Unit,
    onShare: (Entry) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val contentText = remember(entry.content, entry.summary) {
        htmlToText(entry.content ?: entry.summary ?: "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        entry.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onToggleStar(entry, !entry.isStarred) }) {
                        Icon(
                            if (entry.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "收藏",
                            tint = if (entry.isStarred) Color(0xFFFFB300) else colors.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onShare(entry) }) {
                        Icon(Icons.Filled.Share, contentDescription = "分享")
                    }
                    IconButton(onClick = { onOpenBrowser(entry) }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "浏览器打开")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                entry.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            if (!entry.author.isNullOrBlank()) {
                Text(
                    entry.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                formatFullTime(entry.publishedAt),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            if (!entry.link.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.link,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            if (contentText.isBlank()) {
                Text(
                    "（无正文内容）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            } else {
                Text(
                    contentText,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp
                )
            }
        }
    }
}

/**
 * 将 HTML 正文转为纯文本：剔除脚本/样式/广告节点后取 text()
 */
private fun htmlToText(html: String): String {
    if (html.isBlank()) return ""
    return try {
        if (html.trimStart().startsWith("<")) {
            val doc = Jsoup.parse(html)
            doc.select("script,style,ins,iframe,noscript").remove()
            doc.text().trim()
        } else {
            html.trim()
        }
    } catch (_: Exception) {
        html.trim()
    }
}

private fun formatFullTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}
