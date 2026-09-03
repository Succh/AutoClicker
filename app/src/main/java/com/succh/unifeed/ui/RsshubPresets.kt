package com.succh.unifeed.ui

/*
 * RSSHub 精选订阅源，按分类分组。
 * 每个分类对应一个类别，点击可一键订阅。
 * 路由地址格式：https://rsshub.app/xxx/yyy
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
            RsshubRoute("微博热搜", "https://rsshub.app/weibo/search/hot"),
            RsshubRoute("微博热搜榜", "https://rsshub.app/weibo/search/hot/1"),
            RsshubRoute("微博用户", "https://rsshub.app/weibo/user/", "填入用户UID"),
            RsshubRoute("微博图片", "https://rsshub.app/weibo/user/", "填入用户UID（图片模式）"),
            RsshubRoute("知乎热榜", "https://rsshub.app/zhihu/hotlist"),
            RsshubRoute("知乎日报", "https://rsshub.app/zhihu/daily"),
            RsshubRoute("知乎专栏", "https://rsshub.app/zhihu/zhuanlan/", "填入专栏ID"),
            RsshubRoute("B站热门", "https://rsshub.app/bilibili/hot"),
            RsshubRoute("B站UP主", "https://rsshub.app/bilibili/user/video/", "填入UID"),
            RsshubRoute("B站排行榜", "https://rsshub.app/bilibili/ranking/0/3"),
            RsshubRoute("B站番剧表", "https://rsshub.app/bilibili/bangumi/timeline"),
            RsshubRoute("B站直播", "https://rsshub.app/bilibili/live/area/", "填入分区ID"),
            RsshubRoute("小红书热门", "https://rsshub.app/xiaohongshu/board"),
            RsshubRoute("抖音热门", "https://rsshub.app/douyin/hot"),
            RsshubRoute("即刻动态", "https://rsshub.app/jike/topics"),
            RsshubRoute("豆瓣热帖", "https://rsshub.app/douban/group/", "填入小组ID"),
            RsshubRoute("贴吧帖子", "https://rsshub.app/tieba/forum/", "填入吧名"),
            RsshubRoute("Telegram 频道", "https://rsshub.app/telegram/channel/", "填入频道名"),
        )),
        RsshubCategory("科技与编程", "tech", listOf(
            RsshubRoute("GitHub Trending", "https://rsshub.app/github/trending/daily"),
            RsshubRoute("GitHub 仓库", "https://rsshub.app/github/repos/", "填入owner/name"),
            RsshubRoute("GitHub Issues", "https://rsshub.app/github/issue/", "填入owner/repo"),
            RsshubRoute("GitHub Releases", "https://rsshub.app/github/release/", "填入owner/repo"),
            RsshubRoute("GitHub Pull Requests", "https://rsshub.app/github/pull/", "填入owner/repo"),
            RsshubRoute("Hacker News", "https://rsshub.app/hackernews"),
            RsshubRoute("V2EX 最新", "https://rsshub.app/v2ex/topics/latest"),
            RsshubRoute("V2EX 热帖", "https://rsshub.app/v2ex/topics/hot"),
            RsshubRoute("掘金热门", "https://rsshub.app/juejin/trending"),
            RsshubRoute("InfoQ 新闻", "https://rsshub.app/infoq/news"),
            RsshubRoute("SegmentFault", "https://rsshub.app/segmentfault/blogs"),
            RsshubRoute("Solidot", "https://rsshub.app/solidot"),
            RsshubRoute("少数派", "https://rsshub.app/sspai/index"),
            RsshubRoute("博客园", "https://rsshub.app/cnblogs/"),
            RsshubRoute("CSDN", "https://rsshub.app/csdn/", "填入用户名"),
            RsshubRoute("开发者头条", "https://rsshub.app/toutiao/"),
            RsshubRoute("TechCrunch", "https://rsshub.app/techcrunch"),
            RsshubRoute("The Verge", "https://rsshub.app/theverge"),
        )),
        RsshubCategory("新闻与资讯", "news", listOf(
            RsshubRoute("36氪", "https://rsshub.app/36kr/motif/"),
            RsshubRoute("澎湃新闻", "https://rsshub.app/thepaper/featured"),
            RsshubRoute("界面新闻", "https://rsshub.app/jiemian"),
            RsshubRoute("华尔街见闻", "https://rsshub.app/wallstreetcn/news"),
            RsshubRoute("人民日报", "https://rsshub.app/people/paper/", "填入版次"),
            RsshubRoute("新华社", "https://rsshub.app/xinhuanet/"),
            RsshubRoute("NPR", "https://rsshub.app/npr/news"),
            RsshubRoute("Reuters", "https://rsshub.app/reuters"),
            RsshubRoute("BBC", "https://rsshub.app/bbc"),
            RsshubRoute("纽约时报", "https://rsshub.app/nytimes/"),
            RsshubRoute("卫报", "https://rsshub.app/theguardian/"),
            RsshubRoute("路透中文", "https://rsshub.app/reuters/china"),
        )),
        RsshubCategory("设计与创意", "design", listOf(
            RsshubRoute("Dribbble 热门", "https://rsshub.app/dribbble/popular"),
            RsshubRoute("Behance 热门", "https://rsshub.app/behance/popular"),
            RsshubRoute("站酷精选", "https://rsshub.app/zcool/recommend"),
            RsshubRoute("花瓣热门", "https://rsshub.app/huaban/boards"),
            RsshubRoute("优设网", "https://rsshub.app/uisdc/"),
        )),
        RsshubCategory("生活与娱乐", "life", listOf(
            RsshubRoute("豆瓣电影热映", "https://rsshub.app/douban/movie/playing"),
            RsshubRoute("豆瓣电影排行榜", "https://rsshub.app/douban/movie/ranking"),
            RsshubRoute("豆瓣书评", "https://rsshub.app/douban/book/latest"),
            RsshubRoute("网易云音乐歌单", "https://rsshub.app/ncm/playlist/", "填入歌单ID"),
            RsshubRoute("天气预警", "https://rsshub.app/weather/alarm/", "填入省份"),
            RsshubRoute("什么值得买", "https://rsshub.app/smzdm/"),
            RsshubRoute("知乎话题", "https://rsshub.app/zhihu/topic/", "填入话题ID"),
        )),
        RsshubCategory("博客与刊物", "blog", listOf(
            RsshubRoute("阮一峰网络日志", "https://rsshub.app/ruanyifeng/blog"),
            RsshubRoute("爱范儿", "https://rsshub.app/ifanr"),
            RsshubRoute("AppSo", "https://rsshub.app/appso"),
            RsshubRoute("Medium", "https://rsshub.app/medium/", "填入用户/标签"),
            RsshubRoute("知乎日报精选", "https://rsshub.app/zhihu/dailypicks"),
        )),
        RsshubCategory("游戏", "game", listOf(
            RsshubRoute("Steam 新闻", "https://rsshub.app/steam/news/", "填入AppID"),
            RsshubRoute("NGA 热帖", "https://rsshub.app/nga/forum/", "填入版块ID"),
            RsshubRoute("游研社", "https://rsshub.app/yystv/"),
            RsshubRoute("机核网", "https://rsshub.app/gcores/"),
            RsshubRoute("游民星空", "https://rsshub.app/gamersky/"),
            RsshubRoute("Steam 折扣", "https://rsshub.app/steam/sale/", "填入地区代码"),
        )),
        RsshubCategory("财经商业", "finance", listOf(
            RsshubRoute("财新网", "https://rsshub.app/caixin/latest"),
            RsshubRoute("雪球热帖", "https://rsshub.app/xueqiu/hots"),
            RsshubRoute("财联社电报", "https://rsshub.app/cls/telegraph"),
            RsshubRoute("第一财经", "https://rsshub.app/yicai/"),
            RsshubRoute("金十数据", "https://rsshub.app/jin10/"),
            RsshubRoute("东方财富", "https://rsshub.app/eastmoney/"),
        )),
        RsshubCategory("知识教育", "edu", listOf(
            RsshubRoute("TED 演讲", "https://rsshub.app/ted/talks"),
            RsshubRoute("果壳网", "https://rsshub.app/guokr/"),
            RsshubRoute("维基百科", "https://rsshub.app/wikipedia/", "填入语言/词条"),
            RsshubRoute("每日一文", "https://rsshub.app/daily/"),
            RsshubRoute("知乎日报科普", "https://rsshub.app/zhihu/daily/science"),
        )),
        RsshubCategory("视频与直播", "video", listOf(
            RsshubRoute("YouTube 频道", "https://rsshub.app/youtube/channel/", "填入频道ID"),
            RsshubRoute("YouTube 热门", "https://rsshub.app/youtube/trending"),
            RsshubRoute("B站直播", "https://rsshub.app/bilibili/live/area/", "填入分区ID"),
            RsshubRoute("抖音直播", "https://rsshub.app/douyin/live/", "填入用户ID"),
            RsshubRoute("虎牙直播", "https://rsshub.app/huya/live/", "填入房间号"),
            RsshubRoute("斗鱼直播", "https://rsshub.app/douyu/room/", "填入房间号"),
            RsshubRoute("网易云音乐歌手", "https://rsshub.app/ncm/artist/", "填入歌手ID"),
        )),
        RsshubCategory("综合工具", "tools", listOf(
            RsshubRoute("Product Hunt", "https://rsshub.app/producthunt/today"),
            RsshubRoute("谷歌趋势", "https://rsshub.app/google/trends/", "填入关键词"),
            RsshubRoute("少数派 Matrix", "https://rsshub.app/sspai/matrix"),
            RsshubRoute("豆瓣榜单", "https://rsshub.app/douban/list/", "填入榜单类型"),
            RsshubRoute("微博热搜趋势", "https://rsshub.app/weibo/trending/", "填入话题词"),
        )),
    )
}