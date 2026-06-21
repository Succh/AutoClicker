package com.autoclicker.app.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.autoclicker.app.R
import com.autoclicker.app.databinding.FragmentServerBinding
import com.autoclicker.app.server.HttpServerService
import com.autoclicker.app.service.AccessibilityClickService
import com.autoclicker.app.storage.ConfigStorage

class ServerFragment : Fragment() {

    private var _binding: FragmentServerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val storage = ConfigStorage.getInstance(requireContext())
        binding.etPort.setText(storage.getHttpPort().toString())

        updateUI()

        // 启动服务
        binding.btnStart.setOnClickListener {
            val port = binding.etPort.text.toString().toIntOrNull() ?: 8765
            storage.setHttpPort(port)
            HttpServerService.start(requireContext(), port)
            Toast.makeText(requireContext(), "HTTP 服务启动中...", Toast.LENGTH_SHORT).show()
            updateUI()
        }

        // 停止服务
        binding.btnStop.setOnClickListener {
            HttpServerService.stop(requireContext())
            updateUI()
        }

        // 检查无障碍
        binding.btnCheckAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // 复制 API 地址
        binding.btnCopyUrl.setOnClickListener {
            val port = storage.getHttpPort()
            val url = "http://localhost:$port/api/"
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            clipboard.setPrimaryClip(
                android.content.ClipData.newPlainText("api_url", url)
            )
            Toast.makeText(requireContext(), "已复制: $url", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val accEnabled = isAccessibilityServiceEnabled(
            requireContext(), AccessibilityClickService::class.java
        )
        val serverRunning = HttpServerService.isRunning
        val port = HttpServerService.port

        binding.tvAccStatus.text = if (accEnabled) "✅ 已开启" else "❌ 未开启"
        binding.tvServerStatus.text = if (serverRunning) "✅ 运行中 (:$port)" else "❌ 未启动"

        binding.btnStart.visibility = if (serverRunning) View.GONE else View.VISIBLE
        binding.btnStop.visibility = if (serverRunning) View.VISIBLE else View.GONE

        // API 示例
        binding.tvApiExamples.text = buildString {
            appendLine("=== API 接口示例 ===")
            appendLine()
            appendLine("健康检查:")
            appendLine("  curl http://localhost:$port/api/health")
            appendLine()
            appendLine("查看所有配置:")
            appendLine("  curl http://localhost:$port/api/configs")
            appendLine()
            appendLine("运行配置(按名称):")
            appendLine("  curl -X POST http://localhost:$port/api/run \\")
            appendLine("    -H 'Content-Type: application/json' \\")
            appendLine("    -d '{\"name\":\"钉钉打卡\"}'")
            appendLine()
            appendLine("运行配置(按ID):")
            appendLine("  curl -X POST http://localhost:$port/api/run \\")
            appendLine("    -H 'Content-Type: application/json' \\")
            appendLine("    -d '{\"configId\":\"xxx\"}'")
            appendLine()
            appendLine("直接传JSON运行:")
            appendLine("  curl -X POST http://localhost:$port/api/run \\")
            appendLine("    -H 'Content-Type: application/json' \\")
            appendLine("    -d '{\"json\":\"{...完整配置...}\"}'")
            appendLine()
            appendLine("停止:")
            appendLine("  curl -X POST http://localhost:$port/api/stop \\")
            appendLine("    -d '{\"name\":\"钉钉打卡\"}'")
            appendLine()
            appendLine("查看状态:")
            appendLine("  curl http://localhost:$port/api/status")
            appendLine()
            appendLine("快捷点击:")
            appendLine("  curl -X POST http://localhost:$port/api/click \\")
            appendLine("    -d '{\"text\":\"确定\"}'")
            appendLine()
            appendLine("查看当前屏幕:")
            appendLine("  curl http://localhost:$port/api/screen")
        }
    }

    private fun isAccessibilityServiceEnabled(
        context: Context,
        serviceClass: Class<AccessibilityClickService>
    ): Boolean {
        val serviceName = ComponentName(context, serviceClass).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(serviceName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
