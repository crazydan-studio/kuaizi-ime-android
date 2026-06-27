package org.crazydan.studio.app.ime.kuaizi.ui.viewmodel

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent

sealed class PopupTipState {
    data class Message(
        val message: String,
        val timeoutMs: Long = 3000L,
    ) : PopupTipState()

    data class Action(
        val message: String,
        val actionLabel: String,
        val action: ImeIntent,
        val persistent: Boolean = false,
        val timeoutMs: Long = 5000L,
    ) : PopupTipState()
}
