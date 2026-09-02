package com.succh.unifeed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.succh.unifeed.data.rss.DiscoveredFeed

@Composable
fun FeedDiscoveryDialog(
    discoveredFeeds: List<DiscoveredFeed>,
    onDismiss: () -> Unit,
    onConfirm: (DiscoveredFeed) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.RssFeed, contentDescription = null)
        },
        title = { Text("发现订阅源") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(discoveredFeeds) { feed ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(feed) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.RssFeed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    feed.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    feed.feedUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun FeedDiscoveryLoading() {
    AlertDialog(
        onDismissRequest = {},
        icon = {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        },
        title = { Text("正在探测订阅源...") },
        text = { Text("正在抓取网站并扫描 RSS/Atom 链接") },
        confirmButton = {}
    )
}

@Composable
fun FeedDiscoveryError(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("未发现订阅源") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onRetry) { Text("重试") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
