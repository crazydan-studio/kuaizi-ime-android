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

/**
 * 输入动作脚本加载器：管理预设脚本和从文件加载脚本。
 *
 * 提供预设脚本的注册、查询和列表功能，以及基础的文件加载能力。
 * 预设脚本由 [InputActionScriptCompiler] 编译后通过 [registerPreset] 注册，
 * 回放器通过 [loadPreset] 获取预设脚本进行播放。
 */
class InputActionScriptLoader {
    // 预设脚本缓存，key 为脚本名称
    private val presets: MutableMap<String, InputActionScript> = mutableMapOf()

    /** 注册一个预设脚本，可通过 [loadPreset] 按名称加载。 */
    fun registerPreset(name: String, script: InputActionScript) {
        presets[name] = script
    }

    /** 按名称加载预设脚本，不存在时返回 null。 */
    fun loadPreset(name: String): InputActionScript? = presets[name]

    /** 列出所有已注册的预设脚本名称。 */
    fun listPresets(): List<String> = presets.keys.toList()

    /**
     * 从文件加载脚本。
     * 当前为基础实现，使用简单的文本解析方式读取脚本元数据。
     * 后续可扩展为完整的 JSON/YAML 格式解析。
     */
    fun loadFromFile(filePath: String): InputActionScript? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            val content = file.readText()
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
