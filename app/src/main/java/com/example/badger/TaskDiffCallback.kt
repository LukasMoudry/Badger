package com.example.badger

import androidx.recyclerview.widget.DiffUtil

class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(a: Task, b: Task) = a.id == b.id
    override fun areContentsTheSame(a: Task, b: Task) = a == b
}
