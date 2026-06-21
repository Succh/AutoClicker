package com.autoclicker.app.model

import com.google.gson.annotations.SerializedName

data class AutoConfig(
    @SerializedName("id") val id: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("actions") val actions: MutableList<Action> = mutableListOf(),
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("loopCount") val loopCount: Int = 1,
    @SerializedName("loopDelayMs") val loopDelayMs: Long = 1000,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt") val updatedAt: Long = System.currentTimeMillis()
)

data class Action(
    @SerializedName("type") val type: ActionType,
    @SerializedName("target") val target: String = "",
    @SerializedName("coordinates") val coordinates: Point? = null,
    @SerializedName("value") val value: String = "",
    @SerializedName("delayMs") val delayMs: Long = 500,
    @SerializedName("timeoutMs") val timeoutMs: Long = 10000,
    @SerializedName("description") val description: String = "",
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("retryCount") val retryCount: Int = 3,
    @SerializedName("packageName") val packageName: String = "",
    @SerializedName("extra") val extra: String = ""
)

enum class ActionType {
    CLICK_TEXT, CLICK_ID, CLICK坐标, CLICK_NODE,
    LONG_CLICK_TEXT, LONG_CLICK_ID, INPUT_TEXT, INPUT_ID,
    SCROLL_DOWN, SCROLL_UP, SCROLL_TO_TEXT,
    WAIT_TEXT, WAIT_ID, WAIT_GONE,
    LAUNCH_APP, BACK, HOME, RECENT, SWIPE, PINCH,
    SCREENSHOT, SCREENSHOT_AND_ANALYZE, SPEAK,
    CONDITION, LOOP, BREAK, COMMENT
}

data class Point(@SerializedName("x") val x: Float, @SerializedName("y") val y: Float)

data class ActionResult(@SerializedName("success") val success: Boolean, @SerializedName("actionIndex") val actionIndex: Int, @SerializedName("actionType") val actionType: ActionType, @SerializedName("message") val message: String = "", @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis())

data class RunState(@SerializedName("configId") val configId: String, @SerializedName("configName") val configName: String, @SerializedName("isRunning") val isRunning: Boolean, @SerializedName("currentLoop") val currentLoop: Int = 0, @SerializedName("totalLoops") val totalLoops: Int = 0, @SerializedName("currentAction") val currentAction: Int = 0, @SerializedName("totalActions") val totalActions: Int = 0, @SerializedName("startTime") val startTime: Long = 0, @SerializedName("lastError") val lastError: String = "")

data class ApiResponse<T>(@SerializedName("ok") val ok: Boolean, @SerializedName("data") val data: T? = null, @SerializedName("error") val error: String? = null)