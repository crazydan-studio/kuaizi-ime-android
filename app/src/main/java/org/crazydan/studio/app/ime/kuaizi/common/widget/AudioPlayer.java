/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
