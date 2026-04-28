package com.example.badger

import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTaskActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }

    private lateinit var nameEt: EditText
    private lateinit var daySpinner: Spinner
    private lateinit var timePicker: TimePicker
    private lateinit var saveBtn: Button
    private lateinit var repeatSwitch: Switch
    private lateinit var repeatContainer: LinearLayout
    private lateinit var repeatIntervalEt: EditText
    private lateinit var repeatUnitSpinner: Spinner
    private lateinit var snoozeEt: EditText
    private lateinit var snoozeUnitSpinner: Spinner
    private lateinit var weekdaySnoozeEt: EditText
    private lateinit var weekdaySnoozeUnitSpinner: Spinner
    private lateinit var weekendSnoozeEt: EditText
    private lateinit var weekendSnoozeUnitSpinner: Spinner
    private lateinit var startDateBtn: Button
    private var startDate: Calendar? = null

    private val tasks = mutableListOf<Task>()
    private var editingTask: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        nameEt = findViewById(R.id.nameEt)
        daySpinner = findViewById(R.id.daySpinner)
        timePicker = findViewById(R.id.timePicker)
        saveBtn = findViewById(R.id.saveBtn)
        repeatSwitch = findViewById(R.id.repeatSwitch)
        repeatContainer = findViewById(R.id.repeatContainer)
        repeatIntervalEt = findViewById(R.id.repeatIntervalEt)
        repeatUnitSpinner = findViewById(R.id.repeatUnitSpinner)
        snoozeEt = findViewById(R.id.snoozeEt)
        snoozeUnitSpinner = findViewById(R.id.snoozeUnitSpinner)
        weekdaySnoozeEt = findViewById(R.id.weekdaySnoozeEt)
        weekdaySnoozeUnitSpinner = findViewById(R.id.weekdaySnoozeUnitSpinner)
        weekendSnoozeEt = findViewById(R.id.weekendSnoozeEt)
        weekendSnoozeUnitSpinner = findViewById(R.id.weekendSnoozeUnitSpinner)
        startDateBtn = findViewById(R.id.startDateBtn)

        repeatSwitch.setOnCheckedChangeListener { _, isChecked ->
            repeatContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        startDateBtn.setOnClickListener {
            val cal = startDate ?: Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    startDate = Calendar.getInstance().apply { set(y, m, d) }
                    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    startDateBtn.text = dateFormat.format(startDate!!.time)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        daySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, days)

        val unitsAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(R.string.minutes),
                getString(R.string.hours),
                getString(R.string.days)
            )
        )
        repeatUnitSpinner.adapter = unitsAdapter
        snoozeUnitSpinner.adapter = unitsAdapter
        weekdaySnoozeUnitSpinner.adapter = unitsAdapter
        weekendSnoozeUnitSpinner.adapter = unitsAdapter

        timePicker.setIs24HourView(true)

        tasks.addAll(PrefsHelper.loadTasks(this))

        val tid = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (tid >= 0) editingTask = tasks.find { it.id == tid }

        val now = Calendar.getInstance()
        val todayIdx = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7

        if (editingTask == null) {
            daySpinner.setSelection(todayIdx)
            timePicker.setTimeCompat(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        } else {
            editingTask!!.let { t ->
                nameEt.setText(t.name)
                daySpinner.setSelection((t.dayOfWeek + 5) % 7)
                timePicker.setTimeCompat(t.hour, t.minute)
                if (t.repeatIntervalMinutes != null) {
                    repeatSwitch.isChecked = true
                    repeatContainer.visibility = View.VISIBLE
                    fillDurationField(repeatIntervalEt, repeatUnitSpinner, t.repeatIntervalMinutes)
                    fillDurationField(snoozeEt, snoozeUnitSpinner, t.snoozeMinutes)
                    fillDurationField(weekdaySnoozeEt, weekdaySnoozeUnitSpinner, t.weekdaySnoozeMinutes)
                    fillDurationField(weekendSnoozeEt, weekendSnoozeUnitSpinner, t.weekendSnoozeMinutes)
                    startDate = Calendar.getInstance().apply { timeInMillis = t.scheduledEpoch }
                    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    startDateBtn.text = dateFormat.format(startDate!!.time)
                }
            }
        }

        saveBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            if (name.isEmpty()) {
                nameEt.error = "Enter a name"
                return@setOnClickListener
            }

            val customDay = daySpinner.selectedItemPosition + 1
            var calendarDay = if (customDay == 7) Calendar.SUNDAY else Calendar.MONDAY + (customDay - 1)

            val h = timePicker.getHourCompat()
            val m = timePicker.getMinuteCompat()

            val cal = Calendar.getInstance().apply {
                if (repeatSwitch.isChecked && startDate != null) {
                    timeInMillis = startDate!!.timeInMillis
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                } else {
                    set(Calendar.DAY_OF_WEEK, calendarDay)
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }
            }
            calendarDay = cal.get(Calendar.DAY_OF_WEEK)
            val nextEpoch = cal.timeInMillis

            val repeatMinutes = if (repeatSwitch.isChecked) {
                parseDurationMinutes(repeatIntervalEt, repeatUnitSpinner, "Interval", 365 * 24 * 60) ?: return@setOnClickListener
            } else null

            val snoozeMinutes = if (repeatSwitch.isChecked) {
                parseOptionalDurationMinutes(snoozeEt, snoozeUnitSpinner, "Snooze")
            } else null

            val weekdaySnoozeMinutes = if (repeatSwitch.isChecked) {
                parseOptionalDurationMinutes(weekdaySnoozeEt, weekdaySnoozeUnitSpinner, "Weekday snooze")
            } else null

            val weekendSnoozeMinutes = if (repeatSwitch.isChecked) {
                parseOptionalDurationMinutes(weekendSnoozeEt, weekendSnoozeUnitSpinner, "Weekend snooze")
            } else null

            if (repeatSwitch.isChecked && (
                    hasInvalidDuration(snoozeEt, snoozeMinutes) ||
                        hasInvalidDuration(weekdaySnoozeEt, weekdaySnoozeMinutes) ||
                        hasInvalidDuration(weekendSnoozeEt, weekendSnoozeMinutes)
                    )
            ) {
                return@setOnClickListener
            }

            if (editingTask == null) {
                val newId = System.currentTimeMillis().toInt() and 0x7FFFFFFF
                val task = Task(
                    id = newId,
                    name = name,
                    dayOfWeek = calendarDay,
                    hour = h,
                    minute = m,
                    nextAskEpoch = nextEpoch,
                    scheduledEpoch = nextEpoch,
                    repeatIntervalMinutes = repeatMinutes,
                    snoozeMinutes = snoozeMinutes,
                    weekdaySnoozeMinutes = weekdaySnoozeMinutes,
                    weekendSnoozeMinutes = weekendSnoozeMinutes
                )
                tasks.add(task)
                AlarmScheduler.schedule(task, this)
            } else {
                editingTask!!.also { task ->
                    AlarmScheduler.cancel(task, this)
                    task.name = name
                    task.dayOfWeek = calendarDay
                    task.hour = h
                    task.minute = m
                    task.nextAskEpoch = nextEpoch
                    task.scheduledEpoch = nextEpoch
                    task.repeatIntervalMinutes = repeatMinutes
                    task.snoozeMinutes = snoozeMinutes
                    task.weekdaySnoozeMinutes = weekdaySnoozeMinutes
                    task.weekendSnoozeMinutes = weekendSnoozeMinutes
                    AlarmScheduler.schedule(task, this)
                }
            }

            PrefsHelper.saveTasks(this, tasks)
            finish()
        }
    }

    private fun parseOptionalDurationMinutes(field: EditText, unitSpinner: Spinner, label: String): Int? {
        if (field.text.toString().trim().isEmpty()) return null
        return parseDurationMinutes(field, unitSpinner, label, 30 * 24 * 60)
    }

    private fun parseDurationMinutes(
        field: EditText,
        unitSpinner: Spinner,
        label: String,
        maxMinutes: Int
    ): Int? {
        val raw = field.text.toString().trim().toIntOrNull()
        if (raw == null || raw <= 0) {
            field.error = "Enter a positive number"
            return null
        }
        val multiplier = when (unitSpinner.selectedItemPosition) {
            0 -> 1
            1 -> 60
            else -> 60 * 24
        }
        val totalMinutes = raw * multiplier
        if (totalMinutes > maxMinutes) {
            val maxDays = maxMinutes / (24 * 60)
            field.error = "$label too long (max $maxDays days)"
            return null
        }
        return totalMinutes
    }

    private fun hasInvalidDuration(field: EditText, parsedValue: Int?): Boolean {
        return field.text.toString().trim().isNotEmpty() && parsedValue == null
    }

    private fun fillDurationField(field: EditText, unitSpinner: Spinner, totalMinutes: Int?) {
        totalMinutes ?: return
        val (value, unitIdx) = when {
            totalMinutes % (60 * 24) == 0 -> totalMinutes / (60 * 24) to 2
            totalMinutes % 60 == 0 -> totalMinutes / 60 to 1
            else -> totalMinutes to 0
        }
        field.setText(value.toString())
        unitSpinner.setSelection(unitIdx)
    }
}
