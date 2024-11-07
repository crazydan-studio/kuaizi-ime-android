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

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.EmojiWord;

/**
 * {@link InputWord 输入候选字}按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-09
 */
public class InputWordKey extends BaseKey<InputWordKey> {
    private final InputWord word;

    private InputWordKey(InputWord word) {
        this.word = word;
    }

    public static InputWordKey create(InputWord word) {
        return new InputWordKey(word);
    }

    @Override
    public boolean isEmoji() {
        return getWord() instanceof EmojiWord;
    }

    @Override
    public String getLabel() {
        return getText();
    }

    @Override
    public String getText() {
        return getWord().getValue();
    }

    public InputWord getWord() {
        return this.word;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        InputWordKey that = (InputWordKey) o;
        return Objects.equals(this.word, that.word);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.word);
    }
}
