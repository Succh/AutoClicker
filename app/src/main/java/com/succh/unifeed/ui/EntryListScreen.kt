package com.succh.unifeed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.succh.unifeed.data.db.entity.Entry
import com.succh.unifeed.data.db.entity.Feed
import com.succh.unifeed.ui.theme.FoloDimens
import com.succh.unifeed.ui.theme.formatRelativeTimeZh

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
    onMarkUnread: (Entry) -> Unit = {},
    feeds: List<Feed> = emptyList()
) {
    // 用 feedId 索引订阅源，供卡片顶部展示名称与 favicon
    val feedById = remember(feeds) { feeds.associateBy { it.id } }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("搜索文章...") },
                singleLine = true,
                shape = RoundedCornerShape(FoloDimens.SearchRadius),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FoloDimens.ScreenHorizontal, vertical = 8.dp)
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
                contentPadding = PaddingValues(
                    horizontal = FoloDimens.ScreenHorizontal,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(FoloDimens.CardSpacing)
            ) {
                if (filteredEntries.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无文章", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    items(filteredEntries, key = { it.id }) { entry ->
                        val feed = feedById[entry.feedId]
                        SwipeableEntryItem(
                            entry = entry,
                            feedTitle = feed?.title.orEmpty(),
                            feedFaviconUrl = feed?.faviconUrl,
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
    feedTitle: String,
    feedFaviconUrl: String?,
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
                    // 右滑：切换已读/未读，不真正移除条目
                    if (entry.isRead) onMarkUnread() else onMarkRead()
                    false
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
                SwipeToDismissBoxValue.StartToEnd ->
                    if (entry.isRead) Icons.Default.MarkEmailUnread else Icons.Default.MarkEmailRead
                SwipeToDismissBoxValue.EndToStart ->
                    if (entry.isStarred) Icons.Default.StarOutline else Icons.Default.Star
                else -> null
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(FoloDimens.CardRadius))
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
            feedTitle = feedTitle,
            feedFaviconUrl = feedFaviconUrl,
            showSummary = showSummary,
            onClick = onClick,
            onStar = onStar
        )
    }
}

@Composable
private fun EntryItem(
    entry: Entry,
    feedTitle: String,
    feedFaviconUrl: String?,
    showSummary: Boolean,
    onClick: () -> Unit,
    onStar: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    // 已读文章整体降透明度，突出未读时间线
    val cardAlpha = if (entry.isRead) 0.6f else 1f

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        shape = RoundedCornerShape(FoloDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 未读：左侧 3dp 主色竖条
            if (!entry.isRead) {
                Box(
                    modifier = Modifier
                        .width(FoloDimens.UnreadBarWidth)
                        .fillMaxHeight()
                        .background(colors.primary)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = FoloDimens.CardPadding,
                        top = 12.dp,
                        end = 4.dp,
                        bottom = 12.dp
                    )
            ) {
                // 顶部：订阅源 favicon + 名称
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!feedFaviconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = feedFaviconUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(FoloDimens.EntryFaviconSize)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    } else {
                        Icon(
                            Icons.Default.RssFeed,
                            contentDescription = null,
                            modifier = Modifier.size(FoloDimens.EntryFaviconSize),
                            tint = colors.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = feedTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (entry.isRead) FontWeight.Normal else FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val summary = entry.summary.orEmpty()
                if (showSummary && summary.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.2f
                        ),
                        color = colors.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))
                // 底部元信息：相对时间、阅读状态点、收藏星标
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatRelativeTimeZh(entry.publishedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (entry.isRead) colors.outline else colors.primary,
                                shape = CircleShape
                            )
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onStar, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (entry.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "收藏",
                            tint = if (entry.isStarred) Color(0xFFFFB300) else colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
