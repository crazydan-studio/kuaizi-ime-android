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

package org.crazydan.studio.app.ime.kuaizi.internal.key;

import java.util.Objects;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;

/**
 * 控制{@link Key 按键}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class CtrlKey extends BaseKey<CtrlKey> {
    private final Type type;

    public static CtrlKey noop() {
        return create(Type.NoOp);
    }

    public static CtrlKey create(Type type) {
        return new CtrlKey(type);
    }

    private CtrlKey(Type type) {
        this.type = type;
    }

    /** 按钮{@link Type 类型} */
    public Type getType() {
        return this.type;
    }

    public boolean isSpace() {
        return this.type == Type.Space;
    }

    public boolean isNoOp() {
        return this.type == Type.NoOp;
    }

    @Override
    public String getText() {
        switch (this.type) {
            case Space:
                return " ";
            case Enter:
                return "\n";
            case Math_Plus:
            case Math_Minus:
            case Math_Multiply:
            case Math_Divide:
            case Math_Equal:
                return " " + getLabel() + " ";
            case Math_Dot:
            case Math_Percent:
                return getLabel();
            default:
                return null;
        }
    }

    @Override
    public boolean isSameWith(Key<?> key) {
        if (!(key instanceof CtrlKey)) {
            return false;
        }

        CtrlKey that = (CtrlKey) key;
        return this.type == that.type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CtrlKey that = (CtrlKey) o;
        return this.type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.type);
    }

    @NonNull
    @Override
    public String toString() {
        return "CTRL - " + getType();
    }

    public enum Type {
        /** 占位按键 */
        NoOp,
        /** 空格 */
        Space,
        /** 回删 */
        Backspace,

        /** 提交输入列表 */
        CommitInputList,
        /** 丢弃当前输入 */
        DropInput,
        /** 确认当前输入 */
        ConfirmInput,
        /** 撤回输入 */
        RevokeInput,

        /** 在候选字状态下切换当前输入的平舌和翘舌 */
        Toggle_PinyinInputSpell_zcs_h,
        /** 在候选字状态下切换当前输入的前鼻韵和后鼻韵 */
        Toggle_PinyinInputSpell_ng,
        /** 在候选字状态下切换当前输入的 n/l */
        Toggle_PinyinInputSpell_nl,
        /** 在候选字状态下根据笔画过滤候选字：横 */
        Filter_PinyinInputCandidate_stroke_heng,
        /** 在候选字状态下根据笔画过滤候选字：竖 */
        Filter_PinyinInputCandidate_stroke_shu,
        /** 在候选字状态下根据笔画过滤候选字：撇 */
        Filter_PinyinInputCandidate_stroke_pie,
        /** 在候选字状态下根据笔画过滤候选字：捺 */
        Filter_PinyinInputCandidate_stroke_na,
        /** 在候选字状态下根据笔画过滤候选字：折 */
        Filter_PinyinInputCandidate_stroke_zhe,

        /** 输入光标定位按钮 */
        LocateInputCursor,
        /** 输入光标定位按钮 - 定位 */
        LocateInputCursor_Locator,
        /** 输入光标定位按钮 - 选择 */
        LocateInputCursor_Selector,

        /** 回车 */
        Enter,
        /** 退出当前键盘 */
        Exit,
        /** 复制 */
        Copy,
        /** 粘贴 */
        Paste,
        /** 剪切 */
        Cut,
        /** 撤销 */
        Undo,
        /** 重做 */
        Redo,

        /** 切换输入法 */
        SwitchIME,
        /** 切换左右手模式 */
        SwitchHandMode,
        /** 切换至拼音键盘 */
        SwitchToPinyinKeyboard,
        /** 切换至拉丁文键盘 */
        SwitchToLatinKeyboard,
        /** 切换至数学键盘 */
        SwitchToMathKeyboard,
        /** 切换至数字键盘 */
        SwitchToNumberKeyboard,

        /** 切换至标点符号键盘 */
        SwitchToSymbolKeyboard,
        /** 切换至表情键盘 */
        SwitchToEmojiKeyboard,

        /** 切换中英文的标点符号 */
        Toggle_Symbol_Locale_Zh_and_En,
        /** 切换表情符号分组 */
        Toggle_Emoji_Group,

        /** 数学 = */
        Math_Equal,
        /** 数学 + */
        Math_Plus,
        /** 数学 - */
        Math_Minus,
        /** 数学 × */
        Math_Multiply,
        /** 数学 ÷ */
        Math_Divide,
        /** 数学 % */
        Math_Percent,
        /** 数学 () */
        Math_Brackets,
        /** 数学 . */
        Math_Dot,
    }
}
