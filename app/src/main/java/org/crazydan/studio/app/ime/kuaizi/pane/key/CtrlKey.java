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

package org.crazydan.studio.app.ime.kuaizi.pane.key;

import java.util.Objects;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.dict.SymbolGroup;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;

/**
 * 控制{@link Key 按键}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class CtrlKey extends Key {
    private final Type type;

    private Option<?> option;

    private CtrlKey(Type type) {
        this.type = type;
    }

    public static CtrlKey noop() {
        return create(CtrlKey.Type.NoOp);
    }

    public static CtrlKey create(Type type) {
        return new CtrlKey(type);
    }

    public static boolean isNoOp(Key key) {
        return is(key, CtrlKey.Type.NoOp);
    }

    public static boolean is(Key key, Type type) {
        return type != null && key instanceof CtrlKey && ((CtrlKey) key).getType() == type;
    }

    public static boolean isAny(Key key, Type... types) {
        for (Type type : types) {
            if (is(key, type)) {
                return true;
            }
        }
        return false;
    }

    /** 按钮{@link Type 类型} */
    public Type getType() {
        return this.type;
    }

    public Option<?> getOption() {
        return this.option;
    }

    public CtrlKey setOption(Option<?> option) {
        this.option = option;
        return this;
    }

    @Override
    public boolean isSpace() {
        return is(this, CtrlKey.Type.Space);
    }

    @Override
    public String getText() {
        switch (this.type) {
            case Space:
                return " ";
            case Enter:
                return "\n";
            default:
                return getLabel();
        }
    }

    @Override
    public String getLabel() {
        switch (this.type) {
            case Enter:
                return "回车";
            case Space:
                return "空格";
            case Backspace:
                return "回删";
            case Pinyin_End:
                return "结束";
        }
        return super.getLabel();
    }

    @Override
    public boolean isSameWith(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        } else if (this == o) {
            return true;
        }

        CtrlKey that = (CtrlKey) o;
        return this.type == that.type
               && Objects.equals(this.getText(), that.getText())
               && Objects.equals(this.getOption(), that.getOption());
    }

    @NonNull
    @Override
    public String toString() {
        return getType() + (getLabel() != null ? "(" + getLabel() + ")" : "");
    }

    public enum Type {
        /** 占位按键 */
        NoOp,
        /** 空格 */
        Space,
        /** 回删 */
        Backspace,

        /** 提交 输入列表 */
        Commit_InputList,
        /** 输入列表 提交选项 */
        Commit_InputList_Option,

        /** 丢弃当前输入 */
        DropInput,
        /** 确认当前输入 */
        ConfirmInput,
        /** 撤回输入 */
        RevokeInput,

        /** 拼音结束 */
        Pinyin_End,
        /** 在候选字状态下切换当前输入的拼音拼写 */
        Toggle_Pinyin_spell,
        /** 在候选字状态下的候选字高级过滤（根据部首、声调等过滤） */
        Filter_PinyinCandidate_advance,
        /** 在候选字状态下根据读音过滤候选字 */
        Filter_PinyinCandidate_by_Spell,
        /** 在候选字状态下根据部首过滤候选字 */
        Filter_PinyinCandidate_by_Radical,
        /** 确认候选字过滤条件 */
        Confirm_PinyinCandidate_Filter,

        /** 定位 目标编辑器 光标 */
        Editor_Cursor_Locator,
        /** 选择 目标编辑器 内容 */
        Editor_Range_Selector,
        /** 编辑 目标编辑器 */
        Edit_Editor,

        /** 回车 */
        Enter,
        /** 退回到前序状态或原键盘 */
        Exit,

        /** 切换输入法 */
        Switch_IME,
        /** 切换左右手模式 */
        Switch_HandMode,
        /** 切换键盘 */
        Switch_Keyboard,

        /** 切换表情符号分组 */
        Toggle_Emoji_Group,
        /** 切换标点符号分组 */
        Toggle_Symbol_Group,

        /** X 型输入的当前激活块 */
        XPad_Active_Block,
        /** X 型输入的字符按键 */
        XPad_Char_Key,
        /** X 型输入演示终止按键：仅用于发送演示终止消息 */
        XPad_Simulation_Terminated,
    }

    public static abstract class Option<T> {
        private final T value;

        protected Option(T value) {this.value = value;}

        public T value() {return this.value;}

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Option<?> that = (Option<?>) o;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.value);
        }
    }

    public static class CodeOption extends Option<String> {
        public final String text;

        public CodeOption(String value) {
            this(value, null);
        }

        public CodeOption(String value, String text) {
            super(value);
            this.text = text;
        }
    }

    public static class ValueOption extends Option<Object> {
        public ValueOption(Object value) {super(value);}
    }

    public static class KeyboardSwitchOption extends Option<Keyboard.Type> {
        public KeyboardSwitchOption(Keyboard.Type value) {
            super(value);
        }
    }

    public static class SymbolGroupToggleOption extends Option<SymbolGroup> {
        public SymbolGroupToggleOption(SymbolGroup value) {super(value);}
    }

    public static class PinyinSpellToggleOption extends Option<PinyinSpellToggleOption.Toggle> {
        public PinyinSpellToggleOption(Toggle value) {super(value);}

        public enum Toggle {
            zcs_h,
            nl,
            ng,
        }
    }

    public static class InputListCommitOption extends Option<InputListCommitOption.Option> {
        public InputListCommitOption(Option value) {
            super(value);
        }

        public enum Option {
            /** 仅拼音 */
            only_pinyin,
            /** 携带拼音 */
            with_pinyin,
            /** 简体 转换为 繁体 */
            switch_simple_to_trad,
            /** 繁体 转换为 简体 */
            switch_trad_to_simple,
        }
    }

    public static class EditorEditOption extends Option<EditorAction> {
        public EditorEditOption(EditorAction value) {
            super(value);
        }
    }
}
