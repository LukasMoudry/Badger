package com.example.badger

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PrefsHelper {
    private const val PREFS = "badger_prefs"
    private const val KEY_TASKS = "tasks"
    private val gson = Gson()
    private val type = object : TypeToken<MutableList<Task>>() {}.type

    fun loadTasks(ctx: Context): MutableList<Task> {
        val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TASKS, null)
        return if (json.isNullOrEmpty()) mutableListOf()
        else gson.fromJson(json, type)
    }

    fun saveTasks(ctx: Context, list: List<Task>) {
        val json = gson.toJson(list)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TASKS, json)
            .apply()
    }
}
