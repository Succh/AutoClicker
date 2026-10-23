package com.succh.unifeed.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Folo 风格的统一尺寸与形状常量。
 * 卡片、侧边栏、阅读器共用同一套圆角与间距，保证信息层级一致。
 */
object FoloDimens {
    /** 文章卡片圆角 */
    val CardRadius = 16.dp

    /** 文章卡片之间的间距 */
    val CardSpacing = 8.dp

    /** 页面左右内边距 */
    val ScreenHorizontal = 16.dp

    /** 卡片内部内边距 */
    val CardPadding = 16.dp

    /** 未读卡片左侧主色竖条宽度 */
    val UnreadBarWidth = 3.dp

    /** 订阅源列表行高 */
    val FeedItemHeight = 48.dp

    /** 订阅源列表 favicon 尺寸 */
    val FeedFaviconSize = 32.dp

    /** 文章卡片顶部小 favicon */
    val EntryFaviconSize = 16.dp

    /** 文章卡片右侧封面尺寸，固定以免滚动时测量抖动 */
    val EntryCoverWidth = 112.dp
    val EntryCoverHeight = 84.dp

    /** 阅读器图片圆角 */
    val ImageRadius = 12.dp

    /** 侧边栏选中态圆角 */
    val FeedSelectedRadius = 12.dp

    /** 未读数 Pill 徽章圆角（高度的一半，视觉上为胶囊） */
    val PillRadius = 999.dp

    /** 搜索框圆角 */
    val SearchRadius = 12.dp
}

/** Material 3 形状，与 Folo 圆角层级对齐 */
val FoloShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
