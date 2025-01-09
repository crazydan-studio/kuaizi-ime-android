/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
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

package org.crazydan.studio.app.ime.kuaizi.common.widget;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

/**
 * 声音播放器
 * <p/>
 * 使用 {@link SoundPool} 比 MediaPlayer 更轻量级，
 * 适合播放小文件，且不容易出现高频率播放出现中断的情况
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-16
 */
public class AudioPlayer {
    private final SoundPool soundPool;
    private final Map<Integer, Integer> resources = new HashMap<>();

    public AudioPlayer() {
        AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                                             .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                                             .build();
        this.soundPool = new SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attrs).build();
    }

    public void load(Context context, int... resIds) {
        for (int resId : resIds) {
            int soundId = this.soundPool.load(context, resId, 1);
            this.resources.put(resId, soundId);
        }
    }

    public void play(int resId) {
        int soundId = this.resources.getOrDefault(resId, 0);
        if (soundId > 0) {
            this.soundPool.play(soundId, 1, 1, 0, 0, 1);
        }
    }
}
