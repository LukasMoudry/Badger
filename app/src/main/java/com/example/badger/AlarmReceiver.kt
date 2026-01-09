package com.example.badger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
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
        private const val YES_BUTTON_BASE = 10000
        private const val NOT_YET_BUTTON_BASE = 20000
        private const val BUBBLE_INTENT_BASE = 30000
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        // 1) Pull the "taskId" extra
        val id = intent.getIntExtra("taskId", -1)
        if (id < 0) return

        Log.d(TAG, "onReceive fired for taskId=$id")

        // 2) Load the task by ID
        val task = PrefsHelper.loadTasks(ctx).find { it.id == id } ?: return

        // 3) Create NotificationChannel (Android O+)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Badger reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                setAllowBubbles(true) // allow bubbles below TIRAMISU
            }
            nm.createNotificationChannel(channel)
        }

        // 4) Prepare "Yes" and "Not yet" intents with unique request codes
        val yesIntent = PendingIntent.getBroadcast(
            ctx,
            YES_BUTTON_BASE + id,
            Intent(ctx, ActionReceiver::class.java).apply {
                putExtra("taskId", id)
                putExtra("done", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notYetIntent = if (task.snoozeMinutes != null || task.repeatIntervalMinutes != null) {
            PendingIntent.getBroadcast(
                ctx,
                NOT_YET_BUTTON_BASE + id,
                Intent(ctx, ActionReceiver::class.java).apply {
                    putExtra("taskId", id)
                    putExtra("done", false)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                ctx,
                NOT_YET_BUTTON_BASE + id,
                Intent(ctx, RescheduleActivity::class.java).apply {
                    putExtra("taskId", id)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // 5) Build bubble intent & metadata
        val bubbleIntent = PendingIntent.getActivity(
            ctx,
            BUBBLE_INTENT_BASE + id,
            Intent(ctx, RescheduleActivity::class.java).apply {
                putExtra("taskId", id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val bubbleData = BubbleMetadata.Builder()
            .setIntent(bubbleIntent)
            .setIcon(IconCompat.createWithResource(ctx, android.R.drawable.ic_dialog_alert))
            .setDesiredHeight(600)
            .build()

        // 6) Inflate & wire custom layout for both collapsed and heads‑up
        val customView = RemoteViews(ctx.packageName, R.layout.notification_heads_up).apply {
            setTextViewText(R.id.title, "Did you do \u201C${task.name}\u201D?")
            setOnClickPendingIntent(R.id.btn_yes, yesIntent)
            setOnClickPendingIntent(R.id.btn_not_yet, notYetIntent)
        }

        // 7) Build the notification
        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customView)
            .setCustomHeadsUpContentView(customView)
            .setContentIntent(bubbleIntent)
            .addPerson(Person.Builder().setName("Badger").build())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    setBubbleMetadata(bubbleData)
                }
            }

        // 8) Post notification
        NotificationManagerCompat.from(ctx).notify(id, builder.build())
    }
}
