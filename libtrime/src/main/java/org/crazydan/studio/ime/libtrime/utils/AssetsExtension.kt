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

package org.crazydan.studio.ime.libtrime.utils

import android.content.res.AssetManager
import java.io.File

/** 将 `assets` 目录中的指定文件或指定目录中的所有文件复制到目标目录中 */
fun AssetManager.copyToDir(source: String, targetDir: File, recursively: Boolean = true) {
    copyAssetFilesToDir(this, source, targetDir, recursively = recursively)
}

private fun copyAssetFilesToDir(
    assets: AssetManager,
    source: String,
    targetDir: File,
    targetFilePath: String = "",
    recursively: Boolean,
) {
    val sourceFiles = assets.list(source)

    // For normal file
    if (sourceFiles.isNullOrEmpty()) {
        val targetFile = File(
            targetDir,
            targetFilePath.ifEmpty { source.substringAfterLast('/') }
        ).also {
            it.parentFile?.mkdirs()
        }

        assets.open(source).use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    // For directory
    else if (recursively) {
        sourceFiles.forEach { file ->
            copyAssetFilesToDir(
                assets,
                source = "$source/$file", targetDir = targetDir,
                targetFilePath = if (targetFilePath.isEmpty()) file else "$targetFilePath/$file",
                recursively = true,
            )
        }
    }
}