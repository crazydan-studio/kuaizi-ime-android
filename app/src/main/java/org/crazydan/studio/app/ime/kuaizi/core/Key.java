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

package org.crazydan.studio.app.ime.kuaizi.core;

import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.common.Immutable;

/**
 * {@link Keyboard} 上的按键
 * <p/>
 * 注意，其为只读模型，不可对其进行变更，从而确保其实例可被缓存
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public abstract class Key extends Immutable {
    /** 按键的输入值，代表字符按键的实际输入字符，其与{@link #label 显示字符}可能并不相等 */
    public final String value;
    /** 按键上显示的文字内容 */
    public final String label;

    /** 按键上显示的图标资源 id */
    public final Integer icon;
    /** 按键的配色，始终不为 null */
    public final Color color;

    /** 按键是否已被禁用 */
    public final boolean disabled;

    protected Key(Builder<?, ?> builder) {
        super(builder);

        this.value = builder.value;
        this.label = builder.label;

        this.icon = builder.icon;
        this.color = builder.color;

        this.disabled = builder.disabled;
    }

    @Override
    public String toString() {
        return this.label + "(" + this.value + ")";
    }

    /** {@link Key} 构建器，可缓存已构建的 {@link Key}，以避免反复创建相同的 {@link Key} */
    protected static abstract class Builder< //
            B extends Builder<B, K>, //
            K extends Key //
            > extends Immutable.CachableBuilder<K> {
        private String value;
        private String label;

        private Integer icon;
        private Color color = Color.none();

        private boolean disabled;

        protected Builder(int cacheSize) {
            super(cacheSize);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected void reset() {
            this.value = null;
            this.label = null;

            this.icon = null;
            this.color = Color.none();

            this.disabled = false;
        }

        /** 通过构建器的 hash 值作为按键缓存的索引 */
        @Override
        public int hashCode() {
            return Objects.hash(this.value, this.label, this.disabled, this.icon, this.color.fg, this.color.bg);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 按键配置 ===================

        /**
         * 创建与指定 {@link Key} 相同的按键，并可继续按需修改其他配置
         * <p/>
         * 注意，{@link Key#disabled} 状态不做复制，需按需单独处理
         */
        public B from(K key) {
            return value(key.value).label(key.label).icon(key.icon).color(key.color);
        }

        /** @see Key#value */
        public B value(String value) {
            this.value = value;
            return (B) this;
        }

        public String value() {return this.value;}

        /** @see Key#label */
        public B label(String label) {
            this.label = label;
            return (B) this;
        }

        public String label() {return this.label;}

        /** @see Key#icon */
        public B icon(Integer icon) {
            this.icon = icon;
            return (B) this;
        }

        /**
         * @param color
         *         若为 null，则赋值为 {@link Color#none()}
         * @see Key#color
         */
        public B color(Color color) {
            this.color = color != null ? color : Color.none();
            return (B) this;
        }

        /** 设置按键是否被禁用 */
        public B disabled(boolean disabled) {
            this.disabled = disabled;
            return (B) this;
        }

        // ===================== End: 按键配置 ===================
    }

    /** {@link Key} 配色 */
    public static class Color {
        /** 前景色资源 id */
        public final Integer fg;
        /** 背景色资源 id */
        public final Integer bg;

        Color(Integer fg, Integer bg) {
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

    /** {@link Key} 图标 */
    public static class Icon {
        /** 右手模式的图标资源 id */
        public final Integer right;
        /** 左手模式的图标资源 id */
        public final Integer left;

        Icon(Integer resId) {
            this.right = resId;
            this.left = resId;
        }

        Icon(Integer right, Integer left) {
            this.right = right;
            this.left = left;
        }
    }

    /** {@link Key} 样式 */
    public static class Style {
        public final Icon icon;
        public final Color color;

        Style(Color color) {
            this(new Icon(null, null), color);
        }

        Style(Icon icon, Color color) {
            this.icon = icon;
            this.color = color;
        }

        public static Style withColor(int fg, int bg) {
            return withColor(Color.create(fg, bg));
        }

        public static Style withColor(Color color) {
            return new Style(color);
        }

        public static Style withIcon(int right, int left, int bg) {
            return new Style(new Icon(right, left), Color.create(null, bg));
        }

        public static Style withIcon(int resId, int bg) {
            return new Style(new Icon(resId), Color.create(null, bg));
        }
    }
}
