package com.example.badger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class TaskAdapter(
    private val onItemClick: (Task) -> Unit,
    private val onItemAction: (Task, Action) -> Unit
) : ListAdapter<Task, TaskAdapter.VH>(TaskDiffCallback()) {

    enum class Action { DELETE }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val nameTv: TextView = view.findViewById(R.id.nameTv)
        val timeTv: TextView = view.findViewById(R.id.timeTv)
        private val delBtn: Button = view.findViewById(R.id.deleteBtn)

        init {
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
            delBtn.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemAction(getItem(pos), Action.DELETE)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val task = getItem(position)
        holder.nameTv.text = task.name

        // 12-hour conversion + English labels
        val hour12 = when (val h = task.hour % 12) {
            0    -> 12
            else -> h
        }
        val minute = task.minute
        val amPm   = if (task.hour < 12) "morning" else "afternoon"

        // Day-of-week short name
        val dayName = when (task.dayOfWeek) {
            Calendar.MONDAY    -> "Mon"
            Calendar.TUESDAY   -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY  -> "Thu"
            Calendar.FRIDAY    -> "Fri"
            Calendar.SATURDAY  -> "Sat"
            Calendar.SUNDAY    -> "Sun"
            else               -> ""
        }

        holder.timeTv.text = String.format("%s %02d:%02d %s",
            dayName, hour12, minute, amPm)
    }
}
