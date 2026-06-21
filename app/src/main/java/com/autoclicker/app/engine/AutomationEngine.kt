package com.autoclicker.app.engine

import android.util.Log
import com.autoclicker.app.model.*
import com.autoclicker.app.service.AccessibilityClickService
import com.autoclicker.app.storage.ConfigStorage
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 自动化执行引擎
 * 负责解析配置、调度动作、管理运行状态
 */
class AutomationEngine(private val storage: ConfigStorage) {

    companion object {
        private const val TAG = "AutoEngine"

        @Volatile
        var instance: AutomationEngine? = null
            private set
    }

    // configId -> Job
    private val runningJobs = ConcurrentHashMap<String, Job>()

    // configId -> RunState
    private val runStates = ConcurrentHashMap<String, RunState>()

    // configId -> 执行日志
    private val runLogs = ConcurrentHashMap<String, MutableList<ActionResult>>()

    // 最大日志条数
    private val maxLogSize = 200

    init {
        instance = this
    }

    // ==================== 运行控制 ====================

    /**
     * 运行一个配置
     */
    fun runConfig(configId: String): Boolean {
        val config = storage.getConfig(configId) ?: run {
            Log.e(TAG, "配置不存在: $configId")
            return false
        }
        return runConfig(config)
    }

    fun runConfig(config: AutoConfig): Boolean {
        // 如果已经在运行，先停止
        if (runningJobs.containsKey(config.id)) {
            Log.w(TAG, "配置 ${config.name} 已在运行，先停止")
            stopConfig(config.id)
        }

        // 检查无障碍服务
        if (AccessibilityClickService.instance == null) {
            Log.e(TAG, "无障碍服务未开启！")
            return false
        }

        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val job = scope.launch {
            executeConfig(config)
        }
        runningJobs[config.id] = job
        runStates[config.id] = RunState(
            configId = config.id,
            configName = config.name,
            isRunning = true,
            totalLoops = config.loopCount,
            totalActions = config.actions.size,
            startTime = System.currentTimeMillis()
        )
        runLogs[config.id] = mutableListOf()

        Log.i(TAG, "开始运行: ${config.name} (循环${config.loopCount}次)")
        return true
    }

    /**
     * 停止一个配置
     */
    fun stopConfig(configId: String): Boolean {
        val job = runningJobs.remove(configId)
        job?.cancel()

        val state = runStates[configId]
        if (state != null) {
            runStates[configId] = state.copy(isRunning = false)
        }

        Log.i(TAG, "停止运行: $configId")
        return job != null
    }

    /**
     * 停止所有
     */
    fun stopAll() {
        runningJobs.keys.toList().forEach { stopConfig(it) }
    }

    /**
     * 检查是否在运行
     */
    fun isRunning(configId: String): Boolean {
        return runningJobs[configId]?.isActive == true
    }

    /**
     * 获取运行状态
     */
    fun getRunState(configId: String): RunState? = runStates[configId]

    /**
     * 获取所有运行中的状态
     */
    fun getAllRunningStates(): List<RunState> {
        return runStates.values.filter { it.isRunning }
    }

    /**
     * 获取执行日志
     */
    fun getRunLog(configId: String): List<ActionResult> {
        return runLogs[configId]?.toList() ?: emptyList()
    }

    // ==================== 核心执行 ====================

    private suspend fun executeConfig(config: AutoConfig) {
        val log = runLogs.getOrPut(config.id) { mutableListOf() }

        try {
            val loops = if (config.loopCount <= 0) Int.MAX_VALUE else config.loopCount

            for (loop in 1..loops) {
                // 检查是否被取消
                yield()

                updateState(config.id) { it.copy(currentLoop = loop) }

                Log.i(TAG, "=== ${config.name} 第 $loop/$config.loopCount 轮 ===")

                val actions = config.actions.filter { it.enabled }
                for ((index, action) in actions.withIndex()) {
                    yield() // 检查取消点

                    updateState(config.id) { it.copy(currentAction = index + 1) }

                    // 动作前延迟
                    if (action.delayMs > 0) {
                        delay(action.delayMs)
                    }

                    // 执行动作
                    val service = AccessibilityClickService.instance
                    if (service == null) {
                        val result = ActionResult(
                            success = false,
                            actionIndex = index,
                            actionType = action.type,
                            message = "无障碍服务不可用"
                        )
                        log.add(result)
                        updateState(config.id) { it.copy(lastError = "无障碍服务不可用") }
                        // 无障碍服务丢失，停止整个任务
                        break
                    }

                    val (success, message) = service.executeAction(action)

                    val result = ActionResult(
                        success = success,
                        actionIndex = index,
                        actionType = action.type,
                        message = message
                    )
                    log.add(result)

                    // 限制日志大小
                    if (log.size > maxLogSize) {
                        val excess = log.size - maxLogSize
                        repeat(excess) { log.removeAt(0) }
                    }

                    if (!success) {
                        Log.w(TAG, "动作失败: ${action.type} - $message")
                        updateState(config.id) { it.copy(lastError = message) }

                        // 如果是等待类超时失败，继续下一个动作
                        if (action.type in listOf(ActionType.WAIT_TEXT, ActionType.WAIT_ID, ActionType.WAIT_GONE)) {
                            continue
                        }
                    }

                    Log.d(TAG, "动作[${index}] ${action.type}: $message")
                }

                // 轮次间延迟
                if (loop < loops && config.loopDelayMs > 0) {
                    delay(config.loopDelayMs)
                }
            }

            Log.i(TAG, "=== ${config.name} 执行完毕 ===")
        } catch (e: CancellationException) {
            Log.i(TAG, "=== ${config.name} 被取消 ===")
        } catch (e: Exception) {
            Log.e(TAG, "执行异常: ${e.message}", e)
            updateState(config.id) { it.copy(lastError = "异常: ${e.message}") }
        } finally {
            updateState(config.id) { it.copy(isRunning = false) }
            runningJobs.remove(config.id)
        }
    }

    private fun updateState(configId: String, transform: (RunState) -> RunState) {
        val current = runStates[configId] ?: return
        runStates[configId] = transform(current)
    }
}
