package com.example.badger

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter

    // permission launcher for Android 13+
    private val askNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1) RecyclerView setup
        recyclerView = findViewById(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 2) Adapter with delete handling
        adapter = TaskAdapter { task, action ->
            if (action == TaskAdapter.Action.DELETE) {
                AlarmScheduler.cancel(task, this)
                val remaining = PrefsHelper.loadTasks(this)
                    .filterNot { it.id == task.id }
                PrefsHelper.saveTasks(this, remaining)
                adapter.submitList(remaining)
            }
        }
        recyclerView.adapter = adapter
        adapter.submitList(PrefsHelper.loadTasks(this))

        // 3) FAB to add new tasks
        findViewById<FloatingActionButton>(R.id.addBtn)
            .setOnClickListener {
                startActivity(Intent(this, AddTaskActivity::class.java))
            }

        // 4) Ask notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 5) Schedule all saved tasks
        PrefsHelper.loadTasks(this).forEach {
            AlarmScheduler.schedule(it, this)
        }
    }

    override fun onResume() {
        super.onResume()
        // refresh the list in case you returned from Add/Reschedule
        adapter.submitList(PrefsHelper.loadTasks(this))
    }
}
