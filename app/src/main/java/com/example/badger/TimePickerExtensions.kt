package com.example.badger

import android.os.Build
import android.widget.TimePicker

/**
 * Extension functions for TimePicker to handle API version compatibility
 */

fun TimePicker.getHourCompat(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        hour
    } else {
        @Suppress("DEPRECATION")
        currentHour
    }
}

fun TimePicker.getMinuteCompat(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        minute
    } else {
        @Suppress("DEPRECATION")
        currentMinute
    }
}

fun TimePicker.setTimeCompat(hourOfDay: Int, minuteOfHour: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        hour = hourOfDay
        minute = minuteOfHour
    } else {
        @Suppress("DEPRECATION")
        currentHour = hourOfDay
        @Suppress("DEPRECATION")
        currentMinute = minuteOfHour
    }
}
