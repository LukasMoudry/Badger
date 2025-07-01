package com.example.badger

import android.Manifest
import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1) Edge-to-edge padding on the CoordinatorLayout
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
                // EDIT existing
                startActivity(Intent(this, AddTaskActivity::class.java).apply {
                    putExtra(AddTaskActivity.EXTRA_TASK_ID, task.id)
                })
            },
            onItemAction = { task, action ->
                if (action == TaskAdapter.Action.DELETE) {
                    // DELETE handling
                    AlarmScheduler.cancel(task, this)
                    val remaining = PrefsHelper.loadTasks(this)
                        .filterNot { it.id == task.id }
                    PrefsHelper.saveTasks(this, remaining)
                    adapter.submitList(remaining)
                }
            }
        )
        rv.adapter = adapter

        // 3) FAB for adding new
        findViewById<FloatingActionButton>(R.id.addBtn).setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        // 4) Ask POST_NOTIFICATIONS permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 5) Load & schedule
        reloadListAndSchedule()
    }

    private fun reloadListAndSchedule() {
        val list = PrefsHelper.loadTasks(this)
        adapter.submitList(list)
        list.forEach { AlarmScheduler.schedule(it, this) }
    }

    override fun onResume() {
        super.onResume()
        reloadListAndSchedule()
    }
}
