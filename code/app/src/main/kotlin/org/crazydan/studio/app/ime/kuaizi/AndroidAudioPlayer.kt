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
 * If not, see <see href="https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text"/>.
 */

package org.crazydan.studio.app.ime.kuaizi

import android.content.Context
import android.media.SoundPool
import org.crazydan.studio.app.ime.kuaizi.engine.AudioType
import org.crazydan.studio.app.ime.kuaizi.ui.AudioPlayer

/**
 * 基于 Android [SoundPool] 的音效播放器实现。
 *
 * 在 [IMEService.onCreate] 中创建并注入 [KeyboardViewModel]。
 * 所有音效资源在构造时预加载到 SoundPool 中，确保播放时零延迟。
 * 使用 SoundPool 而非 MediaPlayer，因为按键音需要低延迟、短时长、高并发。
 */
class AndroidAudioPlayer(context: Context) : AudioPlayer {

    // SoundPool 实例，最多同时播放 4 路音效
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .build()

    // 音效类型到 SoundPool ID 的映射，构造时预加载音频资源
    private val soundIds: Map<AudioType, Int> = mapOf(
        AudioType.KeyPress to soundPool.load(context, R.raw.tick_single, 1),
        AudioType.Slip to soundPool.load(context, R.raw.tick_single, 1),
        AudioType.CandidateSelect to soundPool.load(context, R.raw.tick_single, 1),
        AudioType.PageFlip to soundPool.load(context, R.raw.page_flip, 1),
    )

    /** 播放指定类型的音效。若音效未加载则静默跳过。 */
    override fun play(type: AudioType) {
        val soundId = soundIds[type] ?: return
        soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    /** 释放 SoundPool 资源，在 [IMEService.onDestroy] 中调用。 */
    fun release() {
        soundPool.release()
    }
}
