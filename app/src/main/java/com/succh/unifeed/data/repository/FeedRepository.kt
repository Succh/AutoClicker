package com.succh.unifeed.data.repository

import com.succh.unifeed.data.db.AppDatabase
import com.succh.unifeed.data.db.entity.Entry
import com.succh.unifeed.data.db.entity.Feed
import com.succh.unifeed.data.model.ParsedFeed
import com.succh.unifeed.data.rss.ContentExtractor
import com.succh.unifeed.data.rss.DiscoveredFeed
import com.succh.unifeed.data.rss.FeedDiscovery
import com.succh.unifeed.data.rss.RssParser
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    suspend fun discoverFeeds(input: String): Result<List<DiscoveredFeed>> {
        return try {
            Result.success(discovery.discover(input))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
     */
    suspend fun addFeedInstant(url: String, titleHint: String? = null): Result<Feed> {
        return try {
            val existing = feedDao.getByUrl(url)
            if (existing != null) {
                return Result.success(existing)
            }
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
                }
            }
            Result.success(savedFeed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun inferTitleFromUrl(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            val path = uri.path?.trim('/') ?: ""
            if (url.contains("rsshub.app") && path.isNotEmpty()) {
                val parts = path.split("/")
                parts.takeLast(2).joinToString("/")
            } else {
                uri.host ?: url
            }
        } catch (_: Exception) {
            url
        }
    }

    /**
     * RSSHub 镜像回退：
     * 1. 非 rsshub 直接抓取
     * 2. rsshub 优先试 cups.moe（已知稳定），再试其他镜像
     * 3. 检测 CF 防护页面，自动跳过
     */
    private suspend fun fetchWithRsshubFallback(url: String): Pair<ParsedFeed, String> {
        if (!url.contains("rsshub.app")) {
            // 非 RSSHub 直接抓取，增加重试
            return try {
                parser.fetch(url) to url
            } catch (e: Exception) {
                throw Exception("源抓取失败: ${e.message}")
            }
        }

        // RSSHub 优先尝试可靠镜像
        val mirrors = listOf(
            RSSHUB_CUPS,      // 最稳定
            RSSHUB_BALANCER,  // 备选
            RSSHUB_SLARKER,   // 备选
            RSSHUB_OFFICIAL   // 最后
        )
        
        // 并发尝试前两个镜像
        var successResult: Pair<ParsedFeed, String>? = null
        coroutineScope {
            val deferreds = mirrors.take(2).map { mirror ->
                async {
                    val candidate = url.replace(RSSHUB_OFFICIAL, mirror)
                    try {
                        val result = parser.fetch(candidate)
                        // 检测是否是 CF 防护页面
                        if (isCloudflareChallenge(result.content ?: "")) {
                            null
                        } else {
                            result to candidate
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            for (deferred in deferreds) {
                val result = deferred.await()
                if (result != null) {
                    successResult = result
                    break
                }
            }
        }
        if (successResult != null) return successResult!!

        // 串行尝试剩余镜像
        for (i in 2 until mirrors.size) {
            val mirror = mirrors[i]
            val candidate = url.replace(RSSHUB_OFFICIAL, mirror)
            try {
                val result = parser.fetch(candidate)
                if (!isCloudflareChallenge(result.content ?: "")) {
                    return result to candidate
                }
            } catch (_: Exception) {
            }
        }
        throw Exception("RSSHub 所有镜像均不可用，该路由可能暂不支持")
    }

    /**
     * 检测是否是 Cloudflare 防护页面
     */
    private fun isCloudflareChallenge(xml: String): Boolean {
        return xml.contains("Just a moment...") 
            || xml.contains("cf-chl")
            || xml.contains("challenge-platform")
            || xml.contains("Enable JavaScript and cookies to continue")
    }

    suspend fun refreshFeed(feedId: Long) {
        val feed = feedDao.getById(feedId) ?: return
        try {
            val (parsed, _) = fetchWithRsshubFallback(feed.url)
            saveEntries(feed.id, parsed)
            feedDao.update(feed.copy(lastUpdated = System.currentTimeMillis()))
            feedDao.updateUnreadCount(feed.id)
        } catch (_: Exception) {
        }
    }

    suspend fun refreshAll() {
        feedDao.observeAll().first().forEach { refreshFeed(it.id) }
    }

    suspend fun deleteFeed(feed: Feed) {
        entryDao.deleteByFeed(feed.id)
        feedDao.delete(feed)
    }

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

    private suspend fun saveEntries(feedId: Long, parsed: ParsedFeed) {
        parsed.entries.forEach { pe ->
            val existing = entryDao.getByGuid(feedId, pe.guid)
            if (existing == null) {
                var content = pe.content
                if ((content.isNullOrBlank() || content.length < 200) && !pe.link.isNullOrBlank()) {
                    try {
                        val html = parser.fetchHtml(pe.link)
                        val extracted = ContentExtractor.extract(html)
                        if (extracted.length > (content?.length ?: 0)) {
                            content = extracted
                        }
                    } catch (_: Exception) {
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
        const val RSSHUB_CUPS = "https://rsshub.cups.moe"
        const val RSSHUB_BALANCER = "https://rsshub-balancer.virworks.moe"
        const val RSSHUB_SLARKER = "https://hub.slarker.me"
    }
}