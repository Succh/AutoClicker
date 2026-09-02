package com.succh.unifeed.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.succh.unifeed.data.db.entity.Entry

@Composable
fun UniFeedApp(
    viewModel: UniFeedViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<Entry?>(null) }
    var discoverInput by remember { mutableStateOf("") }
    var showRsshub by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = remember { ReaderPrefs(context) }

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
            },
            onRsshubBrowse = { showRsshub = true }
        )
    }

    if (showRsshub) {
        RsshubBrowserDialog(
            onDismiss = { showRsshub = false },
            onSubscribe = { url ->
                viewModel.addFeed(url)
                showRsshub = false
                showAddDialog = false
                viewModel.clearDiscovery()
            }
        )
    }

    val error = state.discoveryError
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
    } else if (error != null) {
        FeedDiscoveryError(
            message = error,
            onDismiss = { viewModel.clearDiscovery() },
            onRetry = { viewModel.discoverFeeds(discoverInput) }
        )
    }

    val entry = selectedEntry
    if (entry != null) {
        ReaderScreen(
            entry = entry,
            prefs = prefs,
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
            prefs = prefs,
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
    prefs: ReaderPrefs,
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
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("设置") }
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
                showSummary = prefs.listShowSummary,
                modifier = Modifier.padding(padding)
            )
            2 -> EntryListScreen(
                entries = state.entries.filter { it.isStarred },
                onEntryClick = onEntryClick,
                onStarToggle = onStarToggle,
                showSummary = prefs.listShowSummary,
                modifier = Modifier.padding(padding)
            )
            3 -> SettingsScreen(
                prefs = prefs,
                modifier = Modifier.padding(padding)
            )
        }
    }
}