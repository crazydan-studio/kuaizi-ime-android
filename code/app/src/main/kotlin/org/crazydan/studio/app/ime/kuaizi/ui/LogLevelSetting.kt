/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogLevel

/**
 * 日志等级设置组件。
 *
 * Debug 构建固定为 VERBOSE 等级，不允许修改。
 * Release 构建通过 AlertDialog 中的 RadioButton 选择日志等级。
 * 等级变更立即生效，不需要重启应用。
 */
@Composable
fun LogLevelSetting(
    /** 当前日志等级 */
    currentLevel: LogLevel,
    /** 是否为 debug 构建 */
    isDebugBuild: Boolean,
    /** 等级变更回调 */
    onLevelChange: (LogLevel) -> Unit,
) {
    if (isDebugBuild) {
        // Debug 构建：显示固定等级提示，不允许修改
        ListItem(
            headlineContent = { Text("日志等级") },
            supportingContent = { Text("调试构建固定为 VERBOSE") },
        )
    } else {
        // Release 构建：可点击打开等级选择对话框
        var showDialog by remember { mutableStateOf(false) }

        ListItem(
            headlineContent = { Text("日志等级") },
            supportingContent = { Text(currentLevel.name) },
            modifier = Modifier.clickable { showDialog = true },
        )

        // 等级选择对话框
        if (showDialog) {
//            AlertDialog(
//                title = { Text("选择日志等级") },
//                text = {
//                    Column {
//                        LogLevel.entries.forEach { level ->
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .clickable {
//                                        onLevelChange(level)
//                                        showDialog = false
//                                    }
//                                    .padding(vertical = 8.dp),
//                                verticalAlignment = Alignment.CenterVertically,
//                            ) {
//                                RadioButton(
//                                    selected = level == currentLevel,
//                                    onClick = {
//                                        onLevelChange(level)
//                                        showDialog = false
//                                    },
//                                )
//                                Text(level.displayName)
//                            }
//                        }
//                    }
//                },
//                confirmButton = {
//                    TextButton(onClick = { showDialog = false }) {
//                        Text("取消")
//                    }
//                },
//            )
        }
    }
}

/** LogLevel 的中文显示名称及说明。 */
private val LogLevel.displayName: String
    get() = when (this) {
        LogLevel.VERBOSE -> "VERBOSE（详细）- 记录所有日志"
        LogLevel.DEBUG -> "DEBUG（调试）- 记录调试及以上日志"
        LogLevel.INFO -> "INFO（信息）- 记录一般及以上日志"
        LogLevel.WARN -> "WARN（警告）- 仅记录警告和错误"
        LogLevel.ERROR -> "ERROR（错误）- 仅记录错误"
    }
