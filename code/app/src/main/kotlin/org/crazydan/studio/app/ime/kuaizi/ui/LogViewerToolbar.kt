package org.crazydan.studio.app.ime.kuaizi.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel

@Composable
fun LogViewerToolbar(
    levelFilter: LogLevel?,
    keyword: String?,
    onLevelFilterChange: (LogLevel?) -> Unit,
    onKeywordChange: (String?) -> Unit,
    onRefresh: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.padding(8.dp),
    ) {
        FilterChip(
            selected = levelFilter != null,
            onClick = { expanded = true },
            label = { Text(levelFilter?.name ?: "All") },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onLevelFilterChange(null)
                    expanded = false
                },
            )
            LogLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.name) },
                    onClick = {
                        onLevelFilterChange(level)
                        expanded = false
                    },
                )
            }
        }

        OutlinedTextField(
            value = keyword ?: "",
            onValueChange = { onKeywordChange(it.ifBlank { null }) },
            placeholder = { Text("搜索...") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )

        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "刷新")
        }
    }
}
