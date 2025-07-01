package com.example.badger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "badger_channel"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        // 1) Pull the "taskId" extra
        val id = intent.getIntExtra("taskId", -1)
        if (id < 0) return

        Log.d(TAG, "onReceive fired for taskId=$id")

        // 2) Load the task by ID
        val task = PrefsHelper.loadTasks(ctx).find { it.id == id } ?: return

        // 3) Create NotificationChannel on O+
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Badger reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        // 4) Build and show the notification with actions
        NotificationManagerCompat.from(ctx)
            .notify(
                id,
                NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("Did you do “${task.name}”?")
                    .addAction(
                        0, "Yes",
                        PendingIntent.getBroadcast(
                            ctx,
                            id * 10 + 1,
                            Intent(ctx, ActionReceiver::class.java)
                                .putExtra("taskId", id)
                                .putExtra("done", true),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .addAction(
                        0, "Not yet",
                        PendingIntent.getActivity(
                            ctx,
                            id * 10 + 2,
                            Intent(ctx, RescheduleActivity::class.java)
                                .putExtra("taskId", id),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .setAutoCancel(true)
                    .build()
            )
    }
}
