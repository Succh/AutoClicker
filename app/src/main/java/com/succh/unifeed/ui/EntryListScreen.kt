package com.succh.unifeed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.succh.unifeed.data.db.entity.Entry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    entries: List<Entry>,
    onEntryClick: (Entry) -> Unit,
    onStarToggle: (Entry, Boolean) -> Unit,
    showSummary: Boolean = false,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    onFilter: (FilterMode) -> Unit = {},
    filterMode: FilterMode = FilterMode.ALL,
    onMarkAllRead: () -> Unit = {},
    showFilter: Boolean = true,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onMarkRead: (Entry) -> Unit = {},
    onMarkUnread: (Entry) -> Unit = {}
) {
    // 始终用可滚动的 LazyColumn 作为子内容，空列表时下拉刷新才能生效
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("搜索文章...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (showFilter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = filterMode == FilterMode.ALL,
                        onClick = { onFilter(FilterMode.ALL) },
                        label = { Text("全部") }
                    )
                    FilterChip(
                        selected = filterMode == FilterMode.UNREAD,
                        onClick = { onFilter(FilterMode.UNREAD) },
                        label = { Text("未读") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (entries.any { !it.isRead }) {
                        TextButton(onClick = onMarkAllRead) {
                            Text("全部标为已读")
                        }
                    }
                }
            }
            val filteredEntries = if (searchQuery.isBlank()) entries else entries.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                (it.summary?.contains(searchQuery, ignoreCase = true) == true)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (filteredEntries.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无文章", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    items(filteredEntries, key = { it.id }) { entry ->
                        SwipeableEntryItem(
                            entry = entry,
                            showSummary = showSummary,
                            onClick = { onEntryClick(entry) },
                            onStar = { onStarToggle(entry, !entry.isStarred) },
                            onMarkRead = { onMarkRead(entry) },
                            onMarkUnread = { onMarkUnread(entry) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableEntryItem(
    entry: Entry,
    showSummary: Boolean,
    onClick: () -> Unit,
    onStar: () -> Unit,
    onMarkRead: () -> Unit,
    onMarkUnread: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // 右滑：切换已读/未读
                    if (entry.isRead) onMarkUnread() else onMarkRead()
                    false // 不真正 dismiss，只触发动作
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // 左滑：收藏/取消收藏
                    onStar()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (entry.isRead) Icons.Default.MarkEmailUnread else Icons.Default.MarkEmailRead
                SwipeToDismissBoxValue.EndToStart -> if (entry.isStarred) Icons.Default.StarOutline else Icons.Default.Star
                else -> null
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                }
            ) {
                icon?.let {
                    Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    ) {
        EntryItem(
            entry = entry,
            showSummary = showSummary,
            onClick = onClick,
            onStar = onStar
        )
    }
}

@Composable
private fun EntryItem(
    entry: Entry,
    showSummary: Boolean,
    onClick: () -> Unit,
    onStar: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isRead) colors.surface else colors.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!entry.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(end = 8.dp)
                        .background(color = colors.primary, shape = CircleShape)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (entry.isRead) null else FontWeight.Bold
                )
                if (showSummary) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        entry.summary ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.author ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatTime(entry.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            IconButton(onClick = onStar) {
                Icon(
                    if (entry.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "收藏",
                    tint = if (entry.isStarred) Color(0xFFFFB300) else colors.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return try {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}
