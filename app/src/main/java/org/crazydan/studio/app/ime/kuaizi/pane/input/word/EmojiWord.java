/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.pane.input.word;

import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;

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
