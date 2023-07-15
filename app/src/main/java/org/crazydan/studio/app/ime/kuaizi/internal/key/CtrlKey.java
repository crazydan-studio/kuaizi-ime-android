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
    private final int iconResId;
    private final String text;

    private CtrlKey(Type type, int iconResId) {
        this.type = type;
        this.iconResId = iconResId;
        this.text = null;
    }

    public CtrlKey(Type type, String text) {
        this.type = type;
        this.text = text;
        this.iconResId = -1;
    }

    public static CtrlKey noop() {
        return noop(null);
    }

    public static CtrlKey noop(String text) {
        return new CtrlKey(Type.NoOp, text);
    }

    public static CtrlKey create(Type type, int iconResId) {
        return new CtrlKey(type, iconResId);
    }

    public static CtrlKey create(Type type, String text) {
        return new CtrlKey(type, text);
    }

    /** 按钮{@link Type 类型} */
    public Type getType() {
        return this.type;
    }

    /** 图形资源 id */
    public int getIconResId() {
        return this.iconResId;
    }

    /** 显示的文本 */
    public String getText() {
        return this.text;
    }

    public boolean isSpace() {
        return this.type == Type.Space;
    }

    public boolean isNoOp() {
        return this.type == Type.NoOp;
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
        return this.iconResId == that.iconResId && this.type == that.type //
               && Objects.equals(this.text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.type, this.iconResId, this.text);
    }

    @NonNull
    @Override
    public String toString() {
        return "CtrlKey(" + getType() + ')';
    }

    public enum Type {
        /** 占位按键 */
        NoOp,
        /** 空格 */
        Space,
        /** 向前删除 */
        Backspace,
        /** 提交输入 */
        CommitInput,
        /** 在候选字状态下删除当前输入 */
        DropInput,
        /** 在候选字状态下切换当前输入的平舌和翘舌 */
        ToggleInputSpell_zcs_h,
        /** 在候选字状态下切换当前输入的前鼻韵和后鼻韵 */
        ToggleInputSpell_ng,
        /** 在候选字状态下切换当前输入的 n/l */
        ToggleInputSpell_nl,
        /** 切换输入法 */
        SwitchIME,
        /** 切换左右手模式 */
        SwitchHandMode,
        /** 定位按钮 */
        Locator,
        /** 回车 */
        Enter,
        /** 切换至字母数字键盘 */
        SwitchToAlphanumericKeyboard,
        /** 切换至标点符号键盘: 在拼音键盘中，先选中中文标点，在字母数字键盘时，先选中英文标点 */
        SwitchToPunctuationKeyboard,
    }
}
