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

@Composable
fun EntryListScreen(
    entries: List<Entry>,
    onEntryClick: (Entry) -> Unit,
    onStarToggle: (Entry, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("暂无文章", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                EntryItem(
                    entry = entry,
                    onClick = { onEntryClick(entry) },
                    onStar = { onStarToggle(entry, !entry.isStarred) }
                )
            }
        }
    }
}

@Composable
private fun EntryItem(
    entry: Entry,
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
