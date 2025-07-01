package com.example.badger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat

class ActionReceiver : BroadcastReceiver() {
    companion object {
        // custom action for in‐app updates
        const val ACTION_TASK_DELETED = "com.example.badger.ACTION_TASK_DELETED"
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        // 1) Pull extras
        val id   = intent.getIntExtra("taskId", -1)
        val done = intent.getBooleanExtra("done", false)

        // 2) Log to confirm we got here
        Log.d("ActionReceiver", "✓ onReceive: id=$id done=$done")

        // 3) Only handle the “Yes” tap
        if (id < 0 || !done) return

        // 4) Cancel the notification
        NotificationManagerCompat.from(ctx).cancel(id)

        // 5) Load, cancel alarm, remove from prefs
        val tasks = PrefsHelper.loadTasks(ctx)
        val task  = tasks.find { it.id == id } ?: return

        AlarmScheduler.cancel(task, ctx)
        val updated = tasks.filterNot { it.id == id }
        PrefsHelper.saveTasks(ctx, updated)

        // 6) Notify any in‐app listeners so they can refresh
        ctx.sendBroadcast(Intent(ACTION_TASK_DELETED))
    }
}
