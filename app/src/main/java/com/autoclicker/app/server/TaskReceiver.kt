package com.autoclicker.app.server

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.autoclicker.app.engine.AutomationEngine
import com.autoclicker.app.storage.ConfigStorage

class TaskReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RUN = "com.autoclicker.app.TASK_RUN"
        const val ACTION_STOP = "com.autoclicker.app.TASK_STOP"
        const val ACTION_STOP_ALL = "com.autoclicker.app.TASK_STOP_ALL"
        const val ACTION_STATUS = "com.autoclicker.app.TASK_STATUS"
        const val ACTION_LIST = "com.autoclicker.app.TASK_LIST"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val storage = ConfigStorage.getInstance(context)
        val engine = AutomationEngine(storage)

        when (intent.action) {
            ACTION_RUN -> {
                val configId = intent.getStringExtra("config_id")
                if (configId == null) {
                    sendResult(context, intent, false, "缺少 config_id 参数")
                    return
                }
                val success = engine.runConfig(configId)
                sendResult(context, intent, success,
                    if (success) "任务已启动: $configId" else "启动失败: 配置不存在或无障碍未开启")
            }

            ACTION_STOP -> {
                val configId = intent.getStringExtra("config_id")
                val success = if (configId != null) {
                    engine.stopConfig(configId)
                } else {
                    engine.stopAll()
                    true
                }
                sendResult(context, intent, success,
                    if (success) "任务已停止" else "停止失败")
            }

            ACTION_STOP_ALL -> {
                engine.stopAll()
                sendResult(context, intent, true, "所有任务已停止")
            }

            ACTION_STATUS -> {
                val configId = intent.getStringExtra("config_id")
                if (configId != null) {
                    val state = engine.getRunState(configId)
                    val result = if (state != null) {
                        "运行中: loop=${state.currentLoop}/${state.totalLoops}, " +
                        "action=${state.currentAction}/${state.totalActions}"
                    } else {
                        "未运行"
                    }
                    sendResult(context, intent, true, result)
                } else {
                    val all = engine.getAllRunningStates()
                    val result = if (all.isEmpty()) {
                        "没有任务在运行"
                    } else {
                        all.joinToString("\n") { state ->
                            "${state.configId}: loop=${state.currentLoop}/${state.totalLoops}"
                        }
                    }
                    sendResult(context, intent, true, result)
                }
            }

            ACTION_LIST -> {
                val configs = storage.getAllConfigs()
                val result = if (configs.isEmpty()) {
                    "没有配置"
                } else {
                    configs.joinToString("\n") { config ->
                        "${config.id}: ${config.name} (${config.actions.size}个动作, 循环${config.loopCount}次)"
                    }
                }
                sendResult(context, intent, true, result)
            }
        }
    }

    private fun sendResult(context: Context, intent: Intent, success: Boolean, message: String) {
        val resultIntent = Intent("com.autoclicker.app.TASK_RESULT").apply {
            putExtra("success", success)
            putExtra("message", message)
            putExtra("action", intent.action)
            putExtra("config_id", intent.getStringExtra("config_id"))
            setPackage(context.packageName)
        }
        context.sendBroadcast(resultIntent)
    }
}