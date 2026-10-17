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
    onSearchQueryChange: (String) -> Unit = {}
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
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
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无文章", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    val filteredEntries = if (searchQuery.isBlank()) entries else entries.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        (it.summary?.contains(searchQuery, ignoreCase = true) == true)
                    }
                    items(filteredEntries, key = { it.id }) { entry ->
                        EntryItem(
                            entry = entry,
                            showSummary = showSummary,
                            onClick = { onEntryClick(entry) },
                            onStar = { onStarToggle(entry, !entry.isStarred) }
                        )
                    }
                }
            }
        }
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
