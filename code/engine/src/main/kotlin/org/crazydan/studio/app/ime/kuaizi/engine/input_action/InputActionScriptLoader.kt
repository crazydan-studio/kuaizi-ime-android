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

package org.crazydan.studio.app.ime.kuaizi.engine.input_action

import java.io.File

class InputActionScriptLoader {
    private val presets: MutableMap<String, InputActionScript> = mutableMapOf()

    fun registerPreset(name: String, script: InputActionScript) {
        presets[name] = script
    }

    fun loadPreset(name: String): InputActionScript? = presets[name]

    fun listPresets(): List<String> = presets.keys.toList()

    fun loadFromFile(filePath: String): InputActionScript? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            val content = file.readText()
            // Basic JSON-like parser stub - would need a real parser
            val lines = content.lines()
            val name = lines.firstOrNull()?.removePrefix("name:")?.trim() ?: "unknown"
            InputActionScript(
                name = name,
                description = "Loaded from $filePath",
                actions = emptyList(),
                totalDuration = 0L,
            )
        } catch (e: Exception) {
            null
        }
    }
}
