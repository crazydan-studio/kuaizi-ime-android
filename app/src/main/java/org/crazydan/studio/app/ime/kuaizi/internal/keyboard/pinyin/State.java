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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin;

import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.PinyinKeyboard;

/**
 * {@link PinyinKeyboard 汉语拼音键盘}的状态
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
        /** 初始 */
        Init,
        /** 待输入 */
        InputWaiting,
        /** 输入中 */
        Inputting,
        /** 输入组件光标定位 */
        LocatingInputTargetCursor,
        /** 输入组件光标已定位 */
        LocatingInputTargetCursorDone,
        /** 输入组件文本选择 */
        SelectingInputTargetText,
        /** 输入组件文本已选择 */
        SelectingInputTargetTextDone,
        /** 输入光标移动 */
        MovingInputListCursor,
        /** 输入候选字选择中 */
        ChoosingInputCandidate,
    }

    public interface Data {}
}
