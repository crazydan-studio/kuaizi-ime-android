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

package org.crazydan.studio.app.ime.kuaizi.internal.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public abstract class BaseInput implements Input {
    private final List<Key<?>> keys = new ArrayList<>();

    private InputWord word;

    @Override
    public boolean isLatin() {
        if (isPinyin()) {
            return false;
        }

        for (Key<?> key : this.keys) {
            if (!key.isLatin()) {
                return false;
            }
        }
        return !isEmpty();
    }

    @Override
    public boolean isPinyin() {
        return false;
    }

    @Override
    public boolean isSymbol() {
        for (Key<?> key : this.keys) {
            if (!key.isSymbol()) {
                return false;
            }
        }
        return !isEmpty();
    }

    @Override
    public boolean isEmoji() {
        for (Key<?> key : this.keys) {
            if (!key.isEmoji()) {
                return false;
            }
        }
        return !isEmpty();
    }

    @Override
    public boolean isEmpty() {
        return this.keys.isEmpty();
    }

    @Override
    public List<Key<?>> getKeys() {
        return this.keys;
    }

    @Override
    public Key<?> getFirstKey() {
        return this.keys.isEmpty() ? null : this.keys.get(0);
    }

    @Override
    public Key<?> getLastKey() {
        return this.keys.isEmpty() ? null : this.keys.get(this.keys.size() - 1);
    }

    @Override
    public void appendKey(Key<?> key) {
        this.keys.add(key);
    }

    @Override
    public void replaceLatestKey(Key<?> oldKey, Key<?> newKey) {
        if (oldKey == null || newKey == null) {
            return;
        }

        int oldKeyIndex = this.keys.lastIndexOf(oldKey);
        if (oldKeyIndex >= 0) {
            this.keys.set(oldKeyIndex, newKey);
        }
    }

    @Override
    public List<String> getChars() {
        return this.keys.stream().map(Key::getText).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public StringBuilder getText() {
        StringBuilder sb = new StringBuilder();

        if (this.word != null) {
            sb.append(this.word.getValue());
        } else {
            getChars().forEach(sb::append);
        }
        return sb;
    }

    @Override
    public boolean hasWord() {
        return this.word != null;
    }

    @Override
    public InputWord getWord() {
        return this.word;
    }

    public void setWord(InputWord candidate) {
        this.word = candidate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseInput that = (BaseInput) o;
        return this.keys.equals(that.keys) && Objects.equals(this.word, that.word);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.keys, this.word);
    }
}
