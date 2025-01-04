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

package org.crazydan.studio.app.ime.kuaizi.core.key;

import java.util.Objects;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.dict.Symbol;

/**
 * {@link Symbol 标点符号}按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-29
 */
public class SymbolKey extends Key {
    private final static Builder builder = new Builder();

    public final Symbol symbol;

    /** 构建 {@link SymbolKey} */
    public static SymbolKey build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    /** 构建携带指定 {@link Symbol} 的 {@link SymbolKey} */
    public static SymbolKey build(Symbol symbol) {
        return build((b) -> b.symbol(symbol));
    }

    protected SymbolKey(Builder builder) {
        super(builder);

        this.symbol = builder.symbol;
    }

    @Override
    public String toString() {
        return this.symbol.toString();
    }

    /** {@link SymbolKey} 的构建器 */
    public static class Builder extends Key.Builder<Builder, SymbolKey> {
        public static final Consumer<Builder> noop = (b) -> {};

        private Symbol symbol;

        Builder() {
            super(0);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected SymbolKey doBuild() {
            value(this.symbol.value);
            label(value());

            return new SymbolKey(this);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.symbol);
        }

        @Override
        protected void reset() {
            super.reset();

            this.symbol = null;
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see SymbolKey#symbol */
        public Builder symbol(Symbol symbol) {
            this.symbol = symbol;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
