package com.example.badger

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AlarmScheduler {
    private const val FLAG = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun schedule(task: Task, ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, AlarmReceiver::class.java)
            .putExtra("taskId", task.id)
        val pi = PendingIntent.getBroadcast(ctx, task.id, intent, FLAG)
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            task.nextAskEpoch,
            pi
        )
    }

    fun cancel(task: Task, ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(ctx, task.id, intent, FLAG)
        am.cancel(pi)
        pi.cancel()
    }
}
