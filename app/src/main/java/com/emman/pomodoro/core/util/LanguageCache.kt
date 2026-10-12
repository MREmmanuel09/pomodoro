package com.emman.pomodoro.core.util

import android.content.Context
import com.emman.pomodoro.domain.model.AppLanguage

object LanguageCache {

    private const val PREFS = "pomodoro_cache"
    private const val KEY_LANGUAGE = "language"

    fun read(context: Context): AppLanguage? {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null) ?: return null
        return runCatching { AppLanguage.valueOf(name) }.getOrNull()
    }

    fun write(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.name)
            .apply()
    }
}
