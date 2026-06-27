package org.crazydan.studio.app.ime.kuaizi.engine

interface AudioPlayer {
    fun play(type: AudioType)
}

interface HapticPlayer {
    fun play(type: HapticType)
}
