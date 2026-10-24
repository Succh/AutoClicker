package com.succh.unifeed.ui

/**
 * 预置订阅源（F1-3）。
 *
 * [url] 官方 RSS 用完整地址；RSSHub 源用相对路径（以 `/` 开头），订阅时拼到当前实例。
 * [source] 取值：`官方 RSS` / `RSSHub`。
 * 修不好的源不会删除，只在 [failureHint] 里标注失败分类，UI 照样展示。
 */
data class BuiltinFeed(
    val title: String,
    val url: String,
    val category: String,
    val source: String,
    val description: String = "",
    val failureHint: String? = null
) {
    val isRsshub: Boolean get() = url.startsWith("/") || source == "RSSHub"
}

data class BuiltinCategory(
    val name: String,
    val feeds: List<BuiltinFeed>
)

object BuiltinFeeds {

    val categories: List<BuiltinCategory> = listOf(
        BuiltinCategory(
            "科技",
            listOf(
                BuiltinFeed("少数派", "https://sspai.com/feed", "科技", "官方 RSS"),
                BuiltinFeed("Solidot", "https://www.solidot.org/index.rss", "科技", "官方 RSS"),
                BuiltinFeed("爱范儿", "https://www.ifanr.com/feed", "科技", "官方 RSS"),
                BuiltinFeed("IT之家", "https://www.ithome.com/rss/", "科技", "官方 RSS"),
                BuiltinFeed("cnBeta.COM.TW", "https://www.cnbeta.com.tw/backend.php", "科技", "官方 RSS"),
                BuiltinFeed("开源中国", "https://www.oschina.net/news/rss", "科技", "官方 RSS"),
                BuiltinFeed("InfoQ 中文", "https://www.infoq.cn/feed", "科技", "官方 RSS"),
                BuiltinFeed("小众软件", "https://www.appinn.com/feed/", "科技", "官方 RSS"),
                BuiltinFeed("异次元软件世界", "https://www.iplaysoft.com/feed", "科技", "官方 RSS"),
                BuiltinFeed("人人都是产品经理", "https://www.woshipm.com/feed", "科技", "官方 RSS"),
                BuiltinFeed("数字尾巴", "https://www.dgtle.com/rss/dgtle.xml", "科技", "官方 RSS"),
                BuiltinFeed("美团技术团队", "https://tech.meituan.com/feed", "科技", "官方 RSS"),
                BuiltinFeed("Hacker News", "https://hnrss.org/frontpage", "科技", "官方 RSS"),
                BuiltinFeed("36氪热榜", "/36kr/hot-list", "科技", "RSSHub"),
                BuiltinFeed("36氪快讯", "/36kr/newsflashes", "科技", "RSSHub"),
                BuiltinFeed("掘金本月最热", "/juejin/trending/all/monthly", "科技", "RSSHub"),
                BuiltinFeed("虎嗅资讯", "/huxiu/article", "科技", "RSSHub")
            )
        ),
        BuiltinCategory(
            "新闻",
            listOf(
                BuiltinFeed("纽约时报中文网", "https://cn.nytimes.com/rss.html", "新闻", "官方 RSS"),
                BuiltinFeed("知乎日报", "/zhihu/daily", "新闻", "RSSHub"),
                BuiltinFeed("知乎热榜", "/zhihu/hot", "新闻", "RSSHub"),
                BuiltinFeed("澎湃新闻头条", "/thepaper/featured", "新闻", "RSSHub"),
                BuiltinFeed("华尔街见闻", "/wallstreetcn/news", "新闻", "RSSHub"),
                BuiltinFeed("财联社电报", "/cls/telegraph", "新闻", "RSSHub"),
                BuiltinFeed("财新最新", "/caixin/latest", "新闻", "RSSHub"),
                BuiltinFeed("人民网头条", "/people", "新闻", "RSSHub")
            )
        ),
        BuiltinCategory(
            "独立博客",
            listOf(
                BuiltinFeed("阮一峰的网络日志", "https://www.ruanyifeng.com/blog/atom.xml", "独立博客", "官方 RSS"),
                BuiltinFeed("云风的 BLOG", "https://blog.codingnow.com/atom.xml", "独立博客", "官方 RSS"),
                BuiltinFeed("月光博客", "https://www.williamlong.info/rss.xml", "独立博客", "官方 RSS"),
                BuiltinFeed("王垠", "https://www.yinwang.org/atom.xml", "独立博客", "官方 RSS"),
                BuiltinFeed("PinSite", "https://immmmm.com/atom.xml", "独立博客", "官方 RSS"),
                BuiltinFeed("少数派 Matrix", "/sspai/matrix", "独立博客", "RSSHub")
            )
        ),
        BuiltinCategory(
            "二次元/ACG",
            listOf(
                BuiltinFeed("Anime News Network", "https://www.animenewsnetwork.com/all/rss.xml", "二次元/ACG", "官方 RSS"),
                BuiltinFeed("豆瓣最受欢迎影评", "https://movie.douban.com/feed/review/movie", "二次元/ACG", "官方 RSS"),
                BuiltinFeed("豆瓣最受欢迎书评", "https://www.douban.com/feed/review/book", "二次元/ACG", "官方 RSS"),
                BuiltinFeed("Bangumi 每日放送", "/bangumi.tv/calendar/today", "二次元/ACG", "RSSHub"),
                BuiltinFeed("B站综合热门", "/bilibili/popular/all", "二次元/ACG", "RSSHub"),
                BuiltinFeed("B站排行榜", "/bilibili/ranking/all", "二次元/ACG", "RSSHub"),
                BuiltinFeed("豆瓣正在热映", "/douban/movie/playing", "二次元/ACG", "RSSHub")
            )
        ),
        BuiltinCategory(
            "社区与开发",
            listOf(
                BuiltinFeed("机核", "https://www.gcores.com/rss", "社区与开发", "官方 RSS"),
                BuiltinFeed("Kotlin Blog", "https://blog.jetbrains.com/kotlin/feed/", "社区与开发", "官方 RSS"),
                BuiltinFeed("Google Developers", "https://developers.googleblog.com/feeds/posts/default", "社区与开发", "官方 RSS"),
                BuiltinFeed("LWN.net", "https://lwn.net/headlines/rss", "社区与开发", "官方 RSS"),
                BuiltinFeed("Phoronix", "https://www.phoronix.com/rss.php", "社区与开发", "官方 RSS"),
                BuiltinFeed("V2EX 最新", "/v2ex/topics/latest", "社区与开发", "RSSHub"),
                BuiltinFeed("V2EX 最热", "/v2ex/topics/hot", "社区与开发", "RSSHub"),
                BuiltinFeed("GitHub Trending", "/github/trending/daily/javascript", "社区与开发", "RSSHub"),
                BuiltinFeed("机核资讯", "/gcores/news", "社区与开发", "RSSHub")
            )
        ),
        BuiltinCategory(
            "游戏",
            listOf(
                BuiltinFeed("Steam 新闻", "https://store.steampowered.com/feeds/news.xml", "游戏", "官方 RSS"),
                BuiltinFeed("游研社", "/yystv/docs", "游戏", "RSSHub")
            )
        )
    )

    val all: List<BuiltinFeed> get() = categories.flatMap { it.feeds }

    /**
     * F1-1 修不好的源：保留在精选里，但标注失败分类，禁止静默删除。
     * 这些路由在公共实例上长期 403/503/反爬，应用侧无法单独修好。
     */
    val unrepaired: List<BuiltinFeed> = listOf(
        BuiltinFeed(
            title = "微博热搜",
            url = "/weibo/search/hot",
            category = "社交媒体",
            source = "RSSHub",
            failureHint = "RSSHub 路由不可用：公共实例对该路由返回 503/反爬，需要配置微博 Cookie 的自建实例"
        ),
        BuiltinFeed(
            title = "小红书热门",
            url = "/xiaohongshu/board",
            category = "社交媒体",
            source = "RSSHub",
            failureHint = "RSSHub 路由不可用：文档中无 board 路由，公共实例 404"
        ),
        BuiltinFeed(
            title = "抖音热门",
            url = "/douyin/hot",
            category = "社交媒体",
            source = "RSSHub",
            failureHint = "RSSHub 路由不可用：需要登录态/反爬，公共实例无法抓取"
        ),
        BuiltinFeed(
            title = "Pixiv 日榜",
            url = "/pixiv/ranking/day",
            category = "二次元/ACG",
            source = "RSSHub",
            failureHint = "需要特定请求头：Pixiv 路由依赖令牌，公共实例通常 503"
        )
    )

    /** 精选目录展示：可用源 + 标注了失败原因的源（不删除） */
    val displayCategories: List<BuiltinCategory>
        get() = categories + BuiltinCategory("暂不可用（已标注原因）", unrepaired)
}
