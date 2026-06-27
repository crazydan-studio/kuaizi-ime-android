package org.crazydan.studio.app.ime.kuaizi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
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
import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel

@Composable
fun LogLevelSetting(
    currentLevel: LogLevel,
    isDebugBuild: Boolean,
    onLevelChange: (LogLevel) -> Unit,
) {
    if (isDebugBuild) {
        ListItem(
            headlineContent = { Text("日志等级") },
            supportingContent = { Text("调试构建固定为 VERBOSE") },
        )
    } else {
        var showDialog by remember { mutableStateOf(false) }

        ListItem(
            headlineContent = { Text("日志等级") },
            supportingContent = { Text(currentLevel.name) },
            modifier = Modifier.clickable { showDialog = true },
        )

        if (showDialog) {
            AlertDialog(
                title = { Text("选择日志等级") },
                text = {
                    Column {
                        LogLevel.entries.forEach { level ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLevelChange(level)
                                        showDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = level == currentLevel,
                                    onClick = {
                                        onLevelChange(level)
                                        showDialog = false
                                    },
                                )
                                Text(level.displayName)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("取消")
                    }
                },
            )
        }
    }
}

private val LogLevel.displayName: String
    get() = when (this) {
        LogLevel.VERBOSE -> "VERBOSE（详细）- 记录所有日志"
        LogLevel.DEBUG -> "DEBUG（调试）- 记录调试及以上日志"
        LogLevel.INFO -> "INFO（信息）- 记录一般及以上日志"
        LogLevel.WARN -> "WARN（警告）- 仅记录警告和错误"
        LogLevel.ERROR -> "ERROR（错误）- 仅记录错误"
    }
