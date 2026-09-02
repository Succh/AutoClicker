package com.succh.unifeed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun RsshubBrowserDialog(onDismiss: () -> Unit, onSubscribe: (String) -> Unit) {
    var cat by remember { mutableStateOf<RsshubCategory?>(null) }
    var pending by remember { mutableStateOf<RsshubRoute?>(null) }
    var param by remember { mutableStateOf("") }

    pending?.let { r ->
        AlertDialog(onDismissRequest = { pending = null }, title = { Text(r.title) },
            text = {
                Column {
                    Text(r.description.ifBlank { "该路由需要补充参数" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(param, { param = it }, label = { Text("参数") }, singleLine = true, placeholder = { Text(r.description) })
                    Spacer(Modifier.height(8.dp))
                    Text("订阅地址：${r.url}${param.trim()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = if (r.description.isNotBlank()) r.url.trimEnd('/') + "/" + param.trim().trimStart('/') else r.url
                    pending = null; onSubscribe(url)
                }, enabled = r.description.isBlank() || param.isNotBlank()) { Text("订阅") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("取消") } })
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (cat != null) { IconButton(onClick = { cat = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(if (cat == null) "RSSHub 精选订阅" else cat!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (cat == null) "选择分类" else "点击订阅", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
                LazyColumn(Modifier.height(420.dp)) {
                    val items = if (cat == null) RsshubPresets.categories.map { a -> Triple(a.name, "${a.routes.size} 个源", { cat = a } as () -> Unit) } else cat!!.routes.map { b -> Triple(b.title, if (b.description.isNotBlank()) "需补充${b.description}" else "直接订阅", { if (b.description.isNotBlank()) { param = ""; pending = b } else onSubscribe(b.url) } as () -> Unit) }
                    items(items) { (title, sub, action) ->
                        Row(Modifier.fillMaxWidth().clickable { action() }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (cat == null) Icons.Default.RssFeed else Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}