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

import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-03-09
 */
object ZipUtils {

    fun unzipToDir(input: InputStream, targetDir: File) {
        ZipInputStream(input).use { zip ->
            unzipToDir(zip, targetDir)
        }
    }

    fun unzipToDir(zip: ZipInputStream, targetDir: File) {
        var entry: ZipEntry? = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val targetFile = File(targetDir, entry.name).also {
                    it.parentFile?.mkdirs()
                }

                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (zip.read(buffer).also { len = it } > 0) {
                        output.write(buffer, 0, len)
                    }
                }
            }
            zip.closeEntry()

            entry = zip.nextEntry
        }
    }
}