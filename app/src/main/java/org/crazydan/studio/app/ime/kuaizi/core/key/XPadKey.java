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

package org.crazydan.studio.app.ime.kuaizi.core.key;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.core.Key;

/**
 * 将 X 型输入键盘作为普通按键，以便于与其他普通按键进行统一布局
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-04
 */
public class XPadKey extends Key {
    private final static Builder builder = new Builder();

    /** #0 区按键 */
    public final Key zone_0_key;
    /** #1 区按键 */
    public final Key[] zone_1_keys;
    /** #2 区按键 */
    public final Key[][][] zone_2_keys;

    /** 构建 {@link XPadKey} */
    public static XPadKey build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    protected XPadKey(Builder builder) {
        super(builder);

        this.zone_0_key = builder.zone_0_key;
        this.zone_1_keys = builder.zone_1_keys;
        this.zone_2_keys = builder.zone_2_keys;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /** {@link XPadKey} 的构建器 */
    public static class Builder extends Key.Builder<Builder, XPadKey> {
        public static final Consumer<Builder> noop = (b) -> {};

        private Key zone_0_key;
        private Key[] zone_1_keys;
        private Key[][][] zone_2_keys;

        Builder() {
            super(0);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected XPadKey doBuild() {
            return new XPadKey(this);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(),
                                this.zone_0_key,
                                Arrays.hashCode(this.zone_1_keys),
                                Arrays.deepHashCode(this.zone_2_keys));
        }

        @Override
        protected void reset() {
            super.reset();

            this.zone_0_key = null;
            this.zone_1_keys = null;
            this.zone_2_keys = null;
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see XPadKey#zone_0_key */
        public Builder zone_0_key(Key zone_0_key) {
            this.zone_0_key = zone_0_key;
            return this;
        }

        /** @see XPadKey#zone_1_keys */
        public Builder zone_1_keys(Key[] zone_1_keys) {
            this.zone_1_keys = zone_1_keys;
            return this;
        }

        /** @see XPadKey#zone_2_keys */
        public Builder zone_2_keys(Key[][][] zone_2_keys) {
            this.zone_2_keys = zone_2_keys;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}