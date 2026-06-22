package com.autoclicker.app

import android.os.Bundle
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

        val configId = intent.getStringExtra("config_id")
        config = configId?.let { storage.getConfig(it) }

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
            actions = config.actions.toMutableList(),
            onDelete = { index ->
                config.actions.removeAt(index)
                actionAdapter.notifyDataSetChanged()
            },
            onMoveUp = { index ->
                if (index > 0) {
                    val temp = config.actions[index]
                    config.actions[index] = config.actions[index - 1]
                    config.actions[index - 1] = temp
                    actionAdapter.notifyDataSetChanged()
                }
            },
            onMoveDown = { index ->
                if (index < config.actions.size - 1) {
                    val temp = config.actions[index]
                    config.actions[index] = config.actions[index + 1]
                    config.actions[index + 1] = temp
                    actionAdapter.notifyDataSetChanged()
                }
            },
            onToggle = { index, enabled ->
                config.actions[index] = config.actions[index].copy(enabled = enabled)
            }
        )

        binding.recyclerViewActions.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewActions.adapter = actionAdapter
    }

    private fun saveConfig() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入名称", Toast.LENGTH_SHORT).show()
            return
        }

        val current = config ?: return
        val updated = current.copy(
            name = name,
            description = binding.etDesc.text.toString().trim(),
            loopCount = binding.etLoops.text.toString().toIntOrNull() ?: 1,
            loopDelayMs = binding.etLoopDelay.text.toString().toLongOrNull() ?: 1000
        )

        storage.saveConfig(updated)
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showAddActionDialog() {
        val types = ActionType.values()
        val typeNames = types.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("添加动作")
            .setItems(typeNames) { _, which ->
                val type = types[which]
                showActionDetailDialog(type)
            }
            .show()
    }

    private fun showActionDetailDialog(type: ActionType) {
        val config = config ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_action_detail, null)
        val etTarget = dialogView.findViewById<android.widget.EditText>(R.id.et_target)
        val etValue = dialogView.findViewById<android.widget.EditText>(R.id.et_value)
        val etDelay = dialogView.findViewById<android.widget.EditText>(R.id.et_delay)
        val etTimeout = dialogView.findViewById<android.widget.EditText>(R.id.et_timeout)
        val etDesc = dialogView.findViewById<android.widget.EditText>(R.id.et_action_desc)

        etDelay.setText("500")
        etTimeout.setText("10000")

        when (type) {
            ActionType.CLICK_TEXT -> etTarget.hint = "要点击的文字"
            ActionType.CLICK_ID -> etTarget.hint = "资源ID (如 com.xxx:id/btn)"
            ActionType.CLICK坐标 -> etTarget.hint = "x坐标"
            ActionType.INPUT_TEXT -> etTarget.hint = "要输入的文字"
            ActionType.INPUT_ID -> etTarget.hint = "输入框ID"
            ActionType.LAUNCH_APP -> etTarget.hint = "包名 (如 com.tencent.mm)"
            ActionType.WAIT_TEXT -> etTarget.hint = "要等待出现的文字"
            ActionType.WAIT_ID -> etTarget.hint = "要等待出现的ID"
            ActionType.WAIT_GONE -> etTarget.hint = "要等待消失的文字"
            ActionType.SWIPE -> etTarget.hint = "起始x,y"
            ActionType.COMMENT -> etTarget.hint = "注释内容"
            else -> etTarget.hint = "参数"
        }

        AlertDialog.Builder(this)
            .setTitle("配置: $type")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val action = Action(
                    type = type,
                    target = etTarget.text.toString(),
                    value = etValue.text.toString(),
                    delayMs = etDelay.text.toString().toLongOrNull() ?: 500,
                    timeoutMs = etTimeout.text.toString().toLongOrNull() ?: 10000,
                    description = etDesc.text.toString()
                )
                config.actions.add(action)
                actionAdapter.notifyDataSetChanged()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
