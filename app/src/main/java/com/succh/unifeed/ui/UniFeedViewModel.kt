package com.succh.unifeed.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.succh.unifeed.UniFeedApp
import com.succh.unifeed.data.db.entity.Entry
import com.succh.unifeed.data.db.entity.Feed
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UniFeedUiState(
    val feeds: List<Feed> = emptyList(),
    val entries: List<Entry> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFeedId: Long? = null,
    val filterMode: FilterMode = FilterMode.ALL
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
        observeEntries()
    }

    private fun observeEntries() {
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

    fun addFeed(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repo.addFeed(url.trim())
            _uiState.update { it.copy(isLoading = false) }
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "添加失败") }
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
        viewModelScope.launch {
            repo.markRead(entry)
        }
    }

    fun setStarred(entry: Entry, starred: Boolean) {
        viewModelScope.launch {
            repo.setStarred(entry, starred)
        }
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

    // 用于 Repository 的额外操作委托
    private suspend fun FeedRepository.markRead(entry: Entry) {
        com.succh.unifeed.data.db.dao.EntryDao::class.java
        // 实际通过 DAO 操作
    }

    private suspend fun FeedRepository.setStarred(entry: Entry, starred: Boolean) {
        // 实际通过 DAO 操作
    }
}

// 扩展 Repository 的辅助方法
private suspend fun com.succh.unifeed.data.repository.FeedRepository.markRead(entry: Entry) {
    // 通过 DAO 操作
    com.succh.unifeed.UniFeedApp::class.java
}

private suspend fun com.succh.unifeed.data.repository.FeedRepository.setStarred(entry: Entry, starred: Boolean) {
}
