package com.autoclicker.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.autoclicker.app.adapter.ActionAdapter
import com.autoclicker.app.databinding.ActivityConfigEditBinding
import com.autoclicker.app.model.Action
import com.autoclicker.app.model.ActionType
import com.autoclicker.app.model.AutoConfig
import com.autoclicker.app.storage.ConfigStorage

class ConfigEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfigEditBinding
    private lateinit var storage: ConfigStorage
    private var config: AutoConfig? = null
    private lateinit var actionAdapter: ActionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "编辑配置"
        storage = ConfigStorage.getInstance(this)
        val configId = intent.getStringExtra("config_id") ?: return
        config = storage.getConfig(configId)
        config?.let { loadConfig(it) }
        binding.fabAddAction.setOnClickListener { showAddActionDialog() }
        binding.btnSave.setOnClickListener { saveConfig() }
    }

    private fun loadConfig(config: AutoConfig) {
        binding.etName.setText(config.name)
        binding.etDesc.setText(config.description)
        binding.etLoops.setText(config.loopCount.toString())
        binding.etLoopDelay.setText(config.loopDelayMs.toString())
        actionAdapter = ActionAdapter(
            actions = config.actions,
            onDelete = { pos ->
                config.actions.removeAt(pos)
                actionAdapter.notifyDataSetChanged()
                updateEmptyState()
            },
            onMoveUp = { pos ->
                if (pos > 0) {
                    val item = config.actions.removeAt(pos)
                    config.actions.add(pos - 1, item)
                    actionAdapter.notifyItemMoved(pos, pos - 1)
                }
            },
            onMoveDown = { pos ->
                if (pos < config.actions.size - 1) {
                    val item = config.actions.removeAt(pos)
                    config.actions.add(pos + 1, item)
                    actionAdapter.notifyItemMoved(pos, pos + 1)
                }
            },
            onToggle = { pos, enabled ->
                config.actions[pos] = config.actions[pos].copy(enabled = enabled)
            }
        )
        binding.recyclerViewActions.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewActions.adapter = actionAdapter
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (config?.actions.isNullOrEmpty()) {
            binding.emptyActionsHint.visibility = View.VISIBLE
            binding.recyclerViewActions.visibility = View.GONE
        } else {
            binding.emptyActionsHint.visibility = View.GONE
            binding.recyclerViewActions.visibility = View.VISIBLE
        }
    }

    private fun saveConfig() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入配置名称", Toast.LENGTH_SHORT).show()
            return
        }
        config?.let { current ->
            val updated = current.copy(
                name = name,
                description = binding.etDesc.text.toString().trim(),
                loopCount = binding.etLoops.text.toString().toIntOrNull() ?: 1,
                loopDelayMs = binding.etLoopDelay.text.toString().toLongOrNull() ?: 1000L,
                actions = current.actions,
                updatedAt = System.currentTimeMillis()
            )
            storage.saveConfig(updated)
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showAddActionDialog() {
        val types = arrayOf(
            "启动应用", "点击文字", "点击资源ID", "点击坐标",
            "输入文字", "向指定ID输入", "等待文字出现", "等待ID出现",
            "等待文字消失", "向下滑动", "向上滑动", "滑动操作",
            "返回", "回到桌面", "注释"
        )
        val typeValues = arrayOf(
            ActionType.LAUNCH_APP, ActionType.CLICK_TEXT, ActionType.CLICK_ID,
            ActionType.CLICK坐标, ActionType.INPUT_TEXT, ActionType.INPUT_ID,
            ActionType.WAIT_TEXT, ActionType.WAIT_ID, ActionType.WAIT_GONE,
            ActionType.SCROLL_DOWN, ActionType.SCROLL_UP, ActionType.SWIPE,
            ActionType.BACK, ActionType.HOME, ActionType.COMMENT
        )
        AlertDialog.Builder(this)
            .setTitle("选择动作类型")
            .setItems(types) { _, which -> showActionDetailDialog(typeValues[which]) }
            .show()
    }

    private fun showActionDetailDialog(type: ActionType) {
        val dv = layoutInflater.inflate(R.layout.dialog_action_detail, null)
        val etT = dv.findViewById<android.widget.EditText>(R.id.et_target)
        val etV = dv.findViewById<android.widget.EditText>(R.id.et_value)
        val etD = dv.findViewById<android.widget.EditText>(R.id.et_delay)
        val etO = dv.findViewById<android.widget.EditText>(R.id.et_timeout)
        etD.setText("500"); etO.setText("10000")
        when (type) {
            ActionType.CLICK_TEXT -> { etT.hint = "要点击的文字"; etV.visibility = View.GONE }
            ActionType.CLICK_ID -> { etT.hint = "资源ID"; etV.visibility = View.GONE }
            ActionType.CLICK坐标 -> { etT.hint = "x坐标"; etV.hint = "y坐标" }
            ActionType.INPUT_TEXT -> { etT.hint = "无需填写"; etV.hint = "要输入的文字" }
            ActionType.INPUT_ID -> { etT.hint = "输入框ID"; etV.hint = "要输入的文字" }
            ActionType.LAUNCH_APP -> { etT.hint = "包名 (如 com.tencent.mm)"; etV.visibility = View.GONE }
            ActionType.WAIT_TEXT -> { etT.hint = "等待出现的文字"; etV.visibility = View.GONE }
            ActionType.WAIT_ID -> { etT.hint = "等待出现的ID"; etV.visibility = View.GONE }
            ActionType.WAIT_GONE -> { etT.hint = "等待消失的文字"; etV.visibility = View.GONE }
            ActionType.SWIPE -> { etT.hint = "起点x,y"; etV.hint = "终点x,y" }
            ActionType.COMMENT -> { etT.hint = "注释内容"; etV.visibility = View.GONE }
            else -> { etT.hint = "目标"; etV.visibility = View.GONE }
        }
        AlertDialog.Builder(this)
            .setTitle("添加动作")
            .setView(dv)
            .setPositiveButton("添加") { _, _ ->
                val a = Action(
                    type = type, target = etT.text.toString().trim(),
                    value = etV.text.toString().trim(),
                    delayMs = etD.text.toString().toLongOrNull() ?: 500L,
                    timeoutMs = etO.text.toString().toLongOrNull() ?: 10000L
                )
                config?.actions?.add(a)
                actionAdapter.notifyItemInserted((config?.actions?.size ?: 1) - 1)
                binding.recyclerViewActions.scrollToPosition((config?.actions?.size ?: 1) - 1)
                updateEmptyState()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}