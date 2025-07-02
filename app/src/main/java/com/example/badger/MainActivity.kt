package com.example.badger

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: TaskAdapter

    // Permission launcher for POST_NOTIFICATIONS on Android 13+
    private val askNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op if denied */ }

    // Receiver to catch “task deleted” broadcasts and refresh UI
    private val onTaskDeleted = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reloadListAndSchedule()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1) Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinator)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // 2) RecyclerView + adapter
        val rv = findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        adapter = TaskAdapter(
            onItemClick = { task ->
                // edit via row tap
                startActivity(Intent(this, AddTaskActivity::class.java).apply {
                    putExtra(AddTaskActivity.EXTRA_TASK_ID, task.id)
                })
            },
            onItemAction = { task, action ->
                when (action) {
                    TaskAdapter.Action.DELETE -> {
                        // DELETE
                        AlarmScheduler.cancel(task, this)
                        val remaining = PrefsHelper.loadTasks(this)
                            .filterNot { it.id == task.id }
                        PrefsHelper.saveTasks(this, remaining)
                        adapter.submitList(remaining)
                    }
                    TaskAdapter.Action.EDIT -> {
                        // EDIT via edit button
                        startActivity(Intent(this, AddTaskActivity::class.java).apply {
                            putExtra(AddTaskActivity.EXTRA_TASK_ID, task.id)
                        })
                    }
                }
            }
        )
        rv.adapter = adapter

        // 3) FAB for adding new
        findViewById<FloatingActionButton>(R.id.addBtn).setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        // 4) Ask POST_NOTIFICATIONS if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 5) Load, schedule, show
        reloadListAndSchedule()

        // 6) Deletion broadcast
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                onTaskDeleted,
                IntentFilter(ActionReceiver.ACTION_TASK_DELETED),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                onTaskDeleted,
                IntentFilter(ActionReceiver.ACTION_TASK_DELETED)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onTaskDeleted)
    }

    override fun onResume() {
        super.onResume()
        reloadListAndSchedule()
    }

    private fun reloadListAndSchedule() {
        val list = PrefsHelper.loadTasks(this)
        adapter.submitList(list)
        list.forEach { AlarmScheduler.schedule(it, this) }
    }
}
