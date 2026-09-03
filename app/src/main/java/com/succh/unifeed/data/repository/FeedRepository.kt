package com.succh.unifeed.data.repository

import com.succh.unifeed.data.db.AppDatabase
import com.succh.unifeed.data.db.entity.Entry
import com.succh.unifeed.data.db.entity.Feed
import com.succh.unifeed.data.model.ParsedFeed
import com.succh.unifeed.data.rss.ContentExtractor
import com.succh.unifeed.data.rss.DiscoveredFeed
import com.succh.unifeed.data.rss.FeedDiscovery
import com.succh.unifeed.data.rss.RssParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * 订阅源仓库：统一管理订阅 CRUD 与抓取
 */
class FeedRepository(
    private val db: AppDatabase,
    private val parser: RssParser = RssParser(),
    private val discovery: FeedDiscovery = FeedDiscovery()
) {
    private val feedDao = db.feedDao()
    private val entryDao = db.entryDao()
    private val folderDao = db.folderDao()

    fun observeFeeds(): Flow<List<Feed>> = feedDao.observeAll()
    fun observeEntries(feedId: Long): Flow<List<Entry>> = entryDao.observeByFeed(feedId)
    fun observeAllEntries(): Flow<List<Entry>> = entryDao.observeAll()
    fun observeUnreadEntries(): Flow<List<Entry>> = entryDao.observeUnread()
    fun observeStarredEntries(): Flow<List<Entry>> = entryDao.observeStarred()
    fun observeUnreadCount(): Flow<Int> = entryDao.observeUnreadCount()

    /**
     * 订阅发现：输入任意网站地址，自动探测其 RSS/Atom 订阅源
     */
    suspend fun discoverFeeds(input: String): Result<List<DiscoveredFeed>> {
        return try {
            Result.success(discovery.discover(input))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 添加订阅源：抓取并解析，存入数据库
     */
    suspend fun addFeed(url: String): Result<Feed> {
        return try {
            val parsed: ParsedFeed = parser.fetch(url)
            val existing = feedDao.getByUrl(url)
            val feed = if (existing != null) {
                existing.copy(
                    title = parsed.title,
                    siteUrl = parsed.siteUrl,
                    description = parsed.description
                ).also { feedDao.update(it) }
                existing
            } else {
                Feed(
                    title = parsed.title.ifEmpty { url },
                    url = url,
                    siteUrl = parsed.siteUrl,
                    description = parsed.description,
                    faviconUrl = buildFaviconUrl(parsed.siteUrl ?: url)
                ).also { feedDao.insert(it) }
            }
            saveEntries(feed.id, parsed)
            feedDao.updateUnreadCount(feed.id)
            Result.success(feed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 刷新指定订阅源
     */
    suspend fun refreshFeed(feedId: Long) {
        val feed = feedDao.getById(feedId) ?: return
        try {
            val parsed = parser.fetch(feed.url)
            saveEntries(feed.id, parsed)
            feedDao.update(feed.copy(lastUpdated = System.currentTimeMillis()))
            feedDao.updateUnreadCount(feed.id)
        } catch (_: Exception) {
            // 单源失败不中断整体刷新
        }
    }

    /**
     * 刷新所有订阅源
     */
    suspend fun refreshAll() {
        feedDao.observeAll().first().forEach { refreshFeed(it.id) }
    }

    /**
     * 删除订阅源
     */
    suspend fun deleteFeed(feed: Feed) {
        entryDao.deleteByFeed(feed.id)
        feedDao.delete(feed)
    }

    /**
     * 阅读状态操作
     */
    suspend fun markRead(entry: Entry) {
        entryDao.markRead(entry.id)
        feedDao.updateUnreadCount(entry.feedId)
    }

    suspend fun markUnread(entry: Entry) {
        entryDao.markUnread(entry.id)
        feedDao.updateUnreadCount(entry.feedId)
    }

    suspend fun setStarred(entry: Entry, starred: Boolean) {
        entryDao.setStarred(entry.id, starred)
    }

    suspend fun markAllRead(feedId: Long) {
        entryDao.markAllRead(feedId)
        feedDao.updateUnreadCount(feedId)
    }

    /**
     * 保存解析出的文章条目
     */
    private suspend fun saveEntries(feedId: Long, parsed: ParsedFeed) {
        parsed.entries.forEach { pe ->
            val existing = entryDao.getByGuid(feedId, pe.guid)
            if (existing == null) {
                var content = pe.content
                // 正文太短或为空时尝试从原文链接提取全文
                if ((content.isNullOrBlank() || content.length < 200) && !pe.link.isNullOrBlank()) {
                    try {
                        val html = parser.fetchHtml(pe.link)
                        val extracted = ContentExtractor.extract(html)
                        if (extracted.length > (content?.length ?: 0)) {
                            content = extracted
                        }
                    } catch (_: Exception) {
                        // 全文提取失败不影响正常入库
                    }
                }
                entryDao.insert(
                    Entry(
                        feedId = feedId,
                        guid = pe.guid,
                        title = pe.title,
                        link = pe.link,
                        content = content,
                        summary = pe.summary ?: content?.take(200),
                        author = pe.author,
                        publishedAt = pe.publishedAt
                    )
                )
            }
        }
    }

    private fun buildFaviconUrl(siteUrl: String): String {
        return try {
            val uri = android.net.Uri.parse(siteUrl)
            "https://icons.duckduckgo.com/ip3/${uri.host}.ico"
        } catch (_: Exception) {
            ""
        }
    }
}
