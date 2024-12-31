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

import java.util.Objects;
import java.util.function.Consumer;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.input.word.EmojiWord;

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
    public boolean isSameWith(Object key) {
        if (!(key instanceof InputWordKey)) {
            return false;
        }

        InputWordKey that = (InputWordKey) key;
        return Objects.equals(this.word, that.word);
    }

    @NonNull
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
