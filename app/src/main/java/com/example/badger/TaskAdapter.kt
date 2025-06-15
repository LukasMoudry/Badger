package com.example.badger

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onAction: (Task, Action) -> Unit
) : ListAdapter<Task, TaskAdapter.VH>(DIFF) {

    enum class Action { DELETE }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    inner class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val nameTv = itemView.findViewById<TextView>(R.id.nameTv)
        private val delBtn = itemView.findViewById<ImageButton>(R.id.delBtn)

        fun bind(task: Task) {
            val dayName = SimpleDateFormat("EEEE", Locale.getDefault())
                .format(Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, task.dayOfWeek)
                }.time)
            val time = String.format("%02d:%02d", task.hour, task.minute)
            nameTv.text = "${task.name} — $dayName @ $time"
            delBtn.setOnClickListener { onAction(task, Action.DELETE) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(a: Task, b: Task) = a.id == b.id
            override fun areContentsTheSame(a: Task, b: Task) = a == b
        }
    }
}
