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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
     * 若为 RSSHub 官方实例链接且官方不可达，自动切换公共镜像重试。
     */
    suspend fun addFeed(url: String): Result<Feed> {
        return try {
            val (parsed, actualUrl) = fetchWithRsshubFallback(url)
            val existing = feedDao.getByUrl(url) ?: feedDao.getByUrl(actualUrl)
            val feed = if (existing != null) {
                existing.copy(
                    title = parsed.title,
                    siteUrl = parsed.siteUrl,
                    description = parsed.description
                ).also { feedDao.update(it) }
                existing
            } else {
                val newFeed = Feed(
                    title = parsed.title.ifEmpty { actualUrl },
                    url = actualUrl,
                    siteUrl = parsed.siteUrl,
                    description = parsed.description,
                    faviconUrl = buildFaviconUrl(parsed.siteUrl ?: actualUrl)
                )
                val newId = feedDao.insert(newFeed)
                newFeed.copy(id = newId)
            }
            saveEntries(feed.id, parsed)
            feedDao.updateUnreadCount(feed.id)
            Result.success(feed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 即时添加订阅源（Folo 式）：先存 URL 和标题到数据库，立即返回成功；
     * 后台异步 fetch 文章内容。用户无需等待网络请求。
     * RSSHub 源用路由路径推断标题，普通 URL 用域名做标题。
     */
    suspend fun addFeedInstant(url: String, titleHint: String? = null): Result<Feed> {
        return try {
            // 已存在则直接返回
            val existing = feedDao.getByUrl(url)
            if (existing != null) {
                return Result.success(existing)
            }
            // 推断标题
            val inferredTitle = titleHint ?: inferTitleFromUrl(url)
            val newFeed = Feed(
                title = inferredTitle,
                url = url,
                siteUrl = null,
                description = "",
                faviconUrl = ""
            )
            val newId = feedDao.insert(newFeed)
            val savedFeed = newFeed.copy(id = newId)
            // 后台异步抓取文章
            bgScope.launch {
                try {
                    val (parsed, actualUrl) = fetchWithRsshubFallback(url)
                    feedDao.update(savedFeed.copy(
                        title = parsed.title.ifEmpty { inferredTitle },
                        siteUrl = parsed.siteUrl,
                        description = parsed.description,
                        faviconUrl = buildFaviconUrl(parsed.siteUrl ?: actualUrl),
                        url = actualUrl,
                        lastUpdated = System.currentTimeMillis()
                    ))
                    saveEntries(newId, parsed)
                    feedDao.updateUnreadCount(newId)
                } catch (_: Exception) {
                    // 后台抓取失败不影响订阅成功
                    // 用户下次手动刷新时会重试
                }
            }
            Result.success(savedFeed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从 URL 推断一个人类可读的标题
     */
    private fun inferTitleFromUrl(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            val path = uri.path?.trim('/') ?: ""
            // rsshub.app/weibo/search/hot → 微博热搜
            if (url.contains("rsshub.app") && path.isNotEmpty()) {
                val parts = path.split("/")
                // 取最后 1-2 段拼成简短标题
                val tail = parts.takeLast(2).joinToString("/")
                tail
            } else {
                uri.host ?: url
            }
        } catch (_: Exception) {
            url
        }
    }

    /**
     * RSSHub 官方实例不可达时，自动遍历公共镜像重试。
     * 返回 (解析结果, 实际成功抓取的 URL)。
     */
    private suspend fun fetchWithRsshubFallback(url: String): Pair<ParsedFeed, String> {
        if (!url.contains("rsshub.app")) {
            return parser.fetch(url) to url
        }
        var lastError: Exception? = null
        for (mirror in RSSHUB_MIRRORS) {
            val candidate = if (mirror == RSSHUB_OFFICIAL) url else url.replace(RSSHUB_OFFICIAL, mirror)
            try {
                return parser.fetch(candidate) to candidate
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: Exception("RSSHub 实例均不可用")
    }

    /**
     * 刷新指定订阅源
     */
    suspend fun refreshFeed(feedId: Long) {
        val feed = feedDao.getById(feedId) ?: return
        try {
            val (parsed, _) = fetchWithRsshubFallback(feed.url)
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

    private companion object {
        const val RSSHUB_OFFICIAL = "https://rsshub.app"
        val RSSHUB_MIRRORS = listOf(
            RSSHUB_OFFICIAL,
            "https://rsshub.rssforever.com",
            "https://rsshub.pseudoyu.com",
            "https://rsshub.ktachibana.party",
            "https://rsshub.moeyy.xyz"
        )
    }
}