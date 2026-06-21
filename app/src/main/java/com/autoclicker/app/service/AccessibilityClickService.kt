package com.autoclicker.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.autoclicker.app.model.Action
import com.autoclicker.app.model.ActionType
import com.autoclicker.app.model.Point
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 无障碍服务 - 核心点击引擎
 * 类似 MacroDroid 的辅助功能服务，通过 Accessibility API 操作 UI
 */
class AccessibilityClickService : AccessibilityService() {

    companion object {
        private const val TAG = "AccessibilityClick"

        @Volatile
        var instance: AccessibilityClickService? = null
            private set

        var isRunning: Boolean = false
            private set

        // 最近的截图结果
        @Volatile
        var lastScreenshotResult: String? = null
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
        Log.i(TAG, "无障碍服务已连接")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要处理事件，我们只主动操作
    }

    override fun onInterrupt() {
        Log.i(TAG, "无障碍服务被中断")
    }

    override fun onDestroy() {
        instance = null
        isRunning = false
        serviceScope.cancel()
        super.onDestroy()
        Log.i(TAG, "无障碍服务已销毁")
    }

    // ==================== 动作执行 ====================

    /**
     * 执行单个动作，返回是否成功
     */
    suspend fun executeAction(action: Action): Pair<Boolean, String> {
        if (!action.enabled) return Pair(true, "跳过（已禁用）")

        return when (action.type) {
            ActionType.CLICK_TEXT -> clickByText(action.target, action.retryCount, action.timeoutMs)
            ActionType.CLICK_ID -> clickById(action.target, action.retryCount, action.timeoutMs)
            ActionType.CLICK坐标 -> {
                if (action.coordinates != null) {
                    clickByCoordinates(action.coordinates.x, action.coordinates.y)
                } else {
                    Pair(false, "缺少坐标参数")
                }
            }
            ActionType.CLICK_NODE -> clickByNodeIndex(action.target.toIntOrNull() ?: 0)

            ActionType.LONG_CLICK_TEXT -> longClickByText(action.target, action.retryCount)
            ActionType.LONG_CLICK_ID -> longClickById(action.target, action.retryCount)

            ActionType.INPUT_TEXT -> inputText(action.target)
            ActionType.INPUT_ID -> inputToField(action.target, action.value)

            ActionType.SCROLL_DOWN -> scrollDown()
            ActionType.SCROLL_UP -> scrollUp()
            ActionType.SCROLL_TO_TEXT -> scrollToText(action.target, action.timeoutMs)

            ActionType.WAIT_TEXT -> waitForText(action.target, action.timeoutMs)
            ActionType.WAIT_ID -> waitForId(action.target, action.timeoutMs)
            ActionType.WAIT_GONE -> waitForGone(action.target, action.timeoutMs)

            ActionType.LAUNCH_APP -> launchApp(action.target)
            ActionType.BACK -> performBack()
            ActionType.HOME -> performHome()
            ActionType.RECENT -> performRecent()

            ActionType.SWIPE -> {
                if (action.coordinates != null) {
                    val parts = action.extra.split(",").map { it.trim().toFloatOrNull() ?: 0f }
                    if (parts.size >= 2) {
                        swipe(action.coordinates.x, action.coordinates.y, parts[0], parts[1])
                    } else {
                        Pair(false, "Swipe 需要 extra: endX,endY")
                    }
                } else {
                    Pair(false, "Swipe 需要起点坐标")
                }
            }

            ActionType.SCREENSHOT -> {
                lastScreenshotResult = takeScreenshot()
                Pair(true, "截图已保存")
            }

            ActionType.SCREENSHOT_AND_ANALYZE -> {
                lastScreenshotResult = takeScreenshot()
                Pair(true, "截图完成，请通过 /screenshot 查看")
            }

            ActionType.SPEAK -> {
                val intent = Intent("com.autoclicker.app.SPEAK")
                intent.putExtra("text", action.target)
                sendBroadcast(intent)
                Pair(true, "已发送语音请求")
            }

            ActionType.COMMENT -> Pair(true, "注释，跳过")

            else -> Pair(false, "不支持的动作类型: ${action.type}")
        }
    }

    // ==================== 点击操作 ====================

    private suspend fun clickByText(
        text: String,
        retries: Int = 3,
        timeout: Long = 10000
    ): Pair<Boolean, String> {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            val node = findNodeByText(text)
            if (node != null) {
                return clickNode(node)
            }
            delay(200)
        }
        return Pair(false, "未找到文字: $text (${timeout}ms超时)")
    }

    private suspend fun clickById(
        id: String,
        retries: Int = 3,
        timeout: Long = 10000
    ): Pair<Boolean, String> {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            val node = findNodeById(id)
            if (node != null) {
                return clickNode(node)
            }
            delay(200)
        }
        return Pair(false, "未找到ID: $id (${timeout}ms超时)")
    }

    private suspend fun clickByCoordinates(x: Float, y: Float): Pair<Boolean, String> {
        return suspendCoroutine { cont ->
            val path = Path()
            path.moveTo(x, y)
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    cont.resume(Pair(true, "点击坐标 ($x, $y)"))
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    cont.resume(Pair(false, "手势被取消"))
                }
            }, null)
        }
    }

    private suspend fun clickByNodeIndex(index: Int): Pair<Boolean, String> {
        val rootNode = rootInActiveWindow ?: return Pair(false, "无法获取当前窗口")
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        findClickableNodes(rootNode, clickableNodes)

        return if (index < clickableNodes.size) {
            clickNode(clickableNodes[index])
        } else {
            Pair(false, "索引 $index 超出范围（共 ${clickableNodes.size} 个可点击元素）")
        }
    }

    private fun findClickableNodes(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (node.isClickable) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findClickableNodes(child, result)
        }
    }

    // ==================== 长按 ====================

    private suspend fun longClickByText(text: String, retries: Int = 3): Pair<Boolean, String> {
        for (i in 0..retries) {
            val node = findNodeByText(text) ?: continue
            return longClickNode(node)
        }
        return Pair(false, "未找到文字: $text")
    }

    private suspend fun longClickById(id: String, retries: Int = 3): Pair<Boolean, String> {
        for (i in 0..retries) {
            val node = findNodeById(id) ?: continue
            return longClickNode(node)
        }
        return Pair(false, "未找到ID: $id")
    }

    private suspend fun longClickNode(node: AccessibilityNodeInfo): Pair<Boolean, String> {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val x = rect.centerX().toFloat()
        val y = rect.centerY().toFloat()

        return suspendCoroutine { cont ->
            val path = Path()
            path.moveTo(x, y)
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
                .build()

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    cont.resume(Pair(true, "长按成功"))
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    cont.resume(Pair(false, "长按被取消"))
                }
            }, null)
        }
    }

    // ==================== 输入文字 ====================

    private fun inputText(text: String): Pair<Boolean, String> {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val focused = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        return if (focused?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments) == true) {
            Pair(true, "输入文字: $text")
        } else {
            Pair(false, "无法输入文字（未找到输入框）")
        }
    }

    private fun inputToField(id: String, text: String): Pair<Boolean, String> {
        val node = findNodeById(id) ?: return Pair(false, "未找到输入框: $id")
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
            Pair(true, "向 $id 输入: $text")
        } else {
            Pair(false, "输入失败")
        }
    }

    // ==================== 滚动 ====================

    private fun scrollDown(): Pair<Boolean, String> {
        val node = rootInActiveWindow ?: return Pair(false, "无法获取窗口")
        return if (node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
            Pair(true, "向下滚动")
        } else {
            // fallback: 用手势模拟滚动
            Pair(false, "滚动失败，尝试手势滚动")
        }
    }

    private fun scrollUp(): Pair<Boolean, String> {
        val node = rootInActiveWindow ?: return Pair(false, "无法获取窗口")
        return if (node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
            Pair(true, "向上滚动")
        } else {
            Pair(false, "滚动失败")
        }
    }

    private suspend fun scrollToText(text: String, timeout: Long = 15000): Pair<Boolean, String> {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            if (findNodeByText(text) != null) {
                return Pair(true, "找到文字: $text")
            }
            scrollDown()
            delay(500)
        }
        return Pair(false, "滚动超时，未找到: $text")
    }

    // ==================== 等待 ====================

    private suspend fun waitForText(text: String, timeout: Long = 10000): Pair<Boolean, String> {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            if (findNodeByText(text) != null) {
                return Pair(true, "文字出现: $text")
            }
            delay(200)
        }
        return Pair(false, "等待超时，文字未出现: $text")
    }

    private suspend fun waitForId(id: String, timeout: Long = 10000): Pair<Boolean, String> {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            if (findNodeById(id) != null) {
                return Pair(true, "ID出现: $id")
            }
            delay(200)
        }
        return Pair(false, "等待超时，ID未出现: $id")
    }

    private suspend fun waitForGone(text: String, timeout: Long = 10000): Pair<Boolean, String> {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            if (findNodeByText(text) == null) {
                return Pair(true, "文字已消失: $text")
            }
            delay(200)
        }
        return Pair(false, "等待超时，文字仍然存在: $text")
    }

    // ==================== App 控制 ====================

    private fun launchApp(packageName: String): Pair<Boolean, String> {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Pair(true, "启动应用: $packageName")
        } else {
            Pair(false, "未找到应用: $packageName")
        }
    }

    private fun performBack(): Pair<Boolean, String> {
        return if (performGlobalAction(GLOBAL_ACTION_BACK)) {
            Pair(true, "返回")
        } else {
            Pair(false, "返回失败")
        }
    }

    private fun performHome(): Pair<Boolean, String> {
        return if (performGlobalAction(GLOBAL_ACTION_HOME)) {
            Pair(true, "回到主页")
        } else {
            Pair(false, "主页操作失败")
        }
    }

    private fun performRecent(): Pair<Boolean, String> {
        return if (performGlobalAction(GLOBAL_ACTION_RECENTS)) {
            Pair(true, "显示最近任务")
        } else {
            Pair(false, "最近任务失败")
        }
    }

    // ==================== 滑动 ====================

    private suspend fun swipe(
        startX: Float, startY: Float, endX: Float, endY: Float
    ): Pair<Boolean, String> {
        return suspendCoroutine { cont ->
            val path = Path()
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
                .build()

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    cont.resume(Pair(true, "滑动成功"))
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    cont.resume(Pair(false, "滑动被取消"))
                }
            }, null)
        }
    }

    // ==================== 截图 ====================

    private fun takeScreenshot(): String {
        return "截图功能需要 Android 11+ 的 MediaProjection API"
    }

    // ==================== 节点查找 ====================

    private fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findNodeByTextRecursive(rootNode, text)
    }

    private fun findNodeByTextRecursive(
        node: AccessibilityNodeInfo,
        text: String
    ): AccessibilityNodeInfo? {
        // 精确匹配或包含匹配
        val nodeText = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        if (nodeText.contains(text, ignoreCase = true) ||
            desc.contains(text, ignoreCase = true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByTextRecursive(child, text)
            if (result != null) return result
        }
        return null
    }

    private fun findNodeById(id: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val fullId = if (id.contains(":id/")) id else "${rootInActiveWindow?.packageName}:id/$id"
        return rootNode.findAccessibilityNodeInfosByText(id).firstOrNull()
            ?: findNodeByIdRecursive(rootNode, fullId)
    }

    private fun findNodeByIdRecursive(
        node: AccessibilityNodeInfo,
        id: String
    ): AccessibilityNodeInfo? {
        if (node.viewIdResourceName == id) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByIdRecursive(child, id)
            if (result != null) return result
        }
        return null
    }

    /**
     * 点击节点（优先用节点自带的点击，失败时用坐标手势）
     */
    private suspend fun clickNode(node: AccessibilityNodeInfo): Pair<Boolean, String> {
        // 尝试节点自身的点击
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return Pair(true, "点击节点成功")
        }

        // 尝试父节点点击
        val parent = node.parent
        if (parent != null && parent.isClickable &&
            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return Pair(true, "点击父节点成功")
        }

        // fallback: 坐标手势点击
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() > 0 && rect.height() > 0) {
            return clickByCoordinates(
                rect.centerX().toFloat(),
                rect.centerY().toFloat()
            )
        }

        return Pair(false, "无法点击节点")
    }

    // ==================== 状态查询 ====================

    fun getCurrentScreenInfo(): String {
        val rootNode = rootInActiveWindow ?: return "无法获取当前窗口"
        val sb = StringBuilder()
        sb.appendLine("当前窗口: ${rootInActiveWindow?.packageName}")
        sb.appendLine("节点总数: ${rootNode.childCount}")
        sb.appendLine("---")
        dumpNodeTree(rootNode, sb, 0, 5)
        return sb.toString()
    }

    private fun dumpNodeTree(
        node: AccessibilityNodeInfo,
        sb: StringBuilder,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth) return
        val indent = "  ".repeat(depth)
        val text = node.text?.toString() ?: ""
        val id = node.viewIdResourceName ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val click = if (node.isClickable) "[可点击]" else ""

        sb.appendLine("$indent${node.className} $click " +
                "${if (text.isNotEmpty()) "text=\"$text\"" else ""} " +
                "${if (id.isNotEmpty()) "id=$id" else ""} " +
                "${if (desc.isNotEmpty()) "desc=\"$desc\"" else ""}")

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            dumpNodeTree(child, sb, depth + 1, maxDepth)
        }
    }
}
