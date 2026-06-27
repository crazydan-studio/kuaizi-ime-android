package org.crazydan.studio.app.ime.kuaizi.engine.input_action

data class InputActionScript(
    val name: String,
    val description: String,
    val inputActionMode: InputActionMode,
    val actions: List<InputAction>,
    val totalDuration: Long,
)

enum class InputActionMode { Tap, Swipe }
