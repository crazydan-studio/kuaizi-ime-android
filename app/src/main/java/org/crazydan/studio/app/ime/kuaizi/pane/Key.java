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

import android.util.LruCache;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewData;

/**
 * {@link Keyboard} 上的按键
 * <p/>
 * 注意，其为只读模型，不可对其进行变更，从而确保其实例可被缓存
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public abstract class Key implements RecyclerViewData {
    /** 按键的输入值，代表字符按键的实际输入字符，其与{@link #label 显示字符}可能并不相等 */
    public final String value;
    /** 按键上显示的文字内容 */
    public final String label;

    /** 按键上显示的图标资源 id */
    public final Integer icon;
    /** 按键的配色，始终不为 null */
    public final Color color;

    protected Key(Builder<?, ?> builder) {
        this.value = builder.value;
        this.label = builder.label;

        this.icon = builder.icon;
        this.color = builder.color;
    }

    /** 禁用指定的 {@link Key} */
    public static Key disable(Key key) {
        return new Disabled(key);
    }

    /** 判断指定的 {@link Key} 是否已被禁用 */
    public static boolean disabled(Key key) {
        return key instanceof Disabled;
    }

    /** 设置为禁用 */
    public <K extends Key> K setDisabled(boolean disabled) {
        return (K) this;
    }

    /** 设置按键上显示的文字内容 */
    public <K extends Key> K setLabel(String label) {
        this.label = label;
        return (K) this;
    }

    /** 设置按键上显示的图标资源 id */
    public <K extends Key> K setIcon(Integer icon) {
        this.icon = icon;
        return (K) this;
    }

    /** 设置按键配色 */
    public <K extends Key> K setColor(Color color) {
        this.color = color == null ? Color.none() : color;
        return (K) this;
    }

    @Override
    public String toString() {
        return this.label + "(" + this.value + ")";
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
        return Objects.equals(this.value, that.value)
               && Objects.equals(this.label, that.label)
               && Objects.equals(this.icon, that.icon)
               && Objects.equals(this.color.fg, that.color.fg)
               && Objects.equals(this.color.bg, that.color.bg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.label, this.icon, this.color.fg, this.color.bg);
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

    /**
     * 已被禁用的 {@link Key}
     * <p/>
     * 被禁用的按键采用对象封装模式，从而确保原始按键能够被缓存
     */
    private static class Disabled extends Key {
        private static final Builder<?, ?> builder = new Builder<Builder, Key>(0) {
            @Override
            protected Key doBuild() {
                return null;
            }
        };

        final Key source;

        Disabled(Key source) {
            super(builder);

            this.source = source;
        }

        @Override
        public boolean isSameWith(Object o) {
            return false;
        }
    }

    /**
     * {@link Key} 构建器，用于以调用链形式配置按键属性，并支持缓存按键，从而避免反复创建相同的 {@link Key}
     * <p/>
     * 注意，构建器本身需采用单例模式，并且在 {@link #build()} 时，以其 {@link #hashCode()} 作为按键缓存的唯一索引，
     * 因此，构建器不是线程安全的
     */
    protected static abstract class Builder<B extends Builder<B, K>, K extends Key> {
        private final LruCache<Integer, K> cache;

        protected String value;
        private String label;

        private Integer icon;
        private Color color = Color.none();

        /**
         * @param cacheSize
         *         可缓存的 {@link  Key} 数量
         */
        protected Builder(int cacheSize) {
            this.cache = cacheSize > 0 ? new LruCache<>(cacheSize) : null;
        }

        // ===================== Start: 构建函数 ===================

        abstract protected K doBuild();

        /** 根据当前构建配置创建 {@link Key} 实例 */
        public K build() {
            int hash = hashCode();

            K key = this.cache != null ? this.cache.get(hash) : null;
            if (key == null) {
                key = doBuild();

                if (this.cache != null) {
                    this.cache.put(hash, key);
                }
            }

            reset();

            return key;
        }

        /** 通过构建器的 hash 值作为按键缓存的索引 */
        @Override
        public int hashCode() {
            // Note: 对于 disabled 的按键，仅缓存其原始按键，其对象本身为临时创建的
            return Objects.hash(this.value, this.label, this.icon, //
                                this.color.fg, this.color.bg);
        }

        /** 为便于构建器作为单例复用，必须在 {@link #build()} 返回之前，重置所有的按键配置 */
        protected void reset() {
            this.value = null;
            this.label = null;

            this.icon = null;
            this.color = Color.none();
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 按键配置 ===================

        /** 创建与指定 {@link Key} 相同的按键，并可继续按需修改其他配置 */
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

        // ===================== End: 按键配置 ===================
    }
}
