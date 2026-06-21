package com.autoclicker.app
import android.app.Application
import android.util.Log
import com.autoclicker.app.engine.AutomationEngine
import com.autoclicker.app.storage.ConfigStorage
class AutoClickerApp : Application() {
    lateinit var storage: ConfigStorage; private set
    override fun onCreate() {
        super.onCreate()
        instance = this
        storage = ConfigStorage.getInstance(this)
        AutomationEngine(storage)
    }
    companion object { @Volatile lateinit var instance: AutoClickerApp; private set }
}