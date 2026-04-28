package com.example.badger

import java.util.Calendar

object ReminderTiming {
    fun resolveSnoozeMinutes(task: Task, referenceEpoch: Long = task.nextAskEpoch): Int? {
        val day = Calendar.getInstance().apply { timeInMillis = referenceEpoch }.get(Calendar.DAY_OF_WEEK)
        return when (day) {
            Calendar.SATURDAY, Calendar.SUNDAY -> task.weekendSnoozeMinutes ?: task.snoozeMinutes
            else -> task.weekdaySnoozeMinutes ?: task.snoozeMinutes
        }
    }

    fun advanceToNextOccurrence(task: Task, now: Long = System.currentTimeMillis()): Long {
        val intervalMinutes = task.repeatIntervalMinutes ?: return task.scheduledEpoch
        val stepMs = intervalMinutes.toLong() * 60_000
        var next = task.scheduledEpoch + stepMs
        while (next <= now) {
            next += stepMs
        }
        return next
    }
}
