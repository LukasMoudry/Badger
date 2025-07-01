package com.example.badger

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object AlarmScheduler {
    private const val ALARM_REQUEST_CODE = 1000
    private const val TAG = "AlarmScheduler"

    fun schedule(task: Task, ctx: Context) {
        val alarmMgr = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            putExtra("taskId", task.id)  // must match getIntExtra("taskId", ...) in AlarmReceiver
        }
        val flags =
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE else 0

        val pending = PendingIntent.getBroadcast(
            ctx,
            ALARM_REQUEST_CODE + task.id,
            intent,
            flags
        )

        // Try exact + wakeup (requires exact-alarm permission). If denied, fall back to inexact.
        try {
            alarmMgr.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.nextAskEpoch,
                pending
            )
        } catch (sec: SecurityException) {
            Log.w(TAG, "Exact alarm denied; falling back to inexact", sec)
            alarmMgr.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.nextAskEpoch,
                pending
            )
        }
    }

    fun cancel(task: Task, ctx: Context) {
        val alarmMgr = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            putExtra("taskId", task.id)
        }
        val flags =
            PendingIntent.FLAG_NO_CREATE or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE else 0

        val pending = PendingIntent.getBroadcast(
            ctx,
            ALARM_REQUEST_CODE + task.id,
            intent,
            flags
        )
        if (pending != null) {
            alarmMgr.cancel(pending)
            pending.cancel()
        }
    }
}
