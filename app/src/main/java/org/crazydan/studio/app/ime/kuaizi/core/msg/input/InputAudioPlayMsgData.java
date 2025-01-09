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

package org.crazydan.studio.app.ime.kuaizi.core.msg.input;

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;

/**
 * {@link InputMsgType#InputAudio_Play_Doing} 消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-17
 */
public class InputAudioPlayMsgData extends InputMsgData {
    public final AudioType audioType;

    public InputAudioPlayMsgData(Key key, AudioType audioType) {
        super(key);
        this.audioType = audioType;
    }

    public enum AudioType {
        SingleTick(R.raw.tick_single),
        DoubleTick(R.raw.tick_double),
        ClockTick(R.raw.tick_clock),
        PingTick(R.raw.tick_ping),
        PageFlip(R.raw.page_flip),
        ;

        public final int resId;

        AudioType(int resId) {
            this.resId = resId;
        }
    }
}
