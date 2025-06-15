package com.example.badger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "badger_channel"
        private const val PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra("taskId", -1)
        if (id < 0) return

        val task = PrefsHelper.loadTasks(ctx)
            .find { it.id == id } ?: return

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Badger reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val yesIntent = Intent(ctx, ActionReceiver::class.java)
            .putExtra("taskId", id)
            .putExtra("done", true)
        // Launch RescheduleActivity on “Not yet”
        val noIntent = Intent(ctx, RescheduleActivity::class.java)
            .putExtra("taskId", id)

        nm.notify(
            id, NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Did you do “${task.name}”?")
                .addAction(
                    0, "Yes",
                    PendingIntent.getBroadcast(ctx, id * 10 + 1, yesIntent, PI_FLAGS)
                )
                .addAction(
                    0, "Not yet",
                    PendingIntent.getActivity(ctx, id * 10 + 2, noIntent, PI_FLAGS)
                )
                .setAutoCancel(true)
                .build()
        )
    }
}
