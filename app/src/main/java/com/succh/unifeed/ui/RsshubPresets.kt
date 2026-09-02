package com.succh.unifeed.ui

/*
 * RSSHub 精选賨捡度巴，按分并分组。
 * 每毑返回对帐一个不时，竹僻可一键订阅。
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

    val categories: List<RsshubCategory> = listOf(
        RsshubCategory("社交子伓", "social", listOf(
            RsshubRoute("微博热搜", "https://rsshub.app/weibo/search/hot"),
            RsshubRoute("微博用户", "https://rsshub.app/weibo/user/", "填入用户UID"),
            RsshubRoute("知乎热榜", "https://rsshub.app/zhihu/hotlist"),
            RsshubRoute("知乎日报", "https://rsshub.app/zhihu/daily"),
            RsshubRoute("知乎专栏", "https://rsshub.app/zhihu/zhuanlan/", "填入专栏ID"),
            RsshubRoute("B站热门", "https://rsshub.app/bilibili/hot"),
            RsshubRoute("B站UP主", "https://rsshub.app/bilibili/user/video/", "填入UID"),
            RsshubRoute("B站排行榜", "https://rsshub.app/bilibili/ranking/0/3"),
            RsshubRoute("小红书热门", "https://rsshub.app/xiaohongshu/board"),
            RsshubRoute("抖音热门", "https://rsshub.app/douyin/hot"),
            RsshubRoute("即刻动态", "https://rsshub.app/jike/topics"),
            RsshubRoute("豆瓣热帖", "https://rsshub.app/douban/group/", "填入小组ID"),
        )),
        RsshubCategory("科技与编程", "tech", listOf(
            RsshubRoute("GitHub Trending", "https://rsshub.app/github/trending/daily"),
            RsshubRoute("GitHub 仓库", "https://rsshub.app/github/repos/", "填入owner/name"),
            RsshubRoute("Hacker News", "https://rsshub.app/hackernews"),
            RsshubRoute("V2EX 最新", "https://rsshub.app/v2ex/topics/latest"),
            RsshubRoute("V2EX 热帖", "https://rsshub.app/v2ex/topics/hot"),
            RsshubRoute("掘金热门", "https://rsshub.app/juejin/trending"),
            RsshubRoute("InfoQ 新闻", "https://rsshub.app/infoq/news"),
            RsshubRoute("SegmentFault", "https://rsshub.app/segmentfault/blogs"),
            RsshubRoute("Solidot", "https://rsshub.app/solidot"),
            RsshubRoute("少数派", "https://rsshub.app/sspai/index"),
            RsshubRoute("TechCrunch", "https://rsshub.app/techcrunch"),
            RsshubRoute("The Verge", "https://rsshub.app/theverge"),
        )),
        RsshubCategory("新闻与资讯", "news", listOf(
            RsshubRoute("36氪", "https://rsshub.app/36kr/motif/"),
            RsshubRoute("澎湃新闻", "https://rsshub.app/thepaper/featured"),
            RsshubRoute("界面新闻", "https://rsshub.app/jiemian"),
            RsshubRoute("华尔街见闻", "https://rsshub.app/wallstreetcn/news"),
            RsshubRoute("NPR", "https://rsshub.app/npr/news"),
            RsshubRoute("Reuters", "https://rsshub.app/reuters"),
            RsshubRoute("BBC", "https://rsshub.app/bbc"),
            RsshubRoute("路透中文", "https://rsshub.app/reuters/china"),
        )),
        RsshubCategory("设计与创意", "design", listOf(
            RsshubRoute("Dribbble 热门", "https://rsshub.app/dribbble/popular"),
            RsshubRoute("Behance 热门", "https://rsshub.app/behance/popular"),
            RsshubRoute("站酷精选", "https://rsshub.app/zcool/recommend"),
            RsshubRoute("花瓣热门", "https://rsshub.app/huaban/boards"),
        )),
        RsshubCategory("生活与娱乐", "life", listOf(
            RsshubRoute("豆瓣电影热映", "https://rsshub.app/douban/movie/playing"),
            RsshubRoute("豆瓣书评", "https://rsshub.app/douban/book/latest"),
            RsshubRoute("网易云音乐歌单", "https://rsshub.app/ncm/playlist/", "填入歌单ID"),
            RsshubRoute("天气预警", "https://rsshub.app/weather/alarm/", "填入省份"),
        )),
        RsshubCategory("博客与刊物", "blog", listOf(
            RsshubRoute("阮一峰网络日志", "https://rsshub.app/ruanyifeng/blog"),
            RsshubRoute("爱范儿", "https://rsshub.app/ifanr"),
            RsshubRoute("AppSo", "https://rsshub.app/appso"),
            RsshubRoute("Medium", "https://rsshub.app/medium/", "填入用户/标签"),
        )),
    )
}