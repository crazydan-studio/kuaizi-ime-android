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

package org.crazydan.studio.app.ime.kuaizi.common;

import java.util.function.Consumer;

import android.util.LruCache;

/**
 * 不可变对象
 * <p/>
 * 不可变对象的所有属性都需定义为 <code>final</code>
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-05
 */
public abstract class Immutable {
    /**
     * 当前不可变对象的 Hash 值，其将作为 {@link #hashCode()} 的返回值，
     * 并且用于判断不可变对象是否{@link #equals(Object) 相等}
     * <p/>
     * 该值与其{@link Builder 构造器}的 {@link Builder#hashCode()} 相等，
     * 因为二者的属性值是全部相等的
     */
    private final int objHash;

    protected Immutable(Builder<?> builder) {
        this.objHash = builder.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Immutable that = (Immutable) o;
        return this.objHash == that.objHash;
    }

    @Override
    public int hashCode() {
        return this.objHash;
    }

    /**
     * 不可变对象的构建器
     * <p/>
     * 该构建器以单例模式暂存不可变对象的属性值，并在 {@link #build()}
     * 后重置以实现复用，因此，其不是线程安全的
     */
    public abstract static class Builder<O extends Immutable> {

        /** 在入参函数中添加构建配置，再根据其配置创建不可变对象 */
        public static <O extends Immutable, B extends Builder<O>> O build(B b, Consumer<B> c) {
            // Note: 构建器为单例复用，在使用前必须重置
            b.reset();

            c.accept(b);

            return b.build();
        }

        /**
         * 创建指定 {@link Immutable} 的副本
         * <p/>
         * 注意，在 {@link Builder} 中需实现 {@link Builder#doCopy}
         */
        public static <O extends Immutable, B extends Builder<O>> O copy(B b, O o, Consumer<B> c) {
            return build(b, (_b) -> {
                b.doCopy(o);
                c.accept(b);
            });
        }

        /** 为便于构建器作为单例复用，必须在 {@link #build} 返回之前，重置所有的构建配置 */
        protected abstract void reset();

        /**
         * 在不可变对象的构造函数中根据构建器的配置为只读属性赋初始值
         * <p/>
         * 注意，相关属性的值转换和处理操作需在传给不可变对象的构造函数之前完成，
         * 以使其构造函数内仅需直接引用构建器的属性值，确保二者的 {@link #hashCode()} 是相同的
         */
        protected abstract O build();

        /** 从指定 {@link Immutable} 中复制初始的构建配置 */
        protected void doCopy(O source) {}
    }

    /**
     * 可缓存已构建的 {@link Immutable} 对象的 {@link Builder}
     * <p/>
     * 注意，构建器在 {@link #build} 时，将以其 {@link #hashCode()} 作为按键缓存的唯一索引
     */
    public abstract static class CachableBuilder<O extends Immutable> extends Builder<O> {
        final LruCache<Integer, O> cache;

        private boolean _notCache_;

        /**
         * @param cacheSize
         *         可缓存的 {@link  Immutable} 对象的数量。若小于或等于 0，则表示禁用缓存
         */
        protected CachableBuilder(int cacheSize) {
            this.cache = cacheSize > 0 ? new LruCache<>(cacheSize) : null;
        }

        /**
         * 通过 {@link #doBuild()} 构建 {@link Immutable} 并缓存
         * <p/>
         * 若与当前构建器的 {@link #hashCode()} 相同的不可变对象已缓存，
         * 则直接从缓存中取出该不可变对象
         */
        @Override
        protected O build() {
            int hash = hashCode();

            boolean cachable = this.cache != null && !this._notCache_;
            this._notCache_ = false;

            O obj = cachable ? this.cache.get(hash) : null;
            if (obj == null) {
                obj = doBuild();

                if (cachable) {
                    this.cache.put(hash, obj);
                }
            }
            return obj;
        }

        /**
         * 在不可变对象的构造函数中根据构建器的配置为只读属性赋初始值
         * <p/>
         * 注意，相关属性的值转换和处理操作需在传给不可变对象的构造函数之前完成，
         * 以使其构造函数内仅需直接引用构建器的属性值，确保二者的 {@link #hashCode()} 是相同的
         */
        protected abstract O doBuild();

        /** 清空缓存 */
        public void clear() {
            if (this.cache != null) {
                this.cache.evictAll();
            }
        }

        /** 禁用对当前构建对象的缓存：放在调用链的尾部 */
        public void notCache() {
            this._notCache_ = true;
        }
    }
}
