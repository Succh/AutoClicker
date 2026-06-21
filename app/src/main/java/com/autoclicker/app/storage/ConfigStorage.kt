package com.autoclicker.app.storage

import android.content.Context
import android.content.SharedPreferences
import com.autoclicker.app.model.AutoConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ConfigStorage(context: Context) {
    private val prefs = context.getSharedPreferences("auto_clicker_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    companion object {
        @Volatile private var instance: ConfigStorage? = null
        fun getInstance(context: Context): ConfigStorage = instance ?: synchronized(this) { instance ?: ConfigStorage(context.applicationContext).also { instance = it } }
    }
    fun getAllConfigs(): List<AutoConfig> {
        val json = prefs.getString("configs", "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<AutoConfig>>() {}.type)
    }
    fun getConfig(id: String) = getAllConfigs().find { it.id == id }
    fun getConfigByName(name: String) = getAllConfigs().find { it.name.equals(name, true) }
    fun saveConfig(config: AutoConfig) {
        val list = getAllConfigs().toMutableList()
        val idx = list.indexOfFirst { it.id == config.id }
        val updated = config.copy(updatedAt = System.currentTimeMillis())
        if (idx >= 0) list[idx] = updated else list.add(updated)
        prefs.edit().putString("configs", gson.toJson(list)).apply()
    }
    fun deleteConfig(id: String): Boolean {
        val list = getAllConfigs().toMutableList()
        val ok = list.removeAll { it.id == id }
        if (ok) prefs.edit().putString("configs", gson.toJson(list)).apply()
        return ok
    }
    fun importConfig(json: String) = try { gson.fromJson(json, AutoConfig::class.java).also { saveConfig(it) } } catch (e: Exception) { null }
    fun getHttpPort() = prefs.getInt("http_port", 8765)
    fun setHttpPort(port: Int) { prefs.edit().putInt("http_port", port).apply() }
}