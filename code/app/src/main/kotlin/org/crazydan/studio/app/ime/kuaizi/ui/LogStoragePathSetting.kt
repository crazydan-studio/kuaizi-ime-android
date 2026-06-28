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

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * 日志存储路径设置组件。
 *
 * 通过 Android SAF（Storage Access Framework）的 [OpenDocumentTree] 合约
 * 让用户选择日志存储目录。选择后获取持久化 URI 权限，将路径保存到配置。
 *
 * 缺省路径为应用私有目录下的 logs/ 子目录，无需用户配置。
 * 用户选择外部存储路径后，日志文件在应用卸载后仍可保留。
 */
@Composable
fun LogStoragePathSetting(
    /** 当前配置的存储路径，null 表示使用缺省路径 */
    currentPath: String?,
    /** 路径变更回调 */
    onPathChange: (String?) -> Unit,
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }

    // 显示当前路径，点击打开目录选择器
    ListItem(
        headlineContent = { Text("日志存储路径") },
        supportingContent = {
            Text(currentPath ?: "应用私有目录（缺省）")
        },
        modifier = Modifier.clickable { showPicker = true },
    )

    // 延迟创建启动器，仅在需要时初始化
    if (showPicker) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri?.let {
                // 获取持久化 URI 权限，确保应用重启后仍可访问
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                onPathChange(it.toString())
            }
            showPicker = false
        }
    }
}
