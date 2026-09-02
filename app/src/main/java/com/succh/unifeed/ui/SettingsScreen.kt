package com.succh.unifeed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 设置 Tab：承载列表摘要、自动标记已读等个人设置。 */
@Composable
fun SettingsScreen(
    prefs: ReaderPrefs,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("个人设置", style = MaterialTheme.typography.titleLarge)
        Text("列表设置", style = MaterialTheme.typography.titleMedium)
        SwitchSetting("列表显示摘要", prefs.listShowSummary) { prefs.listShowSummary = it }
        SwitchSetting("打开后自动标记已读", prefs.autoMarkRead) { prefs.autoMarkRead = it }
        Spacer(Modifier.height(8.dp))
        Text("关于", style = MaterialTheme.typography.titleMedium)
        Text(
            "UniFeed v1.0 • 开源 RSS 阅读器",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SwitchSetting(label: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = value, onCheckedChange = onValueChange)
    }
}