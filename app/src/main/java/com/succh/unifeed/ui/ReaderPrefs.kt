package com.succh.unifeed.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ReaderTheme { LIGHT, SEPIA, DARK }

/**
 * 阅读设置：底层存 SharedPreferences，但暴露 Compose 可观察状态，
 * 修改后立即触发重组，实现设置面板实时预览效果。
 */
class ReaderPrefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("unifeed_prefs", Context.MODE_PRIVATE)

    var fontSize: Float by mutableFloatStateOf(sp.getFloat(KEY_SIZE, 17f))
        set(value) {
            field = value.coerceIn(13f, 24f)
            sp.edit().putFloat(KEY_SIZE, field).apply()
        }

    var lineHeight: Float by mutableFloatStateOf(sp.getFloat(KEY_LH, 1.7f))
        set(value) {
            field = value.coerceIn(1.2f, 2.2f)
            sp.edit().putFloat(KEY_LH, field).apply()
        }

    var theme: ReaderTheme by mutableStateOf(loadTheme())
        set(value) {
            field = value
            sp.edit().putString(KEY_THEME, value.name).apply()
        }

    var showImages: Boolean by mutableStateOf(sp.getBoolean(KEY_IMAGES, true))
        set(value) {
            field = value
            sp.edit().putBoolean(KEY_IMAGES, value).apply()
        }

    var serifFont: Boolean by mutableStateOf(sp.getBoolean(KEY_SERIF, false))
        set(value) {
            field = value
            sp.edit().putBoolean(KEY_SERIF, value).apply()
        }

    var justifyText: Boolean by mutableStateOf(sp.getBoolean(KEY_JUSTIFY, false))
        set(value) {
            field = value
            sp.edit().putBoolean(KEY_JUSTIFY, value).apply()
        }

    var listShowSummary: Boolean by mutableStateOf(sp.getBoolean(KEY_LIST_SUMMARY, false))
        set(value) {
            field = value
            sp.edit().putBoolean(KEY_LIST_SUMMARY, value).apply()
        }

    var autoMarkRead: Boolean by mutableStateOf(sp.getBoolean(KEY_AUTO_READ, true))
        set(value) {
            field = value
            sp.edit().putBoolean(KEY_AUTO_READ, value).apply()
        }

    private fun loadTheme(): ReaderTheme {
        val name = sp.getString(KEY_THEME, ReaderTheme.LIGHT.name)
        return ReaderTheme.entries.firstOrNull { it.name == name } ?: ReaderTheme.LIGHT
    }

    private companion object {
        const val KEY_SIZE = "font_size"
        const val KEY_LH = "line_height"
        const val KEY_THEME = "reader_theme"
        const val KEY_IMAGES = "show_images"
        const val KEY_SERIF = "serif_font"
        const val KEY_JUSTIFY = "justify_text"
        const val KEY_LIST_SUMMARY = "list_summary"
        const val KEY_AUTO_READ = "auto_read"
    }
}
