package com.succh.unifeed.ui

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import com.succh.unifeed.data.db.entity.Entry

@Composable
fun UniFeedApp(
    viewModel: UniFeedViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<Entry?>(null) }
    var discoverInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    if (showAddDialog) {
        AddFeedDialog(
            onDismiss = { showAddDialog = false; viewModel.clearDiscovery() },
            onConfirm = {
                viewModel.addFeed(it)
                showAddDialog = false
                viewModel.clearDiscovery()
            },
            onDiscover = {
                discoverInput = it
                viewModel.discoverFeeds(it)
            }
        )
    }

    // 发现流程状态：加载中 / 结果 / 错误
    if (state.isDiscovering) {
        FeedDiscoveryLoading()
    } else if (state.discoveredFeeds.isNotEmpty()) {
        FeedDiscoveryDialog(
            discoveredFeeds = state.discoveredFeeds,
            onDismiss = { viewModel.clearDiscovery() },
            onConfirm = { feed ->
                viewModel.addFeed(feed.feedUrl)
                viewModel.clearDiscovery()
                showAddDialog = false
            }
        )
    } else if (state.discoveryError != null) {
        FeedDiscoveryError(
            message = state.discoveryError,
            onDismiss = { viewModel.clearDiscovery() },
            onRetry = { viewModel.discoverFeeds(discoverInput) }
        )
    }

    val entry = selectedEntry
    if (entry != null) {
        ReaderScreen(
            entry = entry,
            onBack = { selectedEntry = null },
            onToggleStar = { e, s -> viewModel.setStarred(e, s) },
            onOpenBrowser = { e ->
                e.link?.let {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, it.toUri())) }
                }
            },
            onShare = { e ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${e.title}\n${e.link}")
                }
                runCatching { context.startActivity(Intent.createChooser(sendIntent, "分享")) }
            }
        )
    } else {
        MainTabScreen(
            state = state,
            onSelectFeed = viewModel::selectFeed,
            onAddFeed = { showAddDialog = true },
            onDeleteFeed = viewModel::deleteFeed,
            onEntryClick = { selectedEntry = it; viewModel.markRead(it) },
            onStarToggle = viewModel::setStarred,
            onFilter = viewModel::setFilter,
            onRefresh = viewModel::refreshAll
        )
    }
}

@Composable
private fun MainTabScreen(
    state: UniFeedUiState,
    onSelectFeed: (Long?) -> Unit,
    onAddFeed: () -> Unit,
    onDeleteFeed: (com.succh.unifeed.data.db.entity.Feed) -> Unit,
    onEntryClick: (Entry) -> Unit,
    onStarToggle: (Entry, Boolean) -> Unit,
    onFilter: (FilterMode) -> Unit,
    onRefresh: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.RssFeed, contentDescription = null) },
                    label = { Text("订阅源") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Article, contentDescription = null) },
                    label = { Text("文章") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("收藏") }
                )
            }
        }
    ) { padding ->
        when (tab) {
            0 -> FeedListScreen(
                feeds = state.feeds,
                selectedFeedId = state.selectedFeedId,
                onSelectFeed = onSelectFeed,
                onAddFeed = onAddFeed,
                onDeleteFeed = onDeleteFeed,
                modifier = Modifier.padding(padding)
            )
            1 -> EntryListScreen(
                entries = state.entries,
                onEntryClick = onEntryClick,
                onStarToggle = onStarToggle,
                modifier = Modifier.padding(padding)
            )
            2 -> EntryListScreen(
                entries = state.entries.filter { it.isStarred },
                onEntryClick = onEntryClick,
                onStarToggle = onStarToggle,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
