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

package org.crazydan.studio.app.ime.kuaizi.pane.input;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public abstract class BaseInput<T extends BaseInput<?>> implements Input<T> {
    private List<Key<?>> keys = new ArrayList<>();

    private ConfirmableWord word = new ConfirmableWord(null);

    @Override
    public T copy() {
        T copied;
        try {
            copied = (T) getClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        copied.setWord(getWord());
        if (isWordConfirmed()) {
            copied.markWordConfirmed();
        }

        getKeys().forEach(copied::appendKey);

        return copied;
    }

    @Override
    public void confirm() {}

    @Override
    public boolean isGap() {
        return false;
    }

    @Override
    public boolean isSpace() {
        return test(Key::isSpace);
    }

    @Override
    public boolean isLatin() {
        return !isPinyin() && test(Key::isLatin);
    }

    @Override
    public boolean isPinyin() {
        return getWord() instanceof PinyinWord;
    }

    @Override
    public boolean isSymbol() {
        return test(Key::isSymbol);
    }

    @Override
    public boolean isEmoji() {
        return getWord() instanceof EmojiWord || test(Key::isEmoji);
    }

    @Override
    public boolean isMathOperator() {
        return test(Key::isMathOperator);
    }

    @Override
    public boolean isMathExpr() {
        return false;
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
    public boolean hasSameKey(Key<?> key) {
        for (Key<?> k : this.keys) {
            if (k.isSameWith(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendKey(Key<?> key) {
        this.keys.add(key);
    }

    @Override
    public void dropKeys() {
        this.keys.clear();
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
    public void replaceLastKey(Key<?> newKey) {
        dropLastKey();
        appendKey(newKey);
    }

    @Override
    public List<String> getChars() {
        return this.keys.stream().map(Key::getText).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public String getJoinedChars() {
        return String.join("", getChars());
    }

    @Override
    public StringBuilder getText() {
        return getText(null);
    }

    @Override
    public StringBuilder getText(Option option) {
        StringBuilder sb = new StringBuilder();

        InputWord word = getWord();
        if (word != null) {
            String value = word.getValue();
            String spell = word.getSpell().value;
            String variant = word.getVariant();

            if (option != null && option.wordVariantUsed && variant != null) {
                value = variant;
            }
            if (option != null && option.wordSpellUsedMode != null && spell != null) {
                switch (option.wordSpellUsedMode) {
                    case following:
                        value = String.format("%s(%s)", value, spell);
                        break;
                    case replacing:
                        value = spell;
                        break;
                }
            }

            if (value != null) {
                sb.append(value);
            }
        } else {
            getChars().forEach(sb::append);
        }
        return sb;
    }

    @Override
    public boolean isTextOnlyWordSpell(Option option) {
        return option.wordSpellUsedMode == InputWord.SpellUsedMode.replacing && hasWord() && getWord().hasSpell();
    }

    @Override
    public boolean hasWord() {
        return getWord() != null;
    }

    @Override
    public InputWord getWord() {
        return this.word.value;
    }

    @Override
    public boolean isWordConfirmed() {
        return this.word.confirmed;
    }

    public void markWordConfirmed() {
        this.word.confirmed = true;
    }

    public void setWord(InputWord word) {
        this.word = new ConfirmableWord(word);
    }

    protected void replaceKeys(List<Key<?>> keys) {
        this.keys = new ArrayList<>(keys);
    }

    @NonNull
    @Override
    public String toString() {
        return getText().toString();
    }

    @Override
    public boolean isSameWith(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        } else if (this == o) {
            return true;
        }

        BaseInput<?> that = (BaseInput<?>) o;
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

        BaseInput<?> that = (BaseInput<?>) o;
        return this.keys.equals(that.keys) && Objects.equals(getWord(), that.getWord());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.keys, getWord());
    }

    private boolean test(Predicate<Key<?>> predicate) {
        for (Key<?> key : this.keys) {
            if (!predicate.test(key)) {
                return false;
            }
        }
        return !isEmpty();
    }

    /** 已确认的字不会被词组预测等替换 */
    private static class ConfirmableWord {
        private final InputWord value;
        private boolean confirmed;

        public ConfirmableWord(InputWord value) {
            this.value = value;
        }
    }
}
