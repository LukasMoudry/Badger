package com.example.badger

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PrefsHelper {
    private const val PREFS_NAME = "badger_prefs"
    private const val KEY_TASKS = "tasks"

    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<Task>>() {}.type

    fun loadTasks(ctx: Context): MutableList<Task> {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TASKS, null) ?: return mutableListOf()
        return gson.fromJson<MutableList<Task>>(json, listType).onEach { task ->
            if (task.scheduledEpoch == 0L) {
                task.scheduledEpoch = task.nextAskEpoch
            }
        }
    }

    fun saveTasks(ctx: Context, tasks: List<Task>) {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TASKS, gson.toJson(tasks))
            .apply()
    }
}
