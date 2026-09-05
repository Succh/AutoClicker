package com.succh.unifeed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 预设的公共 RSSHub 实例 */
private val RSSHUB_PRESETS = listOf(
    "https://rsshub.app",
    "https://rsshub.cups.moe",
    "https://rss.owo.nz",
    "https://hub.slarker.me",
    "https://rsshub.rssforever.com"
)

/** 设置 Tab：列表设置、RSSHub 实例配置、关于。 */
@Composable
fun SettingsScreen(
    prefs: ReaderPrefs,
    modifier: Modifier = Modifier,
    onRsshubInstanceChange: (String?) -> Unit = {}
) {
    var instanceInput by remember(prefs.rsshubInstance) { mutableStateOf(prefs.rsshubInstance ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("个人设置", style = MaterialTheme.typography.titleLarge)

        Text("列表设置", style = MaterialTheme.typography.titleMedium)
        SwitchSetting("列表显示摘要", prefs.listShowSummary) { prefs.listShowSummary = it }
        SwitchSetting("打开后自动标记已读", prefs.autoMarkRead) { prefs.autoMarkRead = it }

        HorizontalDivider()

        Text("RSSHub 实例", style = MaterialTheme.typography.titleMedium)
        Text(
            "填写自建或第三方实例地址，优先使用；失败时自动回退公共镜像。留空则使用内置自动策略。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = instanceInput,
            onValueChange = { instanceInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("https://rsshub.example.com") },
            trailingIcon = {
                if (instanceInput.isNotEmpty() || prefs.rsshubInstance != null) {
                    TextButton(onClick = {
                        instanceInput = ""
                        prefs.rsshubInstance = null
                        onRsshubInstanceChange(null)
                    }) { Text("恢复自动") }
                }
            }
        )
        TextButton(
            onClick = {
                prefs.rsshubInstance = instanceInput
                onRsshubInstanceChange(instanceInput.trim())
            },
            enabled = instanceInput.trim().isNotEmpty()
        ) { Text("保存") }

        Text("公共实例快捷选择", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RSSHUB_PRESETS.take(3).forEach { preset ->
                AssistChip(onClick = {
                    instanceInput = preset
                    prefs.rsshubInstance = preset
                    onRsshubInstanceChange(preset)
                }, label = { Text(preset.removePrefix("https://")) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RSSHUB_PRESETS.drop(3).forEach { preset ->
                AssistChip(onClick = {
                    instanceInput = preset
                    prefs.rsshubInstance = preset
                    onRsshubInstanceChange(preset)
                }, label = { Text(preset.removePrefix("https://")) })
            }
        }

        HorizontalDivider()

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