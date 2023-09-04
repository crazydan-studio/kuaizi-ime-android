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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
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
        return !isPinyin() && test(Key::isLatin);
    }

    @Override
    public boolean isPinyin() {
        return this.word instanceof PinyinInputWord;
    }

    @Override
    public boolean isSymbol() {
        return test(Key::isSymbol);
    }

    @Override
    public boolean isEmoji() {
        return this.word instanceof EmojiInputWord || test(Key::isEmoji);
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
    public void dropLastKey() {
        if (!this.keys.isEmpty()) {
            this.keys.remove(this.keys.size() - 1);
        }
    }

    @Override
    public void replaceKeyAfterLevel(Key.Level level, Key<?> newKey) {
        boolean removing = false;
        Iterator<Key<?>> it = this.keys.iterator();
        while (it.hasNext()) {
            Key<?> oldKey = it.next();

            if (removing) {
                it.remove();
            }
            // 移除指定级别之后的按键
            removing = removing || (level == oldKey.getLevel());
        }

        // 追加按键
        this.keys.add(newKey);
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

    public void setWord(InputWord word) {
        this.word = word;
    }

    @Override
    public boolean isSameWith(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        } else if (this == o) {
            return true;
        }

        BaseInput that = (BaseInput) o;
        return this.keys.equals(that.keys);
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

    private boolean test(Predicate<Key<?>> predicate) {
        for (Key<?> key : this.keys) {
            if (!predicate.test(key)) {
                return false;
            }
        }
        return !isEmpty();
    }
}
