package com.succh.unifeed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.succh.unifeed.data.db.entity.Feed
import com.succh.unifeed.data.db.entity.Folder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedListScreen(
    feeds: List<Feed>,
    selectedFeedId: Long?,
    onSelectFeed: (Long) -> Unit,
    onAddFeed: () -> Unit,
    onDeleteFeed: (Feed) -> Unit,
    modifier: Modifier = Modifier,
    folders: List<Folder> = emptyList(),
    onCreateFolder: (String) -> Unit = {},
    onRenameFolder: (Long, String) -> Unit = { _, _ -> },
    onDeleteFolder: (Long) -> Unit = {},
    onMoveFeedToFolder: (Long, Long?) -> Unit = { _, _ -> },
    onRenameFeed: (Long, String) -> Unit = { _, _ -> },
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {}
) {
    var showFolderManager by remember { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<Feed?>(null) }
    var renamingFeed by remember { mutableStateOf<Feed?>(null) }
    val folderNameById = remember(folders) { folders.associate { it.id to it.name } }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("订阅源") },
                actions = {
                    TextButton(onClick = { showFolderManager = true }) {
                        Text("管理文件夹", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddFeed,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = "添加") },
                text = { Text("添加订阅") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("搜索订阅源...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (feeds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.RssFeed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("还没有订阅源\n点击右下角 + 添加第一个", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            val filteredFeeds = if (searchQuery.isBlank()) feeds else feeds.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
            }
            val grouped = filteredFeeds.groupBy { it.folderId }
            val sortedKeys = grouped.keys.sortedBy { it ?: Long.MAX_VALUE }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                sortedKeys.forEach { folderId ->
                    val groupFeeds = grouped[folderId].orEmpty()
                    val headerName = if (folderId == null) "未分类" else folderNameById[folderId] ?: "未知文件夹"
                    item(key = "header_${folderId}") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = headerName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${groupFeeds.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(groupFeeds, key = { feed -> feed.id }) { feed ->
                        FeedItem(
                            feed = feed,
                            selected = feed.id == selectedFeedId,
                            onClick = { onSelectFeed(feed.id) },
                            onDelete = { onDeleteFeed(feed) },
                            onMove = { moveTarget = feed },
                            onRename = { renamingFeed = feed }
                        )
                    }
                }
            }
        }
    }

    if (showFolderManager) {
        FolderManagerDialog(
            folders = folders,
            feedCountByFolder = feeds.groupBy { it.folderId }.mapValues { it.value.size },
            onCreateFolder = onCreateFolder,
            onRenameFolder = onRenameFolder,
            onDeleteFolder = onDeleteFolder,
            onDismiss = { showFolderManager = false }
        )
    }
    }

    renamingFeed?.let { feed ->
        FolderNameDialog(
            title = "重命名订阅源",
            initialName = feed.title,
            onConfirm = { name ->
                onRenameFeed(feed.id, name)
                renamingFeed = null
            },
            onDismiss = { renamingFeed = null }
        )
    }

    moveTarget?.let { target ->
        MoveFeedDialog(
            feedTitle = target.title,
            folders = folders,
            onSelect = { folderId ->
                onMoveFeedToFolder(target.id, folderId)
                moveTarget = null
            },
            onDismiss = { moveTarget = null }
        )
    }
}

@Composable
private fun FeedItem(
    feed: Feed,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.primaryContainer else colors.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favicon
            if (feed.faviconUrl != null) {
                AsyncImage(
                    model = feed.faviconUrl,
                    contentDescription = "网站图标",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feed.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                if (feed.unreadCount > 0) {
                    Text(
                        text = "${feed.unreadCount} 未读",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primary
                    )
                } else {
                    Text(
                        text = feed.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onMove) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = "移动到文件夹",
                    tint = colors.primary
                )
            }
            IconButton(onClick = onRename) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "重命名",
                    tint = colors.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = colors.error
                )
            }
        }
    }
}

@Composable
private fun FolderManagerDialog(
    folders: List<Folder>,
    feedCountByFolder: Map<Long?, Int>,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (Long, String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var renamingFolder by remember { mutableStateOf<Folder?>(null) }
    var deletingFolder by remember { mutableStateOf<Folder?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理文件夹") },
        text = {
            Column {
                if (folders.isEmpty()) {
                    Text(
                        "还没有文件夹，点击下方按钮创建第一个文件夹",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(folders, key = { it.id }) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(folder.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text(
                                        "${feedCountByFolder[folder.id] ?: 0} 个订阅",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { renamingFolder = folder }) {
                                    Icon(Icons.Default.Edit, contentDescription = "重命名")
                                }
                                IconButton(onClick = { deletingFolder = folder }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("新建文件夹")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )

    if (showCreateDialog) {
        FolderNameDialog(
            title = "新建文件夹",
            initialName = "",
            onConfirm = { name ->
                onCreateFolder(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    renamingFolder?.let { folder ->
        FolderNameDialog(
            title = "重命名文件夹",
            initialName = folder.name,
            onConfirm = { name ->
                onRenameFolder(folder.id, name)
                renamingFolder = null
            },
            onDismiss = { renamingFolder = null }
        )
    }

    deletingFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { deletingFolder = null },
            title = { Text("删除文件夹") },
            text = { Text("确定删除文件夹「${folder.name}」吗？其中的订阅源将移至未分类。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFolder(folder.id)
                        deletingFolder = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingFolder = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun MoveFeedDialog(
    feedTitle: String,
    folders: List<Folder>,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动到文件夹") },
        text = {
            Column {
                Text(
                    "将「$feedTitle」移动到：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(null) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text("未分类", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    items(folders, key = { it.id }) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(folder.id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(folder.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun FolderNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("文件夹名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.trim().isNotEmpty()) onConfirm(name.trim()) },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
