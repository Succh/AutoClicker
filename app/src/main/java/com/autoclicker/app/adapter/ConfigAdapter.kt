package com.autoclicker.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.R
import com.autoclicker.app.engine.AutomationEngine
import com.autoclicker.app.model.AutoConfig

class ConfigAdapter(
    private val configs: MutableList<AutoConfig>,
    private val onRun: (AutoConfig) -> Unit,
    private val onStop: (AutoConfig) -> Unit,
    private val onDelete: (AutoConfig) -> Unit,
    private val onExport: (AutoConfig) -> Unit,
    private val onEdit: (AutoConfig) -> Unit
) : RecyclerView.Adapter<ConfigAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_config_name)
        val tvDesc: TextView = view.findViewById(R.id.tv_config_desc)
        val tvActionCount: TextView = view.findViewById(R.id.tv_action_count)
        val tvLoopInfo: TextView = view.findViewById(R.id.tv_loop_info)
        val tvStatus: TextView = view.findViewById(R.id.tv_run_status)
        val btnRunStop: ImageButton = view.findViewById(R.id.btn_run_stop)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
        val btnExport: ImageButton = view.findViewById(R.id.btn_export)
        val btnEdit: ImageButton = view.findViewById(R.id.btn_edit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_config, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val config = configs[position]
        val engine = AutomationEngine.instance
        val isRunning = engine?.isRunning(config.id) == true

        holder.tvName.text = config.name
        holder.tvDesc.text = config.description.ifEmpty { "无描述" }
        holder.tvActionCount.text = "${config.actions.size} 个动作"
        holder.tvLoopInfo.text = if (config.loopCount <= 0) "无限循环" else "循环 ${config.loopCount} 次"

        if (isRunning) {
            val state = engine?.getRunState(config.id)
            holder.tvStatus.text = "🔄 运行中 (${state?.currentLoop ?: 0}/${state?.totalLoops ?: 0})"
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.running))
        } else {
            holder.tvStatus.text = if (config.enabled) "⏸ 就绪" else "🔕 已禁用"
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.ready))
        }

        holder.btnRunStop.setOnClickListener {
            if (isRunning) onStop(config) else onRun(config)
        }
        holder.btnDelete.setOnClickListener { onDelete(config) }
        holder.btnExport.setOnClickListener { onExport(config) }
        holder.btnEdit.setOnClickListener { onEdit(config) }
    }

    override fun getItemCount() = configs.size

    fun updateData(newConfigs: List<AutoConfig>) {
        configs.clear()
        configs.addAll(newConfigs)
        notifyDataSetChanged()
    }
}
