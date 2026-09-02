package com.succh.unifeed.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.succh.unifeed.data.db.entity.Entry

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderScreen(
    entry: Entry,
    prefs: ReaderPrefs,
    onBack: () -> Unit,
    onToggleStar: (Entry, Boolean) -> Unit,
    onOpenBrowser: (Entry) -> Unit,
    onShare: (Entry) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var showAaPanel by remember { mutableStateOf(false) }

    val articleHtml = remember(
        entry.content, entry.summary,
        prefs.fontSize, prefs.lineHeight, prefs.theme,
        prefs.showImages, prefs.serifFont, prefs.justifyText
    ) {
        ArticleHtmlBuilder.build(
            title = entry.title,
            author = entry.author,
            link = entry.link,
            publishedAt = entry.publishedAt,
            content = entry.content ?: entry.summary ?: "",
            prefs = prefs
        )
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
                    IconButton(onClick = { showAaPanel = true }) {
                        Icon(Icons.Filled.FormatSize, contentDescription = "阅读设置", tint = colors.onSurfaceVariant)
                    }
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
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.javaScriptEnabled = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.domStorageEnabled = true
                    settings.defaultTextEncodingName = "UTF-8"
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL(null, articleHtml, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, articleHtml, "text/html", "UTF-8", null)
            }
        )
    }

    if (showAaPanel) {
        AaSettingsPanel(
            prefs = prefs,
            onDismiss = { showAaPanel = false }
        )
    }
}
