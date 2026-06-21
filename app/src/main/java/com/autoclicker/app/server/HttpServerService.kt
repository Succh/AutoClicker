package com.autoclicker.app.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.autoclicker.app.MainActivity
import com.autoclicker.app.R
import com.autoclicker.app.engine.AutomationEngine
import com.autoclicker.app.model.*
import com.autoclicker.app.service.AccessibilityClickService
import com.autoclicker.app.storage.ConfigStorage
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * HTTP 服务 - 让 AI 可以通过 HTTP 接口操控自动化
 *
 * AI 调用示例：
 *   curl -X POST http://localhost:8765/api/run -d '{"name":"钉钉打卡"}'
 *   curl http://localhost:8765/api/configs
 *   curl http://localhost:8765/api/status
 */
class HttpServerService : Service() {

    companion object {
        private const val TAG = "HttpServer"
        private const val CHANNEL_ID = "autoclicker_server"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var isRunning = false
            private set

        var port = 8765
            private set

        fun start(context: Context, port: Int = 8765) {
            val intent = Intent(context, HttpServerService::class.java).apply {
                putExtra("port", port)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HttpServerService::class.java))
        }
    }

    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val threadPool = Executors.newCachedThreadPool()
    private val gson = Gson()

    private lateinit var storage: ConfigStorage

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        storage = ConfigStorage.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        port = intent?.getIntExtra("port", storage.getHttpPort()) ?: storage.getHttpPort()

        startForeground(NOTIFICATION_ID, buildNotification())
        startServer()

        return START_STICKY
    }

    override fun onDestroy() {
        stopServer()
        scope.cancel()
        threadPool.shutdownNow()
        super.onDestroy()
    }

    // ==================== Server 启动/停止 ====================

    private fun startServer() {
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                Log.i(TAG, "HTTP 服务启动，端口: $port")

                while (isActive) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        threadPool.submit { handleRequest(client) }
                    } catch (e: Exception) {
                        if (isActive) Log.e(TAG, "接受连接失败: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "服务启动失败: ${e.message}")
                isRunning = false
            }
        }
    }

    private fun stopServer() {
        isRunning = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    // ==================== HTTP 请求处理 ====================

    private fun handleRequest(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)

            // 读取请求行
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 3) return

            val method = parts[0]
            val path = parts[1]

            // 读取 Headers
            val headers = mutableMapOf<String, String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                val colonIndex = line!!.indexOf(':')
                if (colonIndex > 0) {
                    headers[line!!.substring(0, colonIndex).trim().lowercase()] =
                        line!!.substring(colonIndex + 1).trim()
                }
            }

            // 读取 Body (POST/PUT)
            val body = if (method == "POST" || method == "PUT") {
                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                if (contentLength > 0) {
                    val buffer = CharArray(contentLength)
                    reader.read(buffer, 0, contentLength)
                    String(buffer)
                } else ""
            } else ""

            // 路由
            val response = route(method, path, body)

            // 发送响应
            val json = gson.toJson(response)
            writer.println("HTTP/1.1 ${if (response.ok) 200 else 400} OK")
            writer.println("Content-Type: application/json; charset=utf-8")
            writer.println("Access-Control-Allow-Origin: *")
            writer.println("Content-Length: ${json.toByteArray().size}")
            writer.println()
            writer.println(json)

        } catch (e: Exception) {
            Log.e(TAG, "处理请求失败: ${e.message}")
        } finally {
            socket.close()
        }
    }

    // ==================== API 路由 ====================

    private fun route(method: String, path: String, body: String): ApiResponse<Any> {
        return try {
            when {
                // ========== 健康检查 ==========
                path == "/api/health" || path == "/" -> {
                    ApiResponse(true, mapOf(
                        "status" to "running",
                        "accessibility" to (AccessibilityClickService.isRunning),
                        "version" to "1.0.0"
                    ))
                }

                // ========== 配置管理 ==========
                path == "/api/configs" && method == "GET" -> {
                    ApiResponse(true, storage.getAllConfigs())
                }

                path == "/api/configs" && method == "POST" -> {
                    val config = gson.fromJson(body, AutoConfig::class.java)
                    storage.saveConfig(config)
                    ApiResponse(true, config, null)
                }

                path.startsWith("/api/configs/") && method == "GET" -> {
                    val id = path.removePrefix("/api/configs/")
                    val config = storage.getConfig(id)
                    if (config != null) ApiResponse(true, config)
                    else ApiResponse(false, null, "配置不存在")
                }

                path.startsWith("/api/configs/") && method == "DELETE" -> {
                    val id = path.removePrefix("/api/configs/")
                    if (storage.deleteConfig(id)) ApiResponse(true, "已删除")
                    else ApiResponse(false, null, "配置不存在")
                }

                // ========== 运行控制 ==========
                path == "/api/run" && method == "POST" -> {
                    val req = gson.fromJson(body, RunRequest::class.java)
                    val config = when {
                        !req.configId.isNullOrBlank() -> storage.getConfig(req.configId)
                        !req.name.isNullOrBlank() -> storage.getConfigByName(req.name)
                        !req.json.isNullOrBlank() -> {
                            // 直接传入 JSON 配置并运行
                            val config = gson.fromJson(req.json, AutoConfig::class.java)
                            storage.saveConfig(config)
                            config
                        }
                        else -> null
                    }

                    if (config == null) {
                        ApiResponse(false, null, "配置不存在（请提供 configId / name / json）")
                    } else {
                        val started = AutomationEngine.instance?.runConfig(config) ?: false
                        if (started) {
                            ApiResponse(true, mapOf(
                                "configId" to config.id,
                                "configName" to config.name,
                                "message" to "已启动"
                            ))
                        } else {
                            ApiResponse(false, null, "启动失败（无障碍服务是否开启？）")
                        }
                    }
                }

                path == "/api/stop" && method == "POST" -> {
                    val req = gson.fromJson(body, RunRequest::class.java)
                    if (!req.configId.isNullOrBlank()) {
                        AutomationEngine.instance?.stopConfig(req.configId)
                        ApiResponse(true, "已停止")
                    } else if (!req.name.isNullOrBlank()) {
                        val config = storage.getConfigByName(req.name)
                        if (config != null) {
                            AutomationEngine.instance?.stopConfig(config.id)
                            ApiResponse(true, "已停止: ${config.name}")
                        } else {
                            ApiResponse(false, null, "配置不存在")
                        }
                    } else {
                        AutomationEngine.instance?.stopAll()
                        ApiResponse(true, "已停止全部")
                    }
                }

                path == "/api/stop-all" && method == "POST" -> {
                    AutomationEngine.instance?.stopAll()
                    ApiResponse(true, "已停止全部")
                }

                // ========== 状态查询 ==========
                path == "/api/status" && method == "GET" -> {
                    val allStates = AutomationEngine.instance?.getAllRunningStates() ?: emptyList()
                    ApiResponse(true, mapOf(
                        "accessibility" to AccessibilityClickService.isRunning,
                        "httpServer" to isRunning,
                        "port" to port,
                        "runningCount" to allStates.size,
                        "running" to allStates
                    ))
                }

                path.startsWith("/api/status/") && method == "GET" -> {
                    val configId = path.removePrefix("/api/status/")
                    val state = AutomationEngine.instance?.getRunState(configId)
                    if (state != null) ApiResponse(true, state)
                    else ApiResponse(false, null, "无运行状态")
                }

                // ========== 日志 ==========
                path.startsWith("/api/log/") && method == "GET" -> {
                    val configId = path.removePrefix("/api/log/")
                    val log = AutomationEngine.instance?.getRunLog(configId) ?: emptyList()
                    ApiResponse(true, log)
                }

                // ========== 一键操作（AI快捷入口） ==========

                // 快速点击: POST /api/click {"text":"确定"}
                path == "/api/click" && method == "POST" -> {
                    val req = gson.fromJson(body, ClickRequest::class.java)
                    val service = AccessibilityClickService.instance
                    if (service == null) {
                        ApiResponse(false, null, "无障碍服务未开启")
                    } else {
                        scope.launch {
                            val action = Action(
                                type = ActionType.CLICK_TEXT,
                                target = req.text ?: "",
                                coordinates = req.x?.let { Point(it, req.y ?: 0f) }
                            )
                            val (ok, msg) = service.executeAction(action)
                            Log.i(TAG, "快捷点击: ok=$ok, msg=$msg")
                        }
                        ApiResponse(true, "点击指令已发送")
                    }
                }

                // 快速输入: POST /api/input {"text":"hello"}
                path == "/api/input" && method == "POST" -> {
                    val req = gson.fromJson(body, InputRequest::class.java)
                    val service = AccessibilityClickService.instance
                    if (service == null) {
                        ApiResponse(false, null, "无障碍服务未开启")
                    } else {
                        scope.launch {
                            val action = Action(
                                type = ActionType.INPUT_TEXT,
                                target = req.text ?: ""
                            )
                            service.executeAction(action)
                        }
                        ApiResponse(true, "输入指令已发送")
                    }
                }

                // 快速启动App: POST /api/launch {"package":"com.xxx"}
                path == "/api/launch" && method == "POST" -> {
                    val req = gson.fromJson(body, LaunchRequest::class.java)
                    val service = AccessibilityClickService.instance
                    if (service == null) {
                        ApiResponse(false, null, "无障碍服务未开启")
                    } else {
                        val (ok, msg) = service.executeAction(
                            Action(type = ActionType.LAUNCH_APP, target = req.pkg ?: "")
                        )
                        ApiResponse(ok, msg)
                    }
                }

                // 查看当前屏幕: GET /api/screen
                path == "/api/screen" && method == "GET" -> {
                    val service = AccessibilityClickService.instance
                    if (service == null) {
                        ApiResponse(false, null, "无障碍服务未开启")
                    } else {
                        ApiResponse(true, service.getCurrentScreenInfo())
                    }
                }

                // ========== 导入配置 ==========
                path == "/api/import" && method == "POST" -> {
                    val config = storage.importConfig(body)
                    if (config != null) {
                        ApiResponse(true, config)
                    } else {
                        ApiResponse(false, null, "JSON 解析失败")
                    }
                }

                // ========== 端口修改 ==========
                path == "/api/port" && method == "POST" -> {
                    val req = gson.fromJson(body, PortRequest::class.java)
                    val newPort = req.port ?: return ApiResponse(false, null, "缺少 port")
                    storage.setHttpPort(newPort)
                    ApiResponse(true, mapOf(
                        "message" to "端口已保存，重启服务后生效",
                        "port" to newPort
                    ))
                }

                // 404
                else -> ApiResponse(false, null, "未知接口: $method $path")
            }
        } catch (e: Exception) {
            ApiResponse(false, null, "错误: ${e.message}")
        }
    }

    // ==================== 通知 ====================

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "自动化服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "HTTP 服务运行中"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoClicker HTTP 服务")
            .setContentText("端口: $port | 无障碍: ${AccessibilityClickService.isRunning}")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    // ==================== 请求数据类 ====================

    data class RunRequest(
        val configId: String? = null,
        val name: String? = null,
        val json: String? = null
    )

    data class ClickRequest(
        val text: String? = null,
        val x: Float? = null,
        val y: Float? = null
    )

    data class InputRequest(
        val text: String? = null
    )

    data class LaunchRequest(
        val pkg: String? = null
    )

    data class PortRequest(
        val port: Int? = null
    )
}
