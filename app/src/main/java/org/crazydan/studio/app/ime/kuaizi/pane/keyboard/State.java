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

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard;

import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;

/**
 * {@link Keyboard 键盘}所处的状态
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class State {
    public final Type type;
    public final Data data;
    public final State previous;

    public State(Type type) {
        this(type, null, null);
    }

    public State(Type type, Data data) {
        this(type, data, null);
    }

    public State(Type type, State previous) {
        this(type, null, previous);
    }

    public State(Type type, Data data, State previous) {
        this.type = type;
        this.data = data;
        this.previous = previous;
    }

    public enum Type {
        /** 待输入：初始状态 */
        InputChars_Input_Wait_Doing,

        /** 滑屏输入中 */
        InputChars_Slip_Doing,
        /** 翻动输入中：通过在首字母按键上做翻动（快速滑出按键）触发翻动输入 */
        InputChars_Flip_Doing,
        /** X 型面板输入：主要针对拼音输入 */
        InputChars_XPad_Input_Doing,

        /** 输入列表 提交选项 选择中 */
        InputList_Commit_Option_Choose_Doing,

        /** 输入候选字选择中 */
        InputCandidate_Choose_Doing,
        /** 输入候选字高级过滤中 */
        InputCandidate_Advance_Filter_Doing,
    }

    public interface Data {}
}
