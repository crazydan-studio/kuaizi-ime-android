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

package org.crazydan.studio.app.ime.kuaizi.pane;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewData;
import org.crazydan.studio.app.ime.kuaizi.pane.input.word.EmojiWord;
import org.crazydan.studio.app.ime.kuaizi.pane.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.MathOpKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.SymbolKey;

/**
 * 输入对象
 * <p/>
 * 单次有用户输入的所有有效字符均记录在该对象中，
 * 用于识别拼音、表情、符号等用户输入
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public abstract class Input implements RecyclerViewData {
    private List<Key> keys = new ArrayList<>();

    private ConfirmableWord word = new ConfirmableWord(null);

    public static boolean isEmpty(Input input) {
        return input == null || input.isEmpty();
    }

    public static boolean isGap(Input input) {
        return input != null && input.isGap();
    }

    /** 创建副本 */
    public Input copy() {
        Input copied;
        try {
            copied = getClass().newInstance();
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

    /** 确认输入，一般用于包含 输入列表 的输入 */
    public void confirm() {}

    /** 是否为占位输入，也即，光标所在位置 */
    public boolean isGap() {
        return false;
    }

    /** 是否为空格输入 */
    public boolean isSpace() {
        return test(CtrlKey.Type.Space::match);
    }

    /** 是否为英文字母、数字或二者的组合输入 */
    public boolean isLatin() {
        return !isPinyin() && test((key) -> CharKey.Type.Number.match(key) //
                                            || CharKey.Type.Alphabet.match(key) //
                                            // 确保括号与数字和运算符之间都有空格
                                            || MathOpKey.Type.Brackets.match(key) //
                                            || MathOpKey.Type.Dot.match(key));
    }

    /** 是否为拼音输入 */
    public boolean isPinyin() {
        return getWord() instanceof PinyinWord;
    }

    /** 是否为标点符号 */
    public boolean isSymbol() {
        return test((key) -> CharKey.Type.Symbol.match(key) //
                             || key instanceof SymbolKey //
                             || MathOpKey.Type.isSymbol(key)) //
               || isSpace();
    }

    /** 是否为表情符号 */
    public boolean isEmoji() {
        return getWord() instanceof EmojiWord //
               || test((key) -> CharKey.Type.Emoji.match(key) //
                                || InputWordKey.isEmoji(key));
    }

    /** 是否为数学运算符 */
    public boolean isMathOp() {
        // 确保数学运算符前后都有空格
        return test((key) -> key instanceof MathOpKey //
                             && !(MathOpKey.Type.Percent.match(key)
                                  || MathOpKey.Type.Permill.match(key)
                                  || MathOpKey.Type.Permyriad.match(key)));
    }

    /** 是否为数学计算式 */
    public boolean isMathExpr() {
        return false;
    }

    /** 是否为空输入 */
    public boolean isEmpty() {
        return this.keys.isEmpty();
    }

    /** 获取输入按键列表 */
    public List<Key> getKeys() {
        return this.keys;
    }

    /** 获取第一个按键 */
    public Key getFirstKey() {
        return this.keys.isEmpty() ? null : this.keys.get(0);
    }

    /** 获取最后一个按键 */
    public Key getLastKey() {
        return this.keys.isEmpty() ? null : this.keys.get(this.keys.size() - 1);
    }

    /**
     * 是否包含与指定按键相同的按键
     * <p/>
     * 通过 {@link #isSameWith} 判断按键是否相同
     */
    public boolean hasSameKey(Key key) {
        for (Key k : this.keys) {
            if (k.isSameWith(key)) {
                return true;
            }
        }
        return false;
    }

    /** 追加输入按键 */
    public void appendKey(Key key) {
        this.keys.add(key);
    }

    /** 丢弃所有按键 */
    public void dropKeys() {
        this.keys.clear();
    }

    /** 丢弃最后一个按键 */
    public void dropLastKey() {
        if (!this.keys.isEmpty()) {
            this.keys.remove(this.keys.size() - 1);
        }
    }

    /**
     * 替换指定{@link CharKey.Level 按键级别}之后的按键
     * <p/>
     * 先删除指定级别之后的按键，再追加新按键
     */
    public void replaceKeyAfterLevel(CharKey.Level level, Key newKey) {
        boolean removing = false;

        Iterator<Key> it = this.keys.iterator();
        while (it.hasNext()) {
            Key oldKey = it.next();

            if (removing) {
                it.remove();
            }
            // 移除指定级别之后的按键：连续移除匹配之后的按键
            else {
                removing = oldKey instanceof CharKey //
                           && level == ((CharKey) oldKey).level;
            }
        }

        // 追加按键
        this.keys.add(newKey);
    }

    /** 替换指定按键的最近添加位置的按键 */
    public void replaceLatestKey(Key oldKey, Key newKey) {
        if (oldKey == null || newKey == null) {
            return;
        }

        int oldKeyIndex = this.keys.lastIndexOf(oldKey);
        if (oldKeyIndex >= 0) {
            this.keys.set(oldKeyIndex, newKey);
        }
    }

    /**
     * 替换最后一个按键
     * <p/>
     * 若其按键列表为空，则追加新按键
     */
    public void replaceLastKey(Key newKey) {
        dropLastKey();
        appendKey(newKey);
    }

    /** 获取输入字符列表 */
    public List<String> getChars() {
        return this.keys.stream().map((key) -> key.value).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /** 获取合并后的输入字符的字符串 */
    public String getJoinedChars() {
        return String.join("", getChars());
    }

    /** 获取输入文本内容 */
    public StringBuilder getText() {
        return getText(null);
    }

    /** 获取输入文本内容 */
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

    /** 输入文本内容是否只有{@link InputWord#getSpell() 字的读音} */
    public boolean isTextOnlyWordSpell(Option option) {
        return option.wordSpellUsedMode == PinyinWord.SpellUsedMode.replacing && hasWord() && getWord().hasSpell();
    }

    /** 是否有可输入字 */
    public boolean hasWord() {
        return getWord() != null;
    }

    /** 输入字是否已确认，已确认的字不会被词组预测等替换 */
    public boolean isWordConfirmed() {
        return this.word.confirmed;
    }

    /**
     * 获取已选择候选字
     *
     * @return 若为<code>null</code>，则表示未选择
     */
    public InputWord getWord() {
        return this.word.value;
    }

    public void markWordConfirmed() {
        this.word.confirmed = true;
    }

    public void setWord(InputWord word) {
        this.word = new ConfirmableWord(word);
    }

    protected void replaceKeys(List<Key> keys) {
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

        Input that = (Input) o;
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

        Input that = (Input) o;
        return this.keys.equals(that.keys) && Objects.equals(getWord(), that.getWord());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.keys, getWord());
    }

    private boolean test(Predicate<Key> predicate) {
        for (Key key : this.keys) {
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

    public static class Option {
        /** 采用何种读音使用模式 */
        public final PinyinWord.SpellUsedMode wordSpellUsedMode;
        /** 是否使用候选字变体 */
        public final boolean wordVariantUsed;

        public Option(PinyinWord.SpellUsedMode wordSpellUsedMode, boolean wordVariantUsed) {
            this.wordSpellUsedMode = wordSpellUsedMode;
            this.wordVariantUsed = wordVariantUsed;
        }
    }
}
