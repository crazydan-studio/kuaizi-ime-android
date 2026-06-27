package org.crazydan.studio.app.ime.kuaizi.engine

sealed class ImeEffect {
    sealed class PopupTip : ImeEffect() {
        data class Message(
            val message: String,
            val timeoutMs: Long = 3000L,
        ) : PopupTip()

        data class Action(
            val message: String,
            val actionLabel: String,
            val action: ImeIntent,
            val persistent: Boolean = false,
            val timeoutMs: Long = 5000L,
        ) : PopupTip()
    }

    data class PlayAudio(val type: AudioType) : ImeEffect()
    data class PlayHaptic(val type: HapticType) : ImeEffect()
}

enum class AudioType { KeyPress, CandidateSelect, Slip, PageFlip, }
enum class HapticType { LightTap, MediumTap, HeavyTap, }
