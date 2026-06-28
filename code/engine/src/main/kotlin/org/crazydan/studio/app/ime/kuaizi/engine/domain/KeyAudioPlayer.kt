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

package org.crazydan.studio.app.ime.kuaizi.engine.domain

import org.crazydan.studio.app.ime.kuaizi.engine.AudioType

/**
 * 按键音效播放器，封装音效播放的回调函数。
 * 通过构造函数注入播放器实现，解耦引擎与音频播放的具体实现。
 *
 * @param player 音效播放回调函数，接收 [AudioType] 参数
 */
class KeyAudioPlayer(private val player: (AudioType) -> Unit = {}) {
    /**
     * 播放指定类型的音效
     * @param type 音效类型
     */
    fun play(type: AudioType) {
        player(type)
    }
}
