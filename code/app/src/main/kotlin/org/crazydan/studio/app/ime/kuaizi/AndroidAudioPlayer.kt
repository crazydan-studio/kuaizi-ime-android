package org.crazydan.studio.app.ime.kuaizi

import android.content.Context
import android.media.SoundPool
import org.crazydan.studio.app.ime.kuaizi.engine.AudioPlayer
import org.crazydan.studio.app.ime.kuaizi.engine.AudioType

class AndroidAudioPlayer(context: Context) : AudioPlayer {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .build()

    private val soundIds: Map<AudioType, Int> = mapOf(
        AudioType.KeyPress to soundPool.load(context, R.raw.key_press, 1),
        AudioType.Slip to soundPool.load(context, R.raw.slip, 1),
        AudioType.CandidateSelect to soundPool.load(context, R.raw.candidate_select, 1),
        AudioType.PageFlip to soundPool.load(context, R.raw.page_flip, 1),
    )

    override fun play(type: AudioType) {
        val soundId = soundIds[type] ?: return
        soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    fun release() {
        soundPool.release()
    }
}
