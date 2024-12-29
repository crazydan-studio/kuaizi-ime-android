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

package org.crazydan.studio.app.ime.kuaizi.pane;

import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewData;

/**
 * {@link Keyboard} 上的按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public abstract class Key implements RecyclerViewData {
    private final String text;
    private String label;
    private Level level = Level.level_0;
    private Integer iconResId;

    private boolean disabled;
    private Color color = Color.none();

    public Key() {
        this(null);
    }

    protected Key(String text) {
        this.text = text;
    }

    /** 是否为空格 */
    public boolean isSpace() {
        return false;
    }

    /** 是否为英文字母 */
    public boolean isAlphabet() {
        return false;
    }

    /** 是否为数字 */
    public boolean isNumber() {
        return false;
    }

    /** 是否为标点符号 */
    public boolean isSymbol() {
        return false;
    }

    /** 是否为表情符号 */
    public boolean isEmoji() {
        return false;
    }

    /** 是否为数学运算符 */
    public boolean isMathOp() {
        return false;
    }

    /** 是否已禁用 */
    public boolean isDisabled() {
        return this.disabled;
    }

    /** 设置为禁用 */
    public <K extends Key> K setDisabled(boolean disabled) {
        this.disabled = disabled;
        return (K) this;
    }

    /**
     * 获取按键对应的文本字符
     * <p/>
     * 若其不对应任何字符，则返回 <code>null</code>
     */
    public String getText() {
        return this.text;
    }

    /** 按键上显示的文字内容 */
    public String getLabel() {
        return this.label;
    }

    /** 设置按键上显示的文字内容 */
    public <K extends Key> K setLabel(String label) {
        this.label = label;
        return (K) this;
    }

    /** 获取按键所处级别 */
    public Level getLevel() {
        return this.level;
    }

    /** 设置按键所处级别 */
    public <K extends Key> K setLevel(Level level) {
        this.level = level;
        return (K) this;
    }

    /** 按键上显示的图标资源 id */
    public Integer getIconResId() {
        return this.iconResId;
    }

    /** 设置按键上显示的图标资源 id */
    public <K extends Key> K setIconResId(Integer iconResId) {
        this.iconResId = iconResId;
        return (K) this;
    }

    /** 获取按键配色 */
    public Color getColor() {
        return this.color;
    }

    /** 设置按键配色 */
    public <K extends Key> K setColor(Color color) {
        this.color = color == null ? Color.none() : color;
        return (K) this;
    }

    @Override
    public String toString() {
        return this.label + "(" + getText() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Key that = (Key) o;
        return Objects.equals(this.getIconResId(), that.getIconResId())
               && Objects.equals(this.getLabel(),
                                 that.getLabel())
               && Objects.equals(this.getText(), that.getText())
               && Objects.equals(this.getLevel(), that.getLevel())
               && this.disabled == that.disabled
               && Objects.equals(this.color.fg, that.color.fg)
               && Objects.equals(this.color.bg, that.color.bg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getIconResId(),
                            this.getLabel(),
                            this.getText(),
                            this.getLevel(),
                            this.disabled,
                            this.color.fg,
                            this.color.bg);
    }

    /** 按键级别 */
    public enum Level {
        /**
         * 第 0 级：初始布局的按键。
         * 一个拼音的首字母均处于该级别（ch、sh、zh 独立成为第 0 级），
         * 如，huang 中的 h 为第 0 级
         */
        level_0,
        /**
         * 第 1 级：拼音滑屏的第一级后继字母按键。
         * 拼音第 0 级字母之后的第一个字母均处于该级别，
         * 如，huang 中的 u 为第 1 级
         */
        level_1,
        /**
         * 第 2 级：拼音滑屏的第二级后继字母按键。
         * 拼音第 0 级字母之后的剩余字母均处于该级别，
         * 如，huang 中的 uang 为第 2 级
         */
        level_2,
        /**
         * 末级：完整且无后继字母的拼音。
         * 如，ai, er, an, ang, m, n 等
         */
        level_final,
    }

    /** {@link Key} 配色 */
    public static class Color {
        /** 前景色资源 id */
        public final Integer fg;
        /** 背景色资源 id */
        public final Integer bg;

        private Color(Integer fg, Integer bg) {
            this.fg = fg;
            this.bg = bg;
        }

        public static Color create(Integer fg, Integer bg) {
            return new Color(fg, bg);
        }

        public static Color none() {
            return create(null, null);
        }
    }
}
