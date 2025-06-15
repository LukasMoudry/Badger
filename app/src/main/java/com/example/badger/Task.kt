package com.example.badger

data class Task(
    val id: Int,
    var name: String,
    var dayOfWeek: Int,     // Calendar.SUNDAY=1 … Calendar.SATURDAY=7
    var hour: Int,
    var minute: Int,
    var nextAskEpoch: Long  // ms since epoch
)
