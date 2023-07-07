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

    private CtrlKey(Type type, int iconResId) {
        this.type = type;
        this.iconResId = iconResId;
    }

    public static CtrlKey noop() {
        return new CtrlKey(Type.NoOp, -1);
    }

    public static CtrlKey space(int iconResId) {
        return new CtrlKey(Type.Space, iconResId);
    }

    public static CtrlKey backspace(int iconResId) {
        return new CtrlKey(Type.Backspace, iconResId);
    }

    public static CtrlKey locator(int iconResId) {
        return new CtrlKey(Type.Locator, iconResId);
    }

    public static CtrlKey enter(int iconResId) {
        return new CtrlKey(Type.Enter, iconResId);
    }

    public static CtrlKey switchIME(int iconResId) {
        return new CtrlKey(Type.SwitchIME, iconResId);
    }

    public static CtrlKey switchHandMode(int iconResId) {
        return new CtrlKey(Type.SwitchHandMode, iconResId);
    }

    public static CtrlKey switchToAlphanumericKeyboard(int iconResId) {
        return new CtrlKey(Type.SwitchToAlphanumericKeyboard, iconResId);
    }

    public static CtrlKey switchToPunctuationKeyboard(int iconResId) {
        return new CtrlKey(Type.SwitchToPunctuationKeyboard, iconResId);
    }

    /** 按钮{@link Type 类型} */
    public Type type() {
        return this.type;
    }

    /** 图形资源 id */
    public int iconResId() {
        return this.iconResId;
    }

    public enum Type {
        /** 占位按键 */
        NoOp,
        /** 空格 */
        Space,
        /** 向前删除 */
        Backspace,
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
