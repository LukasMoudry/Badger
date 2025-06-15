package com.example.badger

import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TimePicker
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class AddTaskActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_task)

        val nameEt     = findViewById<EditText>(R.id.nameEt)
        val daySpinner = findViewById<Spinner>(R.id.daySpinner)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        val saveBtn    = findViewById<Button>(R.id.saveBtn)

        val days = listOf(
            "Sunday","Monday","Tuesday","Wednesday",
            "Thursday","Friday","Saturday"
        )
        daySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            days
        )

        saveBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            if (name.isEmpty()) return@setOnClickListener

            val dayIndex = daySpinner.selectedItemPosition + Calendar.SUNDAY
            val hour = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                timePicker.hour else @Suppress("DEPRECATION") timePicker.currentHour
            val minute = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                timePicker.minute else @Suppress("DEPRECATION") timePicker.currentMinute

            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayIndex)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val tasks = PrefsHelper.loadTasks(this)
            val newId = (tasks.maxOfOrNull { it.id } ?: 0) + 1
            val task = Task(newId, name, dayIndex, hour, minute, cal.timeInMillis)
            tasks.add(task)
            PrefsHelper.saveTasks(this, tasks)
            AlarmScheduler.schedule(task, this)
            finish()
        }
    }
}
