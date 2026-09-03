package com.succh.unifeed.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.succh.unifeed.UniFeedApp
import com.succh.unifeed.data.db.entity.Entry
import com.succh.unifeed.data.db.entity.Feed
import com.succh.unifeed.data.rss.DiscoveredFeed
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UniFeedUiState(
    val feeds: List<Feed> = emptyList(),
    val entries: List<Entry> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFeedId: Long? = null,
    val filterMode: FilterMode = FilterMode.ALL,
    // 发现状态
    val isDiscovering: Boolean = false,
    val discoveredFeeds: List<DiscoveredFeed> = emptyList(),
    val discoveryError: String? = null
)

enum class FilterMode { ALL, UNREAD, STARRED }

class UniFeedViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as UniFeedApp).repository

    private val _uiState = MutableStateFlow(UniFeedUiState())
    val uiState: StateFlow<UniFeedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeFeeds().collect { feeds ->
                _uiState.update { it.copy(feeds = feeds) }
            }
        }
        viewModelScope.launch {
            repo.observeUnreadCount().collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
        viewModelScope.launch {
            combine(
                repo.observeAllEntries(),
                _uiState.map { it.filterMode },
                _uiState.map { it.selectedFeedId }
            ) { allEntries, filter, feedId ->
                when (filter) {
                    FilterMode.UNREAD -> allEntries.filter { !it.isRead }
                    FilterMode.STARRED -> allEntries.filter { it.isStarred }
                    FilterMode.ALL -> if (feedId != null) allEntries.filter { it.feedId == feedId } else allEntries
                }
            }.collect { filtered ->
                _uiState.update { it.copy(entries = filtered) }
            }
        }
    }

    /** 发现订阅源：输入任意 URL，自动探测 RSS/Atom */
    fun discoverFeeds(input: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true, discoveryError = null, discoveredFeeds = emptyList()) }
            val result = repo.discoverFeeds(input)
            result.onSuccess { feeds ->
                _uiState.update { it.copy(isDiscovering = false, discoveredFeeds = feeds) }
            }.onFailure { e ->
                _uiState.update { it.copy(isDiscovering = false, discoveryError = e.message ?: "探测失败") }
            }
        }
    }

    /** 清除发现结果 */
    fun clearDiscovery() {
        _uiState.update { it.copy(isDiscovering = false, discoveredFeeds = emptyList(), discoveryError = null) }
    }

    /** 添加订阅：先尝试自动发现，发现不到再直接当 RSS 链接解析 */
    fun addFeed(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val trimmed = url.trim()
            // 明显是 RSS 链接时跳过发现，直接解析
            val looksLikeFeed = trimmed.endsWith(".xml") || trimmed.endsWith(".rss") ||
                trimmed.endsWith(".atom") || trimmed.contains("/feed") || trimmed.contains("rsshub")
            val feedUrl = if (looksLikeFeed) {
                trimmed
            } else {
                // 先尝试自动发现（输入普通网页时可自动探测 RSS）
                val discoveryResult = repo.discoverFeeds(trimmed)
                if (discoveryResult.isSuccess && discoveryResult.getOrThrow().isNotEmpty()) {
                    discoveryResult.getOrThrow().first().feedUrl
                } else {
                    trimmed // 直接当 RSS 链接处理
                }
            }
            val result = repo.addFeed(feedUrl)
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess { feed ->
                _uiState.update { it.copy(selectedFeedId = feed.id) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "添加失败：无法识别该链接中的订阅源") }
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repo.refreshAll()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun deleteFeed(feed: Feed) {
        viewModelScope.launch {
            repo.deleteFeed(feed)
        }
    }

    fun markRead(entry: Entry) {
        viewModelScope.launch { repo.markRead(entry) }
    }

    fun markUnread(entry: Entry) {
        viewModelScope.launch { repo.markUnread(entry) }
    }

    fun setStarred(entry: Entry, starred: Boolean) {
        viewModelScope.launch { repo.setStarred(entry, starred) }
    }

    fun selectFeed(feedId: Long?) {
        _uiState.update { it.copy(selectedFeedId = feedId, filterMode = FilterMode.ALL) }
    }

    fun setFilter(mode: FilterMode) {
        _uiState.update { it.copy(filterMode = mode, selectedFeedId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
