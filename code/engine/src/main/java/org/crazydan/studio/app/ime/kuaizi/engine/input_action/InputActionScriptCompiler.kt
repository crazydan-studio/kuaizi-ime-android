package org.crazydan.studio.app.ime.kuaizi.engine.input_action

class InputActionScriptCompiler {
    fun compile(
        text: String,
        method: InputActionMode,
        speed: Float = 1.0f,
    ): InputActionScript {
        val actions = mutableListOf<InputAction>()
        val now = System.currentTimeMillis()
        var offset = 0L

        for (ch in text) {
            when (method) {
                InputActionMode.Tap -> {
                    actions.add(InputAction.KeyDown(now + offset, ch.toString()))
                    offset += 50
                    actions.add(InputAction.KeyUp(now + offset, ch.toString()))
                    offset += 30
                }
                InputActionMode.Swipe -> {
                    actions.add(InputAction.KeyDown(now + offset, ch.toString()))
                    offset += 40
                    actions.add(InputAction.SwipeTo(now + offset, ch.toString(), ch.toString(), 60))
                    offset += 60
                    actions.add(InputAction.KeyUp(now + offset, ch.toString()))
                    offset += 20
                }
            }
        }

        return InputActionScript(
            name = "compiled_${text.take(10)}",
            description = "Auto-compiled from: $text",
            inputActionMode = method,
            actions = actions,
            totalDuration = offset,
        )
    }
}
