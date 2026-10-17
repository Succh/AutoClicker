package com.succh.unifeed.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.succh.unifeed.UniFeedApp
import com.succh.unifeed.data.db.entity.Entry
import com.succh.unifeed.data.db.entity.Feed
import com.succh.unifeed.data.db.entity.Folder
import com.succh.unifeed.data.opml.OpmlManager
import com.succh.unifeed.data.rss.DiscoveredFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

data class UniFeedUiState(
    val feeds: List<Feed> = emptyList(),
    val entries: List<Entry> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFeedId: Long? = null,
    val filterMode: FilterMode = FilterMode.ALL,
    val isDiscovering: Boolean = false,
    val discoveredFeeds: List<DiscoveredFeed> = emptyList(),
    val discoveryError: String? = null,
    val isSubscribing: Boolean = false,
    val subscribeError: String? = null,
    val folders: List<Folder> = emptyList()
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
            repo.observeFolders().collect { folders ->
                _uiState.update { it.copy(folders = folders) }
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

    fun createFolder(name: String) {
        viewModelScope.launch { repo.createFolder(name) }
    }

    fun renameFolder(id: Long, name: String) {
        viewModelScope.launch { repo.renameFolder(id, name) }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch { repo.deleteFolder(id) }
    }

    fun moveFeedToFolder(feedId: Long, folderId: Long?) {
        viewModelScope.launch { repo.moveFeedToFolder(feedId, folderId) }
    }

    fun clearDiscovery() {
        _uiState.update { it.copy(isDiscovering = false, discoveredFeeds = emptyList(), discoveryError = null) }
    }

    /**
     * 添加订阅：RSSHub 源走即时订阅（先存后取），普通源走原有抓取流程
     * 可传 titleHint 用于即时订阅时显示标题
     */
    fun addFeed(url: String, titleHint: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubscribing = true, subscribeError = null) }
            val trimmed = url.trim()
            // RSSHub 源：即时订阅，不等待网络
            if (trimmed.contains("rsshub.app") || trimmed.contains("succh.zone.id") ||
                trimmed.contains("rsshub.cups.moe") || trimmed.contains("rss.owo.nz")) {
                val result = repo.addFeedInstant(trimmed, titleHint)
                result.onSuccess { feed ->
                    _uiState.update { it.copy(isSubscribing = false, selectedFeedId = feed.id) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isSubscribing = false, subscribeError = e.message ?: "订阅失败") }
                }
                return@launch
            }
            // 普通源：先探测再抓取
            val looksLikeFeed = trimmed.endsWith(".xml") || trimmed.endsWith(".rss") ||
                trimmed.endsWith(".atom") || trimmed.contains("/feed")
            val feedUrl = if (looksLikeFeed) {
                trimmed
            } else {
                val discoveryResult = repo.discoverFeeds(trimmed)
                if (discoveryResult.isSuccess && discoveryResult.getOrThrow().isNotEmpty()) {
                    discoveryResult.getOrThrow().first().feedUrl
                } else {
                    trimmed
                }
            }
            val result = repo.addFeed(feedUrl)
            _uiState.update { it.copy(isSubscribing = false) }
            result.onSuccess { feed ->
                _uiState.update { it.copy(selectedFeedId = feed.id) }
            }.onFailure { e ->
                _uiState.update { it.copy(subscribeError = e.message ?: "添加失败：无法识别该链接中的订阅源") }
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

    fun markAllRead() {
        viewModelScope.launch {
            repo.markAllReadAll()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, subscribeError = null) }
    }

    /** 导出 OPML：从仓库取 feeds + folders 生成 XML 字符串 */
    suspend fun exportOpml(): String = withContext(Dispatchers.IO) {
        val feeds = repo.observeFeeds().first()
        val folders = repo.observeFolders().first()
        OpmlManager.export(feeds, folders)
    }

    /** 导入 OPML：解析输入流并批量导入，返回 Pair(成功数, 跳过数) */
    suspend fun importOpml(inputStream: InputStream): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val list = OpmlManager.parse(inputStream)
        repo.importFeeds(list)
    }
}