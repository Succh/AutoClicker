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
    var showDiscover by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<Entry?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val prefs = remember { ReaderPrefs(context) }

    fun updateRsshubInstance(instance: String?) {
        prefs.rsshubInstance = instance
        (context.applicationContext as? UniFeedApp)
            ?.repository?.customRsshubInstance = instance
    }

    LaunchedEffect(state.selectedFeedId) {
        if (state.selectedFeedId != null) {
            tab = 1
            showDiscover = false
        }
    }

    if (showDiscover) {
        DiscoverScreen(
            isDiscovering = state.isDiscovering,
            discoveredFeeds = state.discoveredFeeds,
            discoveryError = state.discoveryError,
            isSubscribing = state.isSubscribing,
            subscribeError = state.subscribeError,
            onBack = {
                showDiscover = false
                viewModel.clearDiscovery()
                viewModel.clearError()
            },
            onDiscover = { viewModel.discoverFeeds(it) },
            onAddDirect = { viewModel.addFeed(it) },
            onSubscribe = { url, title -> viewModel.addFeed(url, title) },
            onClearError = { viewModel.clearError() }
        )
        return
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
            tab = tab,
            onTabChange = { tab = it },
            state = state,
            prefs = prefs,
            onSelectFeed = { feedId ->
                viewModel.selectFeed(feedId)
                if (feedId != null) tab = 1
            },
            onAddFeed = { showDiscover = true },
            onDeleteFeed = viewModel::deleteFeed,
            onEntryClick = { entry ->
                selectedEntry = entry
                if (prefs.autoMarkRead) viewModel.markRead(entry)
            },
            onStarToggle = viewModel::setStarred,
            onFilter = viewModel::setFilter,
            onRefresh = viewModel::refreshAll
        )
    }
}

@Composable
private fun MainTabScreen(
    tab: Int,
    onTabChange: (Int) -> Unit,
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
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { onTabChange(0) },
                    icon = { Icon(Icons.Default.RssFeed, contentDescription = null) },
                    label = { Text("订阅源") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { onTabChange(1) },
                    icon = { Icon(Icons.Default.Article, contentDescription = null) },
                    label = { Text("文章") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { onTabChange(2) },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("收藏") }
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { onTabChange(3) },
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
                modifier = Modifier.padding(padding),
                onRsshubInstanceChange = ::updateRsshubInstance
            )
        }
    }
}