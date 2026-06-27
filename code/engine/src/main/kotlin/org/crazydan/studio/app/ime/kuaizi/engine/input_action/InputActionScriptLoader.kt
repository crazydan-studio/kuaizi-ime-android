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
