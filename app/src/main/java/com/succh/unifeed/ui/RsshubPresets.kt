package com.succh.unifeed.ui

/*
 * RSSHub 精选订阅源，按分类分组。
 * 每个分类对应一个类别，点击可一键订阅。
 * 路由地址格式：https://succh.zone.id/xxx/yyy
 */
data class RsshubRoute(
    val title: String,
    val url: String,
    val description: String = ""
)

data class RsshubCategory(
    val name: String,
    val icon: String,
    val routes: List<RsshubRoute>
)

object RsshubPresets {

    /** 获取所有分类的扁平化路由列表（附带分类名） */
    val allRoutes: List<Pair<String, RsshubRoute>>
        get() = categories.flatMap { cat -> cat.routes.map { cat.name to it } }

    val categories: List<RsshubCategory> = listOf(
        RsshubCategory("社交子版", "social", listOf(
            RsshubRoute("微博热搜", "https://succh.zone.id/weibo/search/hot"),
            RsshubRoute("微博热搜榜", "https://succh.zone.id/weibo/search/hot/1"),
            RsshubRoute("微博用户", "https://succh.zone.id/weibo/user/", "填入用户UID"),
            RsshubRoute("微博关键词", "https://succh.zone.id/weibo/keyword/", "填入关键词"),
            RsshubRoute("知乎热榜", "https://succh.zone.id/zhihu/hotlist"),
            RsshubRoute("知乎日报", "https://succh.zone.id/zhihu/daily"),
            RsshubRoute("知乎专栏", "https://succh.zone.id/zhihu/zhuanlan/", "填入专栏ID"),
            RsshubRoute("B站热门", "https://succh.zone.id/bilibili/hot"),
            RsshubRoute("B站UP主", "https://succh.zone.id/bilibili/user/video/", "填入UID"),
            RsshubRoute("B站排行榜", "https://succh.zone.id/bilibili/ranking/0/3"),
            RsshubRoute("B站番剧表", "https://succh.zone.id/bilibili/bangumi/timeline"),
            RsshubRoute("B站直播", "https://succh.zone.id/bilibili/live/area/", "填入分区ID"),
            RsshubRoute("小红书热门", "https://succh.zone.id/xiaohongshu/board"),
            RsshubRoute("抖音热门", "https://succh.zone.id/douyin/hot"),
            RsshubRoute("即刻动态", "https://succh.zone.id/jike/topics"),
            RsshubRoute("豆瓣热帖", "https://succh.zone.id/douban/group/", "填入小组ID"),
            RsshubRoute("贴吧帖子", "https://succh.zone.id/tieba/forum/", "填入吧名"),
            RsshubRoute("Telegram 频道", "https://succh.zone.id/telegram/channel/", "填入频道名"),
        )),
        RsshubCategory("科技与编程", "tech", listOf(
            RsshubRoute("GitHub Trending", "https://succh.zone.id/github/trending/daily"),
            RsshubRoute("GitHub 仓库", "https://succh.zone.id/github/repos/", "填入owner/name"),
            RsshubRoute("GitHub Issues", "https://succh.zone.id/github/issue/", "填入owner/repo"),
            RsshubRoute("GitHub Releases", "https://succh.zone.id/github/release/", "填入owner/repo"),
            RsshubRoute("GitHub Pull Requests", "https://succh.zone.id/github/pull/", "填入owner/repo"),
            RsshubRoute("Hacker News", "https://succh.zone.id/hackernews"),
            RsshubRoute("V2EX 最新", "https://succh.zone.id/v2ex/topics/latest"),
            RsshubRoute("V2EX 热帖", "https://succh.zone.id/v2ex/topics/hot"),
            RsshubRoute("掘金热门", "https://succh.zone.id/juejin/trending"),
            RsshubRoute("InfoQ 新闻", "https://succh.zone.id/infoq/news"),
            RsshubRoute("SegmentFault", "https://succh.zone.id/segmentfault/blogs"),
            RsshubRoute("Solidot", "https://succh.zone.id/solidot"),
            RsshubRoute("少数派", "https://succh.zone.id/sspai/index"),
            RsshubRoute("博客园", "https://succh.zone.id/cnblogs/"),
            RsshubRoute("CSDN", "https://succh.zone.id/csdn/", "填入用户名"),
            RsshubRoute("开发者头条", "https://succh.zone.id/toutiao/"),
            RsshubRoute("TechCrunch", "https://succh.zone.id/techcrunch"),
            RsshubRoute("The Verge", "https://succh.zone.id/theverge"),
        )),
        RsshubCategory("新闻与资讯", "news", listOf(
            RsshubRoute("36氪", "https://succh.zone.id/36kr/motif/"),
            RsshubRoute("澎湃新闻", "https://succh.zone.id/thepaper/featured"),
            RsshubRoute("界面新闻", "https://succh.zone.id/jiemian"),
            RsshubRoute("华尔街见闻", "https://succh.zone.id/wallstreetcn/news"),
            RsshubRoute("人民日报", "https://succh.zone.id/people/paper/", "填入版次"),
            RsshubRoute("新华社", "https://succh.zone.id/xinhuanet/"),
            RsshubRoute("NPR", "https://succh.zone.id/npr/news"),
            RsshubRoute("Reuters", "https://succh.zone.id/reuters"),
            RsshubRoute("BBC", "https://succh.zone.id/bbc"),
            RsshubRoute("纽约时报", "https://succh.zone.id/nytimes/"),
            RsshubRoute("卫报", "https://succh.zone.id/theguardian/"),
            RsshubRoute("路透中文", "https://succh.zone.id/reuters/china"),
        )),
        RsshubCategory("设计与创意", "design", listOf(
            RsshubRoute("Dribbble 热门", "https://succh.zone.id/dribbble/popular"),
            RsshubRoute("Behance 热门", "https://succh.zone.id/behance/popular"),
            RsshubRoute("站酷精选", "https://succh.zone.id/zcool/recommend"),
            RsshubRoute("花瓣热门", "https://succh.zone.id/huaban/boards"),
            RsshubRoute("优设网", "https://succh.zone.id/uisdc/"),
        )),
        RsshubCategory("生活与娱乐", "life", listOf(
            RsshubRoute("豆瓣电影热映", "https://succh.zone.id/douban/movie/playing"),
            RsshubRoute("豆瓣电影排行榜", "https://succh.zone.id/douban/movie/ranking"),
            RsshubRoute("豆瓣书评", "https://succh.zone.id/douban/book/latest"),
            RsshubRoute("网易云音乐歌单", "https://succh.zone.id/ncm/playlist/", "填入歌单ID"),
            RsshubRoute("天气预警", "https://succh.zone.id/weather/alarm/", "填入省份"),
            RsshubRoute("什么值得买", "https://succh.zone.id/smzdm/"),
            RsshubRoute("知乎话题", "https://succh.zone.id/zhihu/topic/", "填入话题ID"),
        )),
        RsshubCategory("博客与刊物", "blog", listOf(
            RsshubRoute("阮一峰网络日志", "https://succh.zone.id/ruanyifeng/blog"),
            RsshubRoute("爱范儿", "https://succh.zone.id/ifanr"),
            RsshubRoute("AppSo", "https://succh.zone.id/appso"),
            RsshubRoute("Medium", "https://succh.zone.id/medium/", "填入用户/标签"),
            RsshubRoute("知乎日报精选", "https://succh.zone.id/zhihu/dailypicks"),
        )),
        RsshubCategory("游戏", "game", listOf(
            RsshubRoute("Steam 新闻", "https://succh.zone.id/steam/news/", "填入AppID"),
            RsshubRoute("NGA 热帖", "https://succh.zone.id/nga/forum/", "填入版块ID"),
            RsshubRoute("游研社", "https://succh.zone.id/yystv/"),
            RsshubRoute("机核网", "https://succh.zone.id/gcores/"),
            RsshubRoute("游民星空", "https://succh.zone.id/gamersky/"),
            RsshubRoute("Steam 折扣", "https://succh.zone.id/steam/sale/", "填入地区代码"),
        )),
        RsshubCategory("财经商业", "finance", listOf(
            RsshubRoute("财新网", "https://succh.zone.id/caixin/latest"),
            RsshubRoute("雪球热帖", "https://succh.zone.id/xueqiu/hots"),
            RsshubRoute("财联社电报", "https://succh.zone.id/cls/telegraph"),
            RsshubRoute("第一财经", "https://succh.zone.id/yicai/"),
            RsshubRoute("金十数据", "https://succh.zone.id/jin10/"),
            RsshubRoute("东方财富", "https://succh.zone.id/eastmoney/"),
        )),
        RsshubCategory("知识教育", "edu", listOf(
            RsshubRoute("TED 演讲", "https://succh.zone.id/ted/talks"),
            RsshubRoute("果壳网", "https://succh.zone.id/guokr/"),
            RsshubRoute("维基百科", "https://succh.zone.id/wikipedia/", "填入语言/词条"),
            RsshubRoute("每日一文", "https://succh.zone.id/daily/"),
            RsshubRoute("知乎日报科普", "https://succh.zone.id/zhihu/daily/science"),
        )),
        RsshubCategory("视频与直播", "video", listOf(
            RsshubRoute("YouTube 频道", "https://succh.zone.id/youtube/channel/", "填入频道ID"),
            RsshubRoute("YouTube 热门", "https://succh.zone.id/youtube/trending"),
            RsshubRoute("B站直播", "https://succh.zone.id/bilibili/live/area/", "填入分区ID"),
            RsshubRoute("抖音直播", "https://succh.zone.id/douyin/live/", "填入用户ID"),
            RsshubRoute("虎牙直播", "https://succh.zone.id/huya/live/", "填入房间号"),
            RsshubRoute("斗鱼直播", "https://succh.zone.id/douyu/room/", "填入房间号"),
            RsshubRoute("网易云音乐歌手", "https://succh.zone.id/ncm/artist/", "填入歌手ID"),
        )),
        RsshubCategory("综合工具", "tools", listOf(
            RsshubRoute("Product Hunt", "https://succh.zone.id/producthunt/today"),
            RsshubRoute("谷歌趋势", "https://succh.zone.id/google/trends/", "填入关键词"),
            RsshubRoute("少数派 Matrix", "https://succh.zone.id/sspai/matrix"),
            RsshubRoute("豆瓣榜单", "https://succh.zone.id/douban/list/", "填入榜单类型"),
            RsshubRoute("微博热搜趋势", "https://succh.zone.id/weibo/trending/", "填入话题词"),
        )),
    )
}