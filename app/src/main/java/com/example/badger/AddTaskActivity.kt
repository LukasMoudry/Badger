package com.example.badger

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class AddTaskActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }

    // Renamed to match your XML ID "nameEt"
    private lateinit var nameEt: EditText
    private lateinit var daySpinner: Spinner
    private lateinit var timePicker: TimePicker
    private lateinit var saveBtn: Button
    private lateinit var repeatSwitch: Switch
    private lateinit var repeatContainer: LinearLayout
    private lateinit var repeatIntervalEt: EditText
    private lateinit var snoozeEt: EditText
    private lateinit var startDateBtn: Button
    private var startDate: Calendar? = null

    private val tasks = mutableListOf<Task>()
    private var editingTask: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        // -- View binding: use R.id.nameEt, not nameEdit
        nameEt          = findViewById(R.id.nameEt)
        daySpinner      = findViewById(R.id.daySpinner)
        timePicker      = findViewById(R.id.timePicker)
        saveBtn         = findViewById(R.id.saveBtn)
        repeatSwitch    = findViewById(R.id.repeatSwitch)
        repeatContainer = findViewById(R.id.repeatContainer)
        repeatIntervalEt = findViewById(R.id.repeatIntervalEt)
        snoozeEt        = findViewById(R.id.snoozeEt)
        startDateBtn    = findViewById(R.id.startDateBtn)

        repeatSwitch.setOnCheckedChangeListener { _, isChecked ->
            repeatContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        startDateBtn.setOnClickListener {
            val cal = startDate ?: Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    startDate = Calendar.getInstance().apply { set(y, m, d) }
                    startDateBtn.text = String.format("%d/%d/%d", d, m + 1, y)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Spinner with Monday first
        val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
        daySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, days)

        // 24-hour spinner mode
        timePicker.setIs24HourView(true)

        // Load existing
        tasks.addAll(PrefsHelper.loadTasks(this))

        // Check for edit
        val tid = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (tid >= 0) editingTask = tasks.find { it.id == tid }

        // Pre-fill fields
        val now = Calendar.getInstance()
        val todayIdx = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7  // MON→0…SUN→6

        if (editingTask == null) {
            // New: default to now
            daySpinner.setSelection(todayIdx)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                timePicker.hour   = now.get(Calendar.HOUR_OF_DAY)
                timePicker.minute = now.get(Calendar.MINUTE)
            } else {
                @Suppress("DEPRECATION")
                timePicker.currentHour   = now.get(Calendar.HOUR_OF_DAY)
                @Suppress("DEPRECATION")
                timePicker.currentMinute = now.get(Calendar.MINUTE)
            }
        } else {
            // Editing: fill from task
            editingTask!!.let { t ->
                nameEt.setText(t.name)
                val idx = (t.dayOfWeek + 5) % 7
                daySpinner.setSelection(idx)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    timePicker.hour   = t.hour
                    timePicker.minute = t.minute
                } else {
                    @Suppress("DEPRECATION")
                    timePicker.currentHour   = t.hour
                    @Suppress("DEPRECATION")
                    timePicker.currentMinute = t.minute
                }
                if (t.repeatIntervalDays != null) {
                    repeatSwitch.isChecked = true
                    repeatContainer.visibility = View.VISIBLE
                    repeatIntervalEt.setText(t.repeatIntervalDays.toString())
                    t.snoozeMinutes?.let { snoozeEt.setText((it / (60 * 24)).toString()) }
                    startDate = Calendar.getInstance().apply { timeInMillis = t.nextAskEpoch }
                    startDate?.let { sd ->
                        startDateBtn.text = String.format(
                            "%d/%d/%d",
                            sd.get(Calendar.DAY_OF_MONTH),
                            sd.get(Calendar.MONTH) + 1,
                            sd.get(Calendar.YEAR)
                        )
                    }
                }
            }
        }

        saveBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            if (name.isEmpty()) {
                nameEt.error = "Enter a name"
                return@setOnClickListener
            }

            // Spinner to Calendar day
            val customDay = daySpinner.selectedItemPosition + 1
            var calendarDay = if (customDay == 7) Calendar.SUNDAY
            else Calendar.MONDAY + (customDay - 1)

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
                if (repeatSwitch.isChecked && startDate != null) {
                    timeInMillis = startDate!!.timeInMillis
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                } else {
                    set(Calendar.DAY_OF_WEEK, calendarDay)
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }
            }
            calendarDay = cal.get(Calendar.DAY_OF_WEEK)
            val nextEpoch = cal.timeInMillis

            val repeatDays = if (repeatSwitch.isChecked) {
                repeatIntervalEt.text.toString().toIntOrNull().also {
                    if (it == null) {
                        repeatIntervalEt.error = "Enter repeat days"
                        return@setOnClickListener
                    }
                }
            } else null

            val snoozeDays = if (repeatSwitch.isChecked)
                snoozeEt.text.toString().toIntOrNull() else null
            val snoozeMinutes = snoozeDays?.let { it * 24 * 60 }

            if (editingTask == null) {
                // Create new
                val newId = (tasks.maxOfOrNull { it.id } ?: -1) + 1
                val t = Task(newId, name, calendarDay, h, m, nextEpoch, repeatDays, snoozeMinutes)
                tasks.add(t)
                AlarmScheduler.schedule(t, this)
            } else {
                // Update existing
                editingTask!!.also { t ->
                    AlarmScheduler.cancel(t, this)
                    t.name         = name
                    t.dayOfWeek    = calendarDay
                    t.hour         = h
                    t.minute       = m
                    t.nextAskEpoch = nextEpoch
                    t.repeatIntervalDays = repeatDays
                    t.snoozeMinutes = snoozeMinutes
                    AlarmScheduler.schedule(t, this)
                }
            }

            PrefsHelper.saveTasks(this, tasks)
            finish()
        }
    }
}
