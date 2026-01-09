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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    private lateinit var repeatUnitSpinner: Spinner
    private lateinit var snoozeEt: EditText
    private lateinit var snoozeUnitSpinner: Spinner
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
        repeatUnitSpinner = findViewById(R.id.repeatUnitSpinner)
        snoozeUnitSpinner = findViewById(R.id.snoozeUnitSpinner)
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
                    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    startDateBtn.text = dateFormat.format(startDate!!.time)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Spinner with Monday first
        val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
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
            timePicker.setTimeCompat(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        } else {
            // Editing: fill from task
            editingTask!!.let { t ->
                nameEt.setText(t.name)
                val idx = (t.dayOfWeek + 5) % 7
                daySpinner.setSelection(idx)
                timePicker.setTimeCompat(t.hour, t.minute)
                if (t.repeatIntervalMinutes != null) {
                    repeatSwitch.isChecked = true
                    repeatContainer.visibility = View.VISIBLE
                    val interval = t.repeatIntervalMinutes!!
                    val (value, unitIdx) = when {
                        interval % (60 * 24) == 0 -> interval / (60 * 24) to 2
                        interval % 60 == 0 -> interval / 60 to 1
                        else -> interval to 0
                    }
                    repeatIntervalEt.setText(value.toString())
                    repeatUnitSpinner.setSelection(unitIdx)
                    t.snoozeMinutes?.let { sm ->
                        val (sVal, sUnit) = when {
                            sm % (60 * 24) == 0 -> sm / (60 * 24) to 2
                            sm % 60 == 0 -> sm / 60 to 1
                            else -> sm to 0
                        }
                        snoozeEt.setText(sVal.toString())
                        snoozeUnitSpinner.setSelection(sUnit)
                    }
                    startDate = Calendar.getInstance().apply { timeInMillis = t.nextAskEpoch }
                    startDate?.let { sd ->
                        val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                        startDateBtn.text = dateFormat.format(sd.time)
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
            val h = timePicker.getHourCompat()
            val m = timePicker.getMinuteCompat()

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

            val repeatMinutes = if (repeatSwitch.isChecked) {
                val raw = repeatIntervalEt.text.toString().toIntOrNull()
                if (raw == null || raw <= 0) {
                    repeatIntervalEt.error = "Enter a positive number"
                    return@setOnClickListener
                }
                val multiplier = when (repeatUnitSpinner.selectedItemPosition) {
                    0 -> 1      // minutes
                    1 -> 60     // hours
                    else -> 60 * 24  // days
                }
                val totalMinutes = raw * multiplier
                // Validate reasonable range: 1 minute to 365 days
                if (totalMinutes < 1) {
                    repeatIntervalEt.error = "Interval too short"
                    return@setOnClickListener
                }
                if (totalMinutes > 365 * 24 * 60) {
                    repeatIntervalEt.error = "Interval too long (max 365 days)"
                    return@setOnClickListener
                }
                totalMinutes
            } else null

            val snoozeMinutes = if (repeatSwitch.isChecked) {
                val raw = snoozeEt.text.toString().toIntOrNull()
                raw?.let {
                    if (it <= 0) {
                        snoozeEt.error = "Enter a positive number"
                        return@setOnClickListener
                    }
                    val multiplier = when (snoozeUnitSpinner.selectedItemPosition) {
                        0 -> 1      // minutes
                        1 -> 60     // hours
                        else -> 60 * 24  // days
                    }
                    val totalMinutes = it * multiplier
                    // Validate reasonable range: 1 minute to 30 days
                    if (totalMinutes < 1) {
                        snoozeEt.error = "Snooze too short"
                        return@setOnClickListener
                    }
                    if (totalMinutes > 30 * 24 * 60) {
                        snoozeEt.error = "Snooze too long (max 30 days)"
                        return@setOnClickListener
                    }
                    totalMinutes
                }
            } else null

            if (editingTask == null) {
                // Create new - use timestamp-based ID for uniqueness
                val newId = System.currentTimeMillis().toInt() and 0x7FFFFFFF  // Positive int from timestamp
                val t = Task(
                    newId,
                    name,
                    calendarDay,
                    h,
                    m,
                    nextEpoch,
                    repeatIntervalMinutes = repeatMinutes,
                    snoozeMinutes = snoozeMinutes
                )
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
                    t.repeatIntervalMinutes = repeatMinutes
                    t.snoozeMinutes = snoozeMinutes
                    AlarmScheduler.schedule(t, this)
                }
            }

            PrefsHelper.saveTasks(this, tasks)
            finish()
        }
    }
}
