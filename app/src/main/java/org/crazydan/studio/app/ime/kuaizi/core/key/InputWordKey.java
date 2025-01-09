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

import java.util.Objects;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.EmojiWord;

/**
 * {@link InputWord 输入候选字}按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-09
 */
public class InputWordKey extends Key {
    private final static Builder builder = new Builder();

    public final InputWord word;

    /** 构建 {@link InputWordKey} */
    public static InputWordKey build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    /** 构建携带指定 {@link InputWord} 的 {@link InputWordKey} */
    public static InputWordKey build(InputWord word) {
        return build((b) -> b.word(word));
    }

    public static boolean isEmoji(Key key) {
        return key instanceof InputWordKey && ((InputWordKey) key).word instanceof EmojiWord;
    }

    protected InputWordKey(Builder builder) {
        super(builder);

        this.word = builder.word;
    }

    @Override
    public String toString() {
        return this.word.toString();
    }

    /** {@link InputWordKey} 的构建器 */
    public static class Builder extends Key.Builder<Builder, InputWordKey> {
        public static final Consumer<Builder> noop = (b) -> {};

        private InputWord word;

        Builder() {
            super(0);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected InputWordKey doBuild() {
            value(this.word.value);
            label(value());

            return new InputWordKey(this);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.word);
        }

        @Override
        protected void reset() {
            super.reset();

            this.word = null;
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see InputWordKey#word */
        public Builder word(InputWord word) {
            this.word = word;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
