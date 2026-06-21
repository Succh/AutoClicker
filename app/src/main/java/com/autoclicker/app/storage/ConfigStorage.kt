package com.autoclicker.app.storage

import android.content.Context
import android.content.SharedPreferences
import com.autoclicker.app.model.AutoConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ConfigStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auto_clicker_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_CONFIGS = "configs"
        private const val KEY_HTTP_PORT = "http_port"
        private const val KEY_AUTO_START = "auto_start_server"
        private const val KEY_LAST_RUN = "last_run_config_id"

        @Volatile
        private var instance: ConfigStorage? = null

        fun getInstance(context: Context): ConfigStorage {
            return instance ?: synchronized(this) {
                instance ?: ConfigStorage(context.applicationContext).also { instance = it }
            }
        }
    }

    // ==================== 配置管理 ====================

    fun getAllConfigs(): List<AutoConfig> {
        val json = prefs.getString(KEY_CONFIGS, "[]") ?: "[]"
        val type = object : TypeToken<List<AutoConfig>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getConfig(id: String): AutoConfig? {
        return getAllConfigs().find { it.id == id }
    }

    fun getConfigByName(name: String): AutoConfig? {
        return getAllConfigs().find { it.name.equals(name, ignoreCase = true) }
    }

    fun saveConfig(config: AutoConfig) {
        val configs = getAllConfigs().toMutableList()
        val index = configs.indexOfFirst { it.id == config.id }
        val updated = config.copy(updatedAt = System.currentTimeMillis())
        if (index >= 0) {
            configs[index] = updated
        } else {
            configs.add(updated)
        }
        prefs.edit().putString(KEY_CONFIGS, gson.toJson(configs)).apply()
    }

    fun deleteConfig(id: String): Boolean {
        val configs = getAllConfigs().toMutableList()
        val removed = configs.removeAll { it.id == id }
        if (removed) {
            prefs.edit().putString(KEY_CONFIGS, gson.toJson(configs)).apply()
        }
        return removed
    }

    fun importConfig(json: String): AutoConfig? {
        return try {
            val config = gson.fromJson(json, AutoConfig::class.java)
            saveConfig(config)
            config
        } catch (e: Exception) {
            null
        }
    }

    // ==================== 设置 ====================

    fun getHttpPort(): Int = prefs.getInt(KEY_HTTP_PORT, 8765)
    fun setHttpPort(port: Int) {
        prefs.edit().putInt(KEY_HTTP_PORT, port).apply()
    }

    fun isAutoStart(): Boolean = prefs.getBoolean(KEY_AUTO_START, false)
    fun setAutoStart(auto: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START, auto).apply()
    }

    fun setLastRunConfig(id: String?) {
        prefs.edit().putString(KEY_LAST_RUN, id).apply()
    }

    fun getLastRunConfig(): String? = prefs.getString(KEY_LAST_RUN, null)
}
