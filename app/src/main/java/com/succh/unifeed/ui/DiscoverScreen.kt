package com.succh.unifeed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.succh.unifeed.data.rss.DiscoveredFeed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    isDiscovering: Boolean,
    discoveredFeeds: List<DiscoveredFeed>,
    discoveryError: String?,
    isSubscribing: Boolean,
    subscribeError: String?,
    onBack: () -> Unit,
    onDiscover: (String) -> Unit,
    onAddDirect: (String) -> Unit,
    onSubscribe: (String) -> Unit,
    onClearError: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<RsshubCategory?>(null) }
    var pending by remember { mutableStateOf<RsshubRoute?>(null) }
    var param by remember { mutableStateOf("") }

    // 需要补参数的路由确认对话框
    pending?.let { r ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(r.title) },
            text = {
                Column {
                    Text(
                        r.description.ifBlank { "该路由需要补充参数" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        param,
                        { param = it },
                        label = { Text("参数") },
                        singleLine = true,
                        placeholder = { Text(r.description) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "订阅地址：${r.url}${param.trim()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalUrl = if (r.description.isNotBlank()) {
                            r.url.trimEnd('/') + "/" + param.trim().trimStart('/')
                        } else r.url
                        pending = null
                        onSubscribe(finalUrl)
                    },
                    enabled = r.description.isBlank() || param.isNotBlank()
                ) { Text("订阅") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (cat == null) "发现" else cat!!.name) },
                navigationIcon = {
                    IconButton(onClick = { if (cat != null) cat = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 输入区（进入分类浏览时隐藏）
            if (cat == null) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("网站地址 或 RSS/Atom 链接") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = { if (url.isNotBlank()) onDiscover(url) },
                        enabled = url.isNotBlank() && !isDiscovering && !isSubscribing
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("发现")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { if (url.isNotBlank()) onAddDirect(url) },
                        enabled = url.isNotBlank() && !isDiscovering && !isSubscribing
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("直接添加")
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("或从 RSSHub 精选分类一键订阅：", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
            }

            // 订阅中 loading
            if (isSubscribing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("正在订阅...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 订阅错误提示
            subscribeError?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            err,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = onClearError) { Text("知道了") }
                    }
                }
            }

            // 主体内容
            when {
                // 正在探测
                isDiscovering -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                // 发现结果列表
                discoveredFeeds.isNotEmpty() && cat == null -> {
                    Text("发现 ${discoveredFeeds.size} 个订阅源，点击订阅：", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(discoveredFeeds) { feed ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSubscribe(feed.feedUrl) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    Modifier
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
                                    Column(Modifier.weight(1f)) {
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
                }

                // 探测错误
                discoveryError != null && cat == null -> Column(
                    Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        discoveryError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { if (url.isNotBlank()) onDiscover(url) }) { Text("重试") }
                }

                // RSSHub 分类列表
                cat == null -> LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(RsshubPresets.categories) { c ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { cat = c },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.RssFeed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${c.routes.size} 个源",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 分类内路由列表
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(cat!!.routes) { r ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (r.description.isNotBlank()) {
                                        param = ""
                                        pending = r
                                    } else {
                                        onSubscribe(r.url)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        r.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (r.description.isNotBlank()) "需补充${r.description}" else "直接订阅",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
