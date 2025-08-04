package com.example.badger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class TaskAdapter(
    private val onItemClick: (Task) -> Unit,
    private val onItemAction: (Task, Action) -> Unit
) : ListAdapter<Task, TaskAdapter.VH>(TaskDiffCallback()) {

    enum class Action { DELETE, EDIT }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val nameTv: TextView = view.findViewById(R.id.nameTv)
        val timeTv: TextView = view.findViewById(R.id.timeTv)
        private val editBtn: ImageButton = view.findViewById(R.id.editBtn)
        private val delBtn: ImageButton = view.findViewById(R.id.deleteBtn)

        init {
            // full‐row click still edits
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
            // edit button
            editBtn.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemAction(getItem(pos), Action.EDIT)
                }
            }
            // delete button
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

        // short day name
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

        val base = String.format("%s %02d:%02d", dayName, task.hour, task.minute)
        holder.timeTv.text = task.repeatIntervalMinutes?.let { interval ->
            val text = when {
                interval % (60 * 24) == 0 -> "every ${interval / (60 * 24)}d"
                interval % 60 == 0 -> "every ${interval / 60}h"
                else -> "every ${interval}m"
            }
            "$base ($text)"
        } ?: base
    }
}
