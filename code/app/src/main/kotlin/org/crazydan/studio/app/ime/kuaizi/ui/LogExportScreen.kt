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

import android.net.Uri
import androidx.compose.runtime.Composable

/**
 * 日志导出界面。
 *
 * 通过 Android Activity Result API 的 [CreateDocument] 合约，
 * 让用户选择保存位置，默认文件名包含日期范围。
 * 导出操作委托给调用方提供的 [onExport] 回调。
 */
@Composable
fun LogExportScreen(
    /** 导出回调，接收用户选择的 URI */
    onExport: (Uri) -> Unit,
) {
//    // 创建文件选择启动器，MIME 类型为纯文本
//    val createDocumentLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.CreateDocument("text/plain"),
//    ) { uri ->
//        uri?.let(onExport)
//    }
//
//    // 启动文件选择，默认文件名包含最近 7 天的日期范围
//    startExport(
//        launcher = createDocumentLauncher,
//        fromDate = null,
//        toDate = null,
//    )
}

/**
 * 启动导出文件选择器。
 *
 * 生成默认文件名格式：kuaizi_ime_log_{from}_{to}.txt
 * 日期范围默认为最近 7 天。
 */
private fun startExport(
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    fromDate: java.time.LocalDate?,
    toDate: java.time.LocalDate?,
) {
    val dateFormat = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
    val from = fromDate ?: java.time.LocalDate.now().minusDays(7)
    val to = toDate ?: java.time.LocalDate.now()
    val fileName = "kuaizi_ime_log_${dateFormat.format(from)}_${dateFormat.format(to)}.txt"
    launcher.launch(fileName)
}
