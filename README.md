# UniFeed
一款无限订阅源的 RSS 阅读器，受 Folo 启发，自由开源。
## 功能特点
### 订阅管理
- 无限订阅源数量（本地存储，无限制）
- 订阅源添加：支持 RSS/Atom 链接自动发现
- 订阅源重命名、删除
- 文件夹/分类管理：创建、重命名、删除文件夹，订阅源归类
- OPML 导入/导出（支持文件夹结构）
### 阅读体验
- 文章列表：按订阅源/文件夹浏览
- 全部/未读筛选
- 文章搜索（标题+摘要）
- 订阅源搜索（标题+URL）
- 沉浸式阅读器：全文内容提取
- 已读/未读标记，全部标为已读
- 收藏/取消收藏
- 手势操作：右滑切换已读/未读，左滑收藏/取消收藏
### 后台与同步
- 后台自动刷新（可配置间隔：2/6/12/24 小时）
- 下拉刷新
- Favicon 显示
### 界面
- Material 3 设计
- 深色模式跟随系统
## 下载安装
前往 Actions 页面，下载最新构建的 unifeed-debug-apk（Debug 版，可直接安装）。
## 构建
### 环境要求
- JDK 17+
- Android SDK（API 34）
- Gradle 8.10+
### 本地构建
./gradlew assembleDebug
APK 输出：app/build/outputs/apk/debug/app-debug.apk
### GitHub Actions
每次推送到 main 分支会自动构建，可在 Actions 页面下载 APK。
## 技术栈
- 语言：Kotlin
- UI：Jetpack Compose + Material 3
- 数据库：Room（SQLite）
- 后台任务：WorkManager
- 网络：OkHttp + Jsoup（RSS 解析）
- 图片加载：Coil
## 许可证
自由开源。
