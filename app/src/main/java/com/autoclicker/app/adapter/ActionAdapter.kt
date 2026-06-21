package com.autoclicker.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.R
import com.autoclicker.app.model.Action

class ActionAdapter(
    private val actions: MutableList<Action>,
    private val onDelete: (Int) -> Unit,
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit,
    private val onToggle: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<ActionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIndex: TextView = view.findViewById(R.id.tv_action_index)
        val tvType: TextView = view.findViewById(R.id.tv_action_type)
        val tvTarget: TextView = view.findViewById(R.id.tv_action_target)
        val tvDesc: TextView = view.findViewById(R.id.tv_action_desc)
        val switchEnabled: Switch = view.findViewById(R.id.switch_enabled)
        val btnUp: ImageButton = view.findViewById(R.id.btn_move_up)
        val btnDown: ImageButton = view.findViewById(R.id.btn_move_down)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete_action)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_action, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val action = actions[position]
        val index = holder.adapterPosition

        holder.tvIndex.text = "#${index + 1}"
        holder.tvType.text = action.type.name
        holder.tvTarget.text = action.target.ifEmpty { "-" }
        holder.tvDesc.text = action.description.ifEmpty { "无说明" }
        holder.switchEnabled.isChecked = action.enabled

        holder.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            onToggle(index, isChecked)
        }
        holder.btnUp.setOnClickListener { onMoveUp(index) }
        holder.btnDown.setOnClickListener { onMoveDown(index) }
        holder.btnDelete.setOnClickListener { onDelete(index) }
    }

    override fun getItemCount() = actions.size
}
