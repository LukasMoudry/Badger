// === badger/AlarmReceiver.kt ===
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
import androidx.core.app.NotificationCompat.BubbleMetadata
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.app.Person

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "badger_channel"
        private const val TAG = "AlarmReceiver"
        private const val SHORTCUT_ID = "badger_bubble"
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
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Badger reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                setAllowBubbles(true)
            }
            nm.createNotificationChannel(channel)
        }

        // 4) Build the bubble Intent (opens RescheduleActivity)
        val bubbleIntent = PendingIntent.getActivity(
            ctx,
            id,
            Intent(ctx, RescheduleActivity::class.java).apply {
                putExtra("taskId", id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // 5) Build the BubbleMetadata
        val bubbleData = BubbleMetadata.Builder()
            .setIntent(bubbleIntent)
            .setIcon(IconCompat.createWithResource(ctx, android.R.drawable.ic_dialog_alert))
            .setDesiredHeight(600)
            .build()

        // 6) Build & post the bubble notification
        val person = Person.Builder()
            .setName("Badger")
            .build()

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Did you do “${task.name}”?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShortcutId(SHORTCUT_ID)         // must match your shortcut XML
            // When the bubble disappears, tapping the notification should reopen
            // the reschedule screen. Setting a content intent keeps the
            // notification interactive on older Android versions.
            .setContentIntent(bubbleIntent)
            .addPerson(person)                  // conversation style
            .setBubbleMetadata(bubbleData)      // enable bubble overlay
            .setAutoCancel(true)
            // fallback actions in the shade
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
            .addAction(0, "Not yet", bubbleIntent)
            .build()

        NotificationManagerCompat.from(ctx).notify(id, notif)
    }
}
