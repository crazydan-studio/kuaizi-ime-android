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

package org.crazydan.studio.app.ime.kuaizi.core.input.word;

import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;

/**
 * 表情字
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-26
 */
public class EmojiWord extends InputWord {
    private final static Builder builder = new Builder();

    /** 构建 {@link EmojiWord} */
    public static EmojiWord build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    protected EmojiWord(Builder builder) {
        super(builder);
    }

    @Override
    public String toString() {
        return this.value;
    }

    /** {@link EmojiWord} 的构建器 */
    public static class Builder extends InputWord.Builder<Builder, EmojiWord> {

        // ===================== Start: 构建函数 ===================

        @Override
        protected EmojiWord build() {
            return new EmojiWord(this);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        // ===================== End: 构建配置 ===================
    }
}
