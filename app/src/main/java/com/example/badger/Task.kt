package com.example.badger

data class Task(
    val id: Int,            // unique ID
    var name: String,       // user‐entered name
    var dayOfWeek: Int,     // Calendar.MONDAY=2 … Calendar.SUNDAY=1
    var hour: Int,          // 0–23
    var minute: Int,        // 0–59
    var nextAskEpoch: Long,  // ms since epoch of next alarm
    var repeatIntervalDays: Int? = null, // repeat every N days, null = no repeat
    var repeatIntervalMinutes: Int? = null, // repeat every N minutes, null = no repeat
    var snoozeMinutes: Int? = null       // remind again after N minutes if not done
)
