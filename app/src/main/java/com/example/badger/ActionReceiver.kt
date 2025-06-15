package com.example.badger

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.*

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id   = intent.getIntExtra("taskId", -1)
        val done = intent.getBooleanExtra("done", false)
        if (id < 0 || !done) return  // only handle “Yes”

        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(id)

        val tasks = PrefsHelper.loadTasks(ctx)
        val task  = tasks.find { it.id == id } ?: return

        // schedule next week
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, task.dayOfWeek)
            set(Calendar.HOUR_OF_DAY, task.hour)
            set(Calendar.MINUTE, task.minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        task.nextAskEpoch = cal.timeInMillis

        PrefsHelper.saveTasks(ctx, tasks)
        AlarmScheduler.schedule(task, ctx)
    }
}
