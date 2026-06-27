package org.crazydan.studio.app.ime.kuaizi.engine.domain

import org.crazydan.studio.app.ime.kuaizi.engine.AudioType

class KeyAudioPlayer(private val player: (AudioType) -> Unit = {}) {
    fun play(type: AudioType) {
        player(type)
    }
}
