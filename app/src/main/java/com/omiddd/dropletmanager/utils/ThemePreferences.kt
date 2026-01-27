package com.omiddd.dropletmanager.utils

import android.content.Context

object ThemePreferences {
    private const val PREFS = "settings_prefs"
    private const val KEY_DARK = "dark_mode"

    fun isDark(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, false)
    }

    fun setDark(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK, value).apply()
    }
}

