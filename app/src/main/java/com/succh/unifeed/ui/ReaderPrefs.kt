package com.succh.unifeed.ui

import android.content.Context
import android.content.SharedPreferences

enum class ReaderTheme { LIGHT, SEPIA, DARK }

class ReaderPrefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("unifeed_prefs", Context.MODE_PRIVATE)

    var fontSize: Float
        get() = sp.getFloat(KEY_SIZE, 17f)
        set(value) = sp.edit().putFloat(KEY_SIZE, value.coerceIn(13f, 24f)).apply()

    var lineHeight: Float
        get() = sp.getFloat(KEY_LH, 1.7f)
        set(value) = sp.edit().putFloat(KEY_LH, value.coerceIn(1.2f, 2.2f)).apply()

    var theme: ReaderTheme
        get() {
            val name = sp.getString(KEY_THEME, ReaderTheme.LIGHT.name)
            return ReaderTheme.entries.firstOrNull { it.name == name } ?: ReaderTheme.LIGHT
        }
        set(value) = sp.edit().putString(KEY_THEME, value.name).apply()

    var showImages: Boolean
        get() = sp.getBoolean(KEY_IMAGES, true)
        set(value) = sp.edit().putBoolean(KEY_IMAGES, value).apply()

    var serifFont: Boolean
        get() = sp.getBoolean(KEY_SERIF, false)
        set(value) = sp.edit().putBoolean(KEY_SERIF, value).apply()

    var justifyText: Boolean
        get() = sp.getBoolean(KEY_JUSTIFY, false)
        set(value) = sp.edit().putBoolean(KEY_JUSTIFY, value).apply()

    var listShowSummary: Boolean
        get() = sp.getBoolean(KEY_LIST_SUMMARY, false)
        set(value) = sp.edit().putBoolean(KEY_LIST_SUMMARY, value).apply()

    var autoMarkRead: Boolean
        get() = sp.getBoolean(KEY_AUTO_READ, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO_READ, value).apply()

    private companion object {
        const val KEY_SIZE = "font_size"
        const val KEY_LH = "line_height"
        const val KEY_THEME= "reader_theme"
        const val KEY_IMAGES= "show_images"
        const val KEY_SERIF = "serif_font"
        const val KEY_JUSTIFY = "justify_text"
        const val KEY_LIST_SUMMARY = "list_summary"
        const val KEY_AUTO_READ = "auto_read"
    }
}
