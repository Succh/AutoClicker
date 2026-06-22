package com.autoclicker.app.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.autoclicker.app.ConfigEditActivity
import com.autoclicker.app.R
import com.autoclicker.app.adapter.ConfigAdapter
import com.autoclicker.app.databinding.FragmentConfigListBinding
import com.autoclicker.app.engine.AutomationEngine
import com.autoclicker.app.model.Action
import com.autoclicker.app.model.ActionType
import com.autoclicker.app.model.AutoConfig
import com.autoclicker.app.storage.ConfigStorage
import com.google.gson.Gson

class ConfigListFragment : Fragment() {

    private var _binding: FragmentConfigListBinding? = null
    private val binding get() = _binding!!
    private lateinit var storage: ConfigStorage
    private lateinit var adapter: ConfigAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storage = ConfigStorage.getInstance(requireContext())

        adapter = ConfigAdapter(
            configs = storage.getAllConfigs().toMutableList(),
            onRun = { config -> runConfig(config) },
            onStop = { config -> stopConfig(config) },
            onDelete = { config -> deleteConfig(config) },
            onExport = { config -> exportConfig(config) },
            onEdit = { config -> editConfig(config) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.fab.setOnClickListener { showCreateDialog() }
        binding.btnImport.setOnClickListener { showImportDialog() }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        adapter.updateData(storage.getAllConfigs())
        binding.emptyHint.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun runConfig(config: AutoConfig) {
        val engine = AutomationEngine.instance
        if (engine == null) {
            Toast.makeText(requireContext(), "引擎未就绪", Toast.LENGTH_SHORT).show()
            return
        }
        if (engine.isRunning(config.id)) {
            engine.stopConfig(config.id)
            Toast.makeText(requireContext(), "已停止: ${config.name}", Toast.LENGTH_SHORT).show()
        } else {
            val started = engine.runConfig(config)
            Toast.makeText(
                requireContext(),
                if (started) "已启动: ${config.name}" else "启动失败（请检查无障碍服务）",
                Toast.LENGTH_SHORT
            ).show()
        }
        refreshList()
    }

    private fun stopConfig(config: AutoConfig) {
        AutomationEngine.instance?.stopConfig(config.id)
        refreshList()
    }

    private fun deleteConfig(config: AutoConfig) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除配置")
            .setMessage("确定删除「${config.name}」？")
            .setPositiveButton("删除") { _, _ ->
                storage.deleteConfig(config.id)
                refreshList()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun exportConfig(config: AutoConfig) {
        val json = Gson().toJson(config)
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("auto_config", json))
        Toast.makeText(requireContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun editConfig(config: AutoConfig) {
        val intent = Intent(requireContext(), ConfigEditActivity::class.java)
        intent.putExtra("config_id", config.id)
        startActivity(intent)
    }

    private fun showCreateDialog() {
        val items = arrayOf("从零新建", "快速模板: 钉钉打卡", "快速模板: 微信签到", "快速模板: 定时打开App")
        AlertDialog.Builder(requireContext())
            .setTitle("新建配置")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> createBlankConfig()
                    1 -> createDingTalkTemplate()
                    2 -> createWechatTemplate()
                    3 -> createLaunchTemplate()
                }
            }
            .show()
    }

    private fun showImportDialog() {
        val input = EditText(requireContext()).apply {
            hint = "粘贴 JSON 配置..."
            minLines = 5
        }
        AlertDialog.Builder(requireContext())
            .setTitle("导入配置")
            .setView(input)
            .setPositiveButton("导入") { _, _ ->
                val json = input.text.toString()
                val config = storage.importConfig(json)
                if (config != null) {
                    Toast.makeText(requireContext(), "导入成功: ${config.name}", Toast.LENGTH_SHORT).show()
                    refreshList()
                } else {
                    Toast.makeText(requireContext(), "JSON 解析失败", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createBlankConfig() {
        val config = AutoConfig(
            name = "新配置 ${System.currentTimeMillis() % 10000}",
            actions = mutableListOf()
        )
        storage.saveConfig(config)
        refreshList()
    }

    private fun createDingTalkTemplate() {
        val config = AutoConfig(
            name = "钉钉打卡",
            description = "自动打开钉钉并执行打卡操作",
            loopCount = 1,
            actions = mutableListOf(
                Action(type = ActionType.LAUNCH_APP, target = "com.alibaba.android.rimet", delayMs = 1000, description = "打开钉钉"),
                Action(type = ActionType.WAIT_TEXT, target = "工作台", timeoutMs = 15000, description = "等待首页加载"),
                Action(type = ActionType.CLICK_TEXT, target = "工作台", delayMs = 1000, description = "点击工作台"),
                Action(type = ActionType.WAIT_TEXT, target = "打卡", timeoutMs = 10000, description = "等待打卡入口出现"),
                Action(type = ActionType.CLICK_TEXT, target = "打卡", delayMs = 2000, description = "点击打卡"),
                Action(type = ActionType.WAIT_TEXT, target = "上班打卡", timeoutMs = 10000, description = "等待打卡页面"),
                Action(type = ActionType.CLICK_TEXT, target = "上班打卡", delayMs = 1000, description = "点击上班打卡"),
                Action(type = ActionType.COMMENT, target = "注意: 钉钉打卡可能有定位/WiFi限制", description = "提醒")
            )
        )
        storage.saveConfig(config)
        refreshList()
    }

    private fun createWechatTemplate() {
        val config = AutoConfig(
            name = "微信签到",
            description = "自动打开微信执行签到",
            loopCount = 1,
            actions = mutableListOf(
                Action(type = ActionType.LAUNCH_APP, target = "com.tencent.mm", delayMs = 2000, description = "打开微信"),
                Action(type = ActionType.CLICK_TEXT, target = "我", delayMs = 1000, description = "点击我"),
                Action(type = ActionType.CLICK_TEXT, target = "服务", delayMs = 1000, description = "点击服务"),
            )
        )
        storage.saveConfig(config)
        refreshList()
    }

    private fun createLaunchTemplate() {
        val config = AutoConfig(
            name = "定时打开App",
            description = "简单模板：启动指定App",
            loopCount = 1,
            actions = mutableListOf(
                Action(type = ActionType.LAUNCH_APP, target = "输入包名", description = "启动应用（请修改包名）")
            )
        )
        storage.saveConfig(config)
        refreshList()
    }
}
