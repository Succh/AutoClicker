package com.autoclicker.app.model

import com.google.gson.annotations.SerializedName

/**
 * 自动化配置 - 类似 MacroDroid 的宏
 */
data class AutoConfig(
    @SerializedName("id") val id: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("actions") val actions: MutableList<Action> = mutableListOf(),
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("loopCount") val loopCount: Int = 1,       // 0 = 无限循环
    @SerializedName("loopDelayMs") val loopDelayMs: Long = 1000,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 单个动作 - 每一步操作
 */
data class Action(
    @SerializedName("type") val type: ActionType,
    @SerializedName("target") val target: String = "",          // 文字/资源ID/packageName
    @SerializedName("coordinates") val coordinates: Point? = null,
    @SerializedName("value") val value: String = "",            // 输入文字等
    @SerializedName("delayMs") val delayMs: Long = 500,         // 动作前等待
    @SerializedName("timeoutMs") val timeoutMs: Long = 10000,   // 等待超时
    @SerializedName("description") val description: String = "", // 动作说明
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("retryCount") val retryCount: Int = 3,      // 失败重试次数
    @SerializedName("packageName") val packageName: String = "", // 目标App包名
    @SerializedName("extra") val extra: String = ""             // 额外参数
)

enum class ActionType {
    CLICK_TEXT,             // 按文字点击
    CLICK_ID,               // 按资源ID点击
    CLICK坐标,              // 按坐标点击
    CLICK_NODE,             // 按节点索引点击（第N个匹配节点）

    LONG_CLICK_TEXT,        // 长按文字
    LONG_CLICK_ID,          // 长按资源ID

    INPUT_TEXT,             // 输入文字（到当前焦点）
    INPUT_ID,               // 向指定ID的输入框输入文字

    SCROLL_DOWN,            // 向下滚动
    SCROLL_UP,              // 向上滚动
    SCROLL_TO_TEXT,         // 滚动直到找到文字

    WAIT_TEXT,              // 等待文字出现
    WAIT_ID,                // 等待ID出现
    WAIT_GONE,              // 等待文字消失

    LAUNCH_APP,             // 启动App
    BACK,                   // 返回
    HOME,                   // 主页
    RECENT,                 // 最近任务

    SWIPE,                  // 滑动
    PINCH,                  // 缩放

    SCREENSHOT,             // 截图
    SCREENSHOT_AND_ANALYZE, // 截图并分析（返回给AI）

    SPEAK,                  // 语音播报

    CONDITION,              // 条件判断（If-Else）
    LOOP,                   // 循环子动作
    BREAK,                  // 跳出循环

    COMMENT,                // 注释（跳过）
}

data class Point(
    @SerializedName("x") val x: Float,
    @SerializedName("y") val y: Float
)

/**
 * 执行结果
 */
data class ActionResult(
    @SerializedName("success") val success: Boolean,
    @SerializedName("actionIndex") val actionIndex: Int,
    @SerializedName("actionType") val actionType: ActionType,
    @SerializedName("message") val message: String = "",
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

/**
 * 宏运行状态
 */
data class RunState(
    @SerializedName("configId") val configId: String,
    @SerializedName("configName") val configName: String,
    @SerializedName("isRunning") val isRunning: Boolean,
    @SerializedName("currentLoop") val currentLoop: Int = 0,
    @SerializedName("totalLoops") val totalLoops: Int = 0,
    @SerializedName("currentAction") val currentAction: Int = 0,
    @SerializedName("totalActions") val totalActions: Int = 0,
    @SerializedName("startTime") val startTime: Long = 0,
    @SerializedName("lastError") val lastError: String = ""
)

/**
 * API 统一响应
 */
data class ApiResponse<T>(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error") val error: String? = null
)
