package org.crazydan.studio.app.ime.kuaizi.ui

import org.crazydan.studio.app.ime.kuaizi.engine.AudioType
import org.crazydan.studio.app.ime.kuaizi.engine.HapticType

interface FeedbackPlayer<T> {
    fun play(type: T)
}

typealias AudioPlayer = FeedbackPlayer<AudioType>
typealias HapticPlayer = FeedbackPlayer<HapticType>
