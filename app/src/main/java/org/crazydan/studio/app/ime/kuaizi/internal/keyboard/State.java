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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard;

import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;

/**
 * {@link Keyboard 键盘}所处的状态
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class State {
    public final Type type;
    public final Data data;

    public State(Type type) {
        this(type, null);
    }

    public State(Type type, Data data) {
        this.type = type;
        this.data = data;
    }

    public enum Type {
        /** 待输入 */
        Input_Waiting,
        /** 滑行输入中 */
        SlippingInput,

        /** 输入组件光标定位：含移动光标和文本选择 */
        InputTarget_Cursor_Locating,

        /** 输入候选字选择中 */
        ChoosingInputCandidate,
    }

    public interface Data {}
}