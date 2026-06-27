package org.crazydan.studio.app.ime.kuaizi.engine.input_action

class InputActionScriptLoader {
    private val presets: MutableMap<String, InputActionScript> = mutableMapOf()

    fun registerPreset(name: String, script: InputActionScript) {
        presets[name] = script
    }

    fun loadPreset(name: String): InputActionScript? = presets[name]

    fun listPresets(): List<String> = presets.keys.toList()

    fun loadFromFile(uri: String): InputActionScript? {
        return null
    }
}
