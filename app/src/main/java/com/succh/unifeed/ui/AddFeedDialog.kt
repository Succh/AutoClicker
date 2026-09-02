package com.succh.unifeed.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun AddFeedDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加订阅") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("RSS/Atom 链接") },
                placeholder = { Text("https://example.com/rss") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        onConfirm(url)
                        onDismiss()
                    }
                },
                enabled = url.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = androidx.compose.ui.Modifier.size(16.dp))
                Spacer(androidx.compose.foundation.layout.Spacer(androidx.compose.foundation.layout.width(4.dp)))
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
