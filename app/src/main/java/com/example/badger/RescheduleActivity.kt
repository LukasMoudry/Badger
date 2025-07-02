package com.example.badger

import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.TimePicker
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

class RescheduleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reschedule)

        // 0) Dismiss the original notification
        val taskId = intent.getIntExtra("taskId", -1)
        if (taskId >= 0) {
            NotificationManagerCompat.from(this).cancel(taskId)
        }

        // 1) Find views
        val taskNameTv  = findViewById<TextView>(R.id.taskNameTv)
        val daySpinner  = findViewById<Spinner>(R.id.daySpinner)
        val timePicker  = findViewById<TimePicker>(R.id.timePicker)
        val saveBtn     = findViewById<Button>(R.id.saveBtn)

        // 2) Load task
        val tasks  = PrefsHelper.loadTasks(this)
        val task   = tasks.find { it.id == taskId } ?: run {
            finish()
            return
        }

        // 3) Show its name
        taskNameTv.text = task.name

        // 4) Spinner with Monday first (same as AddTaskActivity)
        val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
        daySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            days
        )

        // 5) Pre-select the current day index
        //    Convert Calendar.MONDAY=2…SUNDAY=1 into 0…6
        val idx = (task.dayOfWeek + 5) % 7
        daySpinner.setSelection(idx)

        // 6) 24-hour mode TimePicker
        timePicker.setIs24HourView(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.hour   = task.hour
            timePicker.minute = task.minute
        } else {
            @Suppress("DEPRECATION")
            timePicker.currentHour   = task.hour
            @Suppress("DEPRECATION")
            timePicker.currentMinute = task.minute
        }

        // 7) Save button: update nextAskEpoch just like AddTaskActivity
        saveBtn.setOnClickListener {
            // Map spinner position back to Calendar day
            val customDay   = daySpinner.selectedItemPosition + 1
            val calendarDay = if (customDay == 7)
                Calendar.SUNDAY
            else
                Calendar.MONDAY + (customDay - 1)

            // Read time
            val (h, m) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                timePicker.hour to timePicker.minute
            } else {
                @Suppress("DEPRECATION")
                timePicker.currentHour to
                        @Suppress("DEPRECATION")
                        timePicker.currentMinute
            }

            // Compute nextAskEpoch
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, calendarDay)
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            task.nextAskEpoch = cal.timeInMillis
            task.dayOfWeek    = calendarDay
            task.hour         = h
            task.minute       = m

            // Save & schedule
            PrefsHelper.saveTasks(this, tasks)
            AlarmScheduler.schedule(task, this)
            finish()
        }
    }
}
