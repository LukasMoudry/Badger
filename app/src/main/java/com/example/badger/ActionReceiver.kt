// === badger/ActionReceiver.kt ===
package com.example.badger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

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

        // 3) When tapping "Not yet" open the reschedule screen
        if (id < 0) return

        val tasks = PrefsHelper.loadTasks(ctx)
        val task  = tasks.find { it.id == id } ?: return

        if (!done) {
            NotificationManagerCompat.from(ctx).cancel(id)
            if (task.snoozeMinutes != null) {
                val delayMs = task.snoozeMinutes!!.toLong() * 60_000
                task.nextAskEpoch = System.currentTimeMillis() + delayMs
                val cal = Calendar.getInstance().apply { timeInMillis = task.nextAskEpoch }
                task.dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                task.hour = cal.get(Calendar.HOUR_OF_DAY)
                task.minute = cal.get(Calendar.MINUTE)
                PrefsHelper.saveTasks(ctx, tasks)
                AlarmScheduler.schedule(task, ctx)
                ctx.sendBroadcast(Intent(ACTION_TASK_DELETED))
            } else {
                ctx.startActivity(
                    Intent(ctx, RescheduleActivity::class.java).apply {
                        putExtra("taskId", id)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            return
        }

        // 4) Cancel the original notification
        NotificationManagerCompat.from(ctx).cancel(id)

        if (task.repeatIntervalDays != null) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = task.nextAskEpoch
                add(Calendar.DAY_OF_YEAR, task.repeatIntervalDays!!)
            }
            task.nextAskEpoch = cal.timeInMillis
            task.dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            task.hour = cal.get(Calendar.HOUR_OF_DAY)
            task.minute = cal.get(Calendar.MINUTE)
            PrefsHelper.saveTasks(ctx, tasks)
            AlarmScheduler.schedule(task, ctx)
            ctx.sendBroadcast(Intent(ACTION_TASK_DELETED))
        } else {
            AlarmScheduler.cancel(task, ctx)
            val updated = tasks.filterNot { it.id == id }
            PrefsHelper.saveTasks(ctx, updated)
            ctx.sendBroadcast(Intent(ACTION_TASK_DELETED))
        }

        // 7) Show a “Good job!” notification with sprinkle emojis
        //    on Android O+ we need to (re)create the channel
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    "badger_channel",
                    "Badger reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        NotificationManagerCompat.from(ctx).notify(
            /* notificationId */ id + 1000,
            NotificationCompat.Builder(ctx, "badger_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Good job!")
                .setContentText("✨🌟✨ ✨🌟✨")
                .setAutoCancel(true)
                .build()
        )
    }
}
