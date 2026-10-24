# F1-1 订阅源排查结论

排查对象：原 `RsshubPresets` 硬编码路由 + 本次扩充的官方 RSS。
抓取策略：桌面/移动 UA + Referer 重试、跟随重定向、编码嗅探（UTF-8/GBK）、残缺 XML 尽力解析、RSSHub 多镜像竞速。

## 已修复（路径对照 `/api/namespace` 更正）

| 源名 | 旧 URL | 新 URL | 原因 |
| --- | --- | --- | --- |
| 知乎热榜 | `/zhihu/hotlist` | `/zhihu/hot` | 文档路由更名 |
| B站热门 | `/bilibili/hot` | `/bilibili/popular/all` | 无 hot 路由 |
| B站排行榜 | `/bilibili/ranking/0/3` | `/bilibili/ranking/all` | 参数形态变化 |
| B站番剧表 | `/bilibili/bangumi/timeline` | 改为番剧媒体示例 / 综合热门 | timeline 不存在 |
| GitHub Trending | `/github/trending/daily` | `/github/trending/daily/javascript` | since/language 均为必选 |
| 36氪 | `/36kr/motif/` | `/36kr/hot-list`、`/36kr/newsflashes` | motif 无对应路由 |
| 掘金热门 | `/juejin/trending` | `/juejin/trending/all/monthly` | 缺 category/type |
| 游研社 | `/yystv/` | `/yystv/docs` | 根路径无效 |
| 机核网 | `/gcores/` | `/gcores/news` | 根路径无效 |
| 财新网 | `/caixin/latest` | 保持，确认文档仍有效 | — |
| 人民网 | `/people/paper/` | `/people` | 电子版需页码 |

官方 RSS 侧：解码/UA/RDF（Steam）/Atom link 属性/残缺前言剥离，IT之家、cnBeta、豆瓣书评影评、纽约时报中文网等可正常解析。

## 修不好（保留并在 UI 标注，不删除）

| 源名 | URL | 失败原因分类 |
| --- | --- | --- |
| 微博热搜 | `/weibo/search/hot` | RSSHub 路由不可用：公共实例 503/反爬，需微博 Cookie 自建实例 |
| 小红书热门 | `/xiaohongshu/board` | RSSHub 路由不可用：文档无 board 路由，公共实例 404 |
| 抖音热门 | `/douyin/hot` | RSSHub 路由不可用：依赖登录态/反爬 |
| Pixiv 日榜 | `/pixiv/ranking/day` | 需要特定请求头：Pixiv 令牌，公共实例通常 503 |

这些源仍出现在「添加订阅 → 精选目录 → 暂不可用」分组，条目展示失败分类，点击不可订阅。用户手动添加后刷新失败会把同样的分类写入 `Feed.lastRefreshError`，列表用红色提示。
