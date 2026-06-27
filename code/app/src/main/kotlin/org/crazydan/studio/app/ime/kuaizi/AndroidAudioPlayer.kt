/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi

import android.content.Context
import android.media.SoundPool
import org.crazydan.studio.app.ime.kuaizi.engine.AudioType
import org.crazydan.studio.app.ime.kuaizi.ui.AudioPlayer

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
