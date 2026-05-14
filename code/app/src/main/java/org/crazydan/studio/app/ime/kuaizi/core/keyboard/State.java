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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard;

import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;

/**
 * {@link Keyboard 键盘}所处的状态
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class State {
    public final Type type;
    private final Data data;

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

    /** 用于自动做 {@link #data} 的类型转换 */
    public <T extends Data> T data() {
        return (T) this.data;
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

        /** 编辑器编辑中：光标移动 或 内容选择 */
        Editor_Edit_Doing,
    }

    public interface Data {}
}
