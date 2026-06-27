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
