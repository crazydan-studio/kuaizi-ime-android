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

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;

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

    /**
     * 确保仅在按键布局发生变化时才触发
     * RecyclerViewGestureDetector#hasChangedViewData
     * 中的事件视图的重置
     */
    @Override
    public boolean isSameWith(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        XPadKey that = (XPadKey) o;
        return Objects.equals(this.zone_0_key, that.zone_0_key)
               && Arrays.equals(this.zone_1_keys, that.zone_1_keys)
               && Arrays.deepEquals(this.zone_2_keys, that.zone_2_keys);
    }

    @NonNull
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

        // ===================== Start: 按键配置 ===================

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

        // ===================== End: 按键配置 ===================
    }
}