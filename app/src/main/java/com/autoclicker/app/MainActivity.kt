package com.autoclicker.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.autoclicker.app.databinding.ActivityMainBinding
import com.autoclicker.app.fragment.ConfigListFragment
import com.autoclicker.app.fragment.ServerFragment
import com.autoclicker.app.server.HttpServerService
import com.autoclicker.app.service.AccessibilityClickService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // 底部导航
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_configs -> loadFragment(ConfigListFragment())
                R.id.nav_server -> loadFragment(ServerFragment())
                else -> false
            }
        }

        // 默认页面
        if (savedInstanceState == null) {
            loadFragment(ConfigListFragment())
        }

        // 检查无障碍服务状态
        checkAccessibility()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }

    private fun checkAccessibility() {
        if (!isAccessibilityServiceEnabled(this, AccessibilityClickService::class.java)) {
            AlertDialog.Builder(this)
                .setTitle("需要无障碍权限")
                .setMessage("自动点击功能需要无障碍服务权限才能操作界面。\n\n请在设置中开启 AutoClicker 的无障碍服务。")
                .setPositiveButton("去开启") { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton("稍后", null)
                .show()
        }
    }

    private fun updateStatus() {
        val accEnabled = isAccessibilityServiceEnabled(this, AccessibilityClickService::class.java)
        val serverRunning = HttpServerService.isRunning

        binding.statusText.text = buildString {
            append("无障碍: ${if (accEnabled) "✅" else "❌"}")
            append("  |  ")
            append("HTTP服务: ${if (serverRunning) "✅ :${HttpServerService.port}" else "❌"}")
        }
    }

    companion object {
        fun isAccessibilityServiceEnabled(
            context: Context,
            serviceClass: Class<AccessibilityClickService>
        ): Boolean {
            val serviceName = ComponentName(context, serviceClass).flattenToString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                if (colonSplitter.next().equals(serviceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }
}
