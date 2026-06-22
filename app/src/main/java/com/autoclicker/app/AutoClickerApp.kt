package com.autoclicker.app

import android.app.Application
import android.util.Log
import com.autoclicker.app.engine.AutomationEngine
import com.autoclicker.app.storage.ConfigStorage

class AutoClickerApp : Application() {

    lateinit var storage: ConfigStorage
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        storage = ConfigStorage.getInstance(this)
        AutomationEngine(storage)
        Log.i(TAG, "AutoClicker 应用启动")
    }

    companion object {
        private const val TAG = "AutoClickerApp"

        @Volatile
        lateinit var instance: AutoClickerApp
            private set
    }
}
