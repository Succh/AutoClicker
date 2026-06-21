package com.autoclicker.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import com.autoclicker.app.engine.AutomationEngine
import com.autoclicker.app.server.HttpServerService
import com.autoclicker.app.storage.ConfigStorage

class AutoClickerApp : Application() {

    companion object {
        private const val TAG = "AutoClickerApp"
    }

    lateinit var storage: ConfigStorage
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        storage = ConfigStorage.getInstance(this)

        // 初始化引擎
        AutomationEngine(storage)

        Log.i(TAG, "AutoClicker 应用启动")
    }

    companion object {
        @Volatile
        lateinit var instance: AutoClickerApp
            private set
    }
}
