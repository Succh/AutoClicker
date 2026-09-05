package com.succh.unifeed.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

enum class ReaderTheme { LIGHT, SEPIA, DARK }

class ReaderPrefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("unifeed_prefs", Context.MODE_PRIVATE)

    private val _fontSize = mutableFloatStateOf(sp.getFloat(KEY_SIZE, 17f))
    var fontSize: Float
        get() = _fontSize.floatValue
        set(value) {
            val v = value.coerceIn(13f, 24f)
            _fontSize.floatValue = v
            sp.edit().putFloat(KEY_SIZE, v).apply()
        }

    private val _lineHeight = mutableFloatStateOf(sp.getFloat(KEY_LH, 1.7f))
    var lineHeight: Float
        get() = _lineHeight.floatValue
        set(value) {
            val v = value.coerceIn(1.2f, 2.2f)
            _lineHeight.floatValue = v
            sp.edit().putFloat(KEY_LH, v).apply()
        }

    private val _theme = mutableStateOf(loadTheme())
    var theme: ReaderTheme
        get() = _theme.value
        set(value) {
            _theme.value = value
            sp.edit().putString(KEY_THEME, value.name).apply()
        }

    private val _showImages = mutableStateOf(sp.getBoolean(KEY_IMAGES, true))
    var showImages: Boolean
        get() = _showImages.value
        set(value) {
            _showImages.value = value
            sp.edit().putBoolean(KEY_IMAGES, value).apply()
        }

    private val _serifFont = mutableStateOf(sp.getBoolean(KEY_SERIF, false))
    var serifFont: Boolean
        get() = _serifFont.value
        set(value) {
            _serifFont.value = value
            sp.edit().putBoolean(KEY_SERIF, value).apply()
        }

    private val _justifyText = mutableStateOf(sp.getBoolean(KEY_JUSTIFY, false))
    var justifyText: Boolean
        get() = _justifyText.value
        set(value) {
            _justifyText.value = value
            sp.edit().putBoolean(KEY_JUSTIFY, value).apply()
        }

    private val _listShowSummary = mutableStateOf(sp.getBoolean(KEY_LIST_SUMMARY, false))
    var listShowSummary: Boolean
        get() = _listShowSummary.value
        set(value) {
            _listShowSummary.value = value
            sp.edit().putBoolean(KEY_LIST_SUMMARY, value).apply()
        }

    private val _autoMarkRead = mutableStateOf(sp.getBoolean(KEY_AUTO_READ, true))
    var autoMarkRead: Boolean
        get() = _autoMarkRead.value
        set(value) {
            _autoMarkRead.value = value
            sp.edit().putBoolean(KEY_AUTO_READ, value).apply()
        }

    /** 自定义 RSSHub 实例地址（null 表示使用内置自动镜像策略） */
    private val _rsshubInstance = mutableStateOf(sp.getString(KEY_RSSHUB_INSTANCE, null))
    var rsshubInstance: String?
        get() = _rsshubInstance.value
        set(value) {
            val v = value?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
            _rsshubInstance.value = v
            sp.edit().putString(KEY_RSSHUB_INSTANCE, v).apply()
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
        const val KEY_RSSHUB_INSTANCE = "rsshub_instance"
    }
}
