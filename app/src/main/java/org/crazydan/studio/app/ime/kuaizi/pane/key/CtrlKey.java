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
import java.util.function.Consumer;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;

/**
 * 控制按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class CtrlKey extends TypedKey<CtrlKey.Type> {
    private final static Builder builder = new Builder();

    public final Option<?> option;

    /** 构建 {@link CtrlKey} */
    public static CtrlKey build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    /** 构建指定类型的 {@link CtrlKey} */
    public static CtrlKey build(Type type) {
        return build((b) -> b.type(type));
    }

    protected CtrlKey(Builder builder) {
        super(builder);

        this.option = builder.option;
    }

    public <O extends Option<?>> O option() {
        return (O) this.option;
    }

    @Override
    public boolean isSameWith(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        } else if (this == o) {
            return true;
        }

        CtrlKey that = (CtrlKey) o;
        return this.type == that.type //
               && Objects.equals(this.value, that.value) //
               && Objects.equals(this.option, that.option);
    }

    @NonNull
    @Override
    public String toString() {
        return this.type + (this.label != null ? "(" + this.label + ")" : "");
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
        ;

        /** 判断指定的 {@link Key} 是否为当前类型的 {@link CtrlKey} */
        public boolean match(Key key) {
            return key instanceof CtrlKey && ((CtrlKey) key).type == this;
        }
    }

    /** 拼音转换模式 */
    public enum PinyinToggleMode {
        /** z/c/s 与 zh/ch/sh 的转换 */
        zcs_start,
        /** n 与 l 的转换 */
        nl_start,
        /** ing/eng/ang 与 in/en/an 等的转换 */
        ng_end,
    }

    /** 输入候选字提交模式 */
    public enum InputWordCommitMode {
        /** 仅拼音 */
        only_pinyin("仅拼音"),
        /** 携带拼音 */
        with_pinyin("带拼音"),
        /** 简体 转换为 繁体 */
        simple_to_trad("简➙繁"),
        /** 繁体 转换为 简体 */
        trad_to_simple("繁➙简"),
        ;

        public final String label;

        InputWordCommitMode(String label) {this.label = label;}
    }

    /** 在控制按键上附加的选项信息 */
    public static class Option<T> {
        public final T value;

        public Option(T value) {this.value = value;}

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

    /** {@link CtrlKey} 的构建器 */
    public static class Builder extends TypedKey.Builder<Builder, CtrlKey, CtrlKey.Type> {
        public static final Consumer<Builder> noop = (b) -> {};

        private Option<?> option;

        Builder() {
            super(30);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected CtrlKey doBuild() {
            switch (type()) {
                case Enter: {
                    label("回车");
                    break;
                }
                case Space: {
                    label("空格");
                    break;
                }
                case Backspace: {
                    label("回删");
                    break;
                }
            }

            switch (type()) {
                case Space: {
                    value(" ");
                    break;
                }
                case Enter: {
                    value("\n");
                    break;
                }
                default: {
                    value(label());
                }
            }

            return new CtrlKey(this);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.option);
        }

        @Override
        protected void reset() {
            super.reset();

            this.option = null;
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        @Override
        public Builder from(CtrlKey key) {
            return super.from(key).option(key.option);
        }

        /** @see CtrlKey#option */
        public Builder option(Option<?> option) {
            this.option = option;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
