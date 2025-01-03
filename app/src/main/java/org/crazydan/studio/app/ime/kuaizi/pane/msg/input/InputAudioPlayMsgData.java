/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2023 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.pane.msg.input;

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType;

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
