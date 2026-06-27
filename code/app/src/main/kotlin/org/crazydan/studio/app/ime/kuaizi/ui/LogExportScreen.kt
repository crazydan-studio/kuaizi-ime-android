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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
fun LogExportScreen(
    onExport: (Uri) -> Unit,
) {
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        uri?.let(onExport)
    }

    startExport(
        launcher = createDocumentLauncher,
        fromDate = null,
        toDate = null,
    )
}

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
