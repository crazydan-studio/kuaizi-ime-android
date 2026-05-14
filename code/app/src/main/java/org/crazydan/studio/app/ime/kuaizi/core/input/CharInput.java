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

package org.crazydan.studio.app.ime.kuaizi.core.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.EmojiWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.MathOpKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.SymbolKey;

/**
 * 字符{@link Input 输入}
 * <p/>
 * 记录任意可见输入字符
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class CharInput extends Input {
    private List<Key> keys = new ArrayList<>();

    private CharInput pair;
    private ConfirmableWord word = new ConfirmableWord(null);

    public static CharInput from(List<Key> keys) {
        CharInput input = new CharInput();

        input.replaceAllKeys(keys);

        return input;
    }

    public static CharInput from(Key... keys) {
        return from(Arrays.asList(keys));
    }

    /** 指定输入是否为{@link CharInput 字符输入}且仅包含{@link CtrlKey.Type#Space 空格按键} */
    public static boolean isSpace(Input input) {
        return input instanceof CharInput //
               && ((CharInput) input).hasAllKeys(CtrlKey.Type.Space::match);
    }

    /** 指定输入是否为{@link CharInput 字符输入}且为有效拼音输入 */
    public static boolean isPinyin(Input input) {
        return input instanceof CharInput //
               && ((CharInput) input).getWord() instanceof PinyinWord;
    }

    /** 是指定输入是否为{@link CharInput 字符输入}且仅包含标点符号 */
    public static boolean isSymbol(Input input) {
        return input instanceof CharInput //
               && ((CharInput) input).hasAllKeys((key) -> CharKey.Type.Symbol.match(key) //
                                                          || key instanceof SymbolKey //
                                                          || (key instanceof MathOpKey //
                                                              && !MathOpKey.Type.isOperator(key)));
    }

    /** 指定输入是否为{@link CharInput 字符输入}且仅包含英文字母、数字或二者的组合输入 */
    public static boolean isLatin(Input input) {
        return input instanceof CharInput
               && !isPinyin(input)
               && ((CharInput) input).hasAllKeys((key) -> CharKey.Type.Number.match(key) //
                                                          || CharKey.Type.Alphabet.match(key) //
                                                          // 确保括号与数字和运算符之间都有空格
                                                          || MathOpKey.Type.Brackets.match(key) //
                                                          || MathOpKey.Type.Dot.match(key));
    }

    /**
     * 指定输入是否为{@link CharInput 字符输入}且仅包含数字和唯一的{@link MathOpKey.Type#Dot 小数点}
     * <p/>
     * 注意，百分号需为独立的输入，不应该在数字输入中出现
     */
    public static boolean isNumber(Input input) {
        return input instanceof CharInput //
               && ((CharInput) input).hasAllKeys((key) -> CharKey.Type.Number.match(key) //
                                                          || MathOpKey.Type.Dot.match(key));
    }

    /** 指定输入是否为{@link CharInput 字符输入}且仅包含表情符号 */
    public static boolean isEmoji(Input input) {
        return input instanceof CharInput //
               && (((CharInput) input).getWord() instanceof EmojiWord //
                   || ((CharInput) input).hasAllKeys((key) -> CharKey.Type.Emoji.match(key) //
                                                              || InputWordKey.isEmoji(key)));
    }

    /** 指定输入是否为{@link CharInput 字符输入}且仅包含{@link MathOpKey.Type#isOperator 数学运算符} */
    public static boolean isMathOp(Input input) {
        return input instanceof CharInput //
               && ((CharInput) input).hasAllKeys(MathOpKey.Type::isOperator);
    }

    /** 是否仅使用{@link CharInput#getWord() 输入字}的{@link PinyinWord#spell 读音}作为指定输入的{@link Input#getText() 文本内容} */
    public static boolean useWordSpellAsText(Input input, Option option) {
        return option.wordSpellUsedMode == PinyinWord.SpellUsedMode.replacing //
               && input instanceof CharInput //
               && ((CharInput) input).getWord() instanceof PinyinWord;
    }

    // ======================= Start: 输入按键相关处理 ======================

    @Override
    protected boolean isEmpty() {
        return this.keys.isEmpty();
    }

    @Override
    public Input copy() {
        CharInput copied = new CharInput();

        copied.setWord(getWord());
        if (isWordConfirmed()) {
            copied.confirmWord();
        }

        copied.replaceAllKeys(this.keys);

        return copied;
    }

    /** 获取输入按键列表（只读） */
    public List<Key> getKeys() {
        return Collections.unmodifiableList(this.keys);
    }

    /** 获取按键数量 */
    public int countKeys() {
        return this.keys.size();
    }

    /** 获取第一个按键 */
    public Key getFirstKey() {
        return this.keys.isEmpty() ? null : this.keys.get(0);
    }

    /** 获取最后一个按键 */
    public Key getLastKey() {
        return this.keys.isEmpty() ? null : this.keys.get(this.keys.size() - 1);
    }

    /** 是否包含指定条件的按键 */
    public boolean hasAnyKey(Predicate<Key> filter) {
        for (Key k : this.keys) {
            if (filter.test(k)) {
                return true;
            }
        }
        return false;
    }

    /** 是否所有的按键均满足指定的条件 */
    public boolean hasAllKeys(Predicate<Key> filter) {
        for (Key key : this.keys) {
            if (!filter.test(key)) {
                return false;
            }
        }
        return !isEmpty();
    }

    /** 追加输入按键 */
    public void appendKey(Key key) {
        this.keys.add(key);
    }

    /** 丢弃最后一个按键 */
    public void dropLastKey() {
        if (!this.keys.isEmpty()) {
            this.keys.remove(this.keys.size() - 1);
        }
    }

    /** 丢弃所有按键 */
    public void dropAllKeys() {
        this.keys.clear();
    }

    /** 替换所有按键为指定按键 */
    protected void replaceAllKeys(List<Key> keys) {
        this.keys = new ArrayList<>(keys);
    }

    /**
     * 替换指定{@link CharKey.Level 按键级别}之后的按键
     * <p/>
     * 先删除指定级别之后的按键，再追加新按键
     */
    public void replaceKeysAfterLevel(CharKey.Level level, Key newKey) {
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

    /**
     * 替换 指定按键的 最后一次的 添加位置为新的按键
     * <p/>
     * {@link Key} 的实例是只读的，因此，相同的按键会被复用
     */
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

    /** 获取输入按键的字符列表：{@link Key#value} 为 null 的将被排除 */
    public List<String> getKeyChars() {
        return this.keys.stream().map((key) -> key.value).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /** 获取合并后的 {@link #getKeyChars()} 字符串 */
    public String getJoinedKeyChars() {
        return String.join("", getKeyChars());
    }

    @Override
    public StringBuilder getText(Option option) {
        StringBuilder sb = new StringBuilder();

        InputWord word = getWord();
        if (word == null) {
            getKeyChars().forEach(sb::append);
            return sb;
        }

        String value = word.value;
        if (word instanceof PinyinWord && option != null) {
            String spell = ((PinyinWord) word).spell.value;
            String variant = ((PinyinWord) word).variant;

            if (option.wordVariantUsed && variant != null) {
                value = variant;
            }
            if (option.wordSpellUsedMode != null && spell != null) {
                switch (option.wordSpellUsedMode) {
                    case following:
                        value = String.format("%s(%s)", value, spell);
                        break;
                    case replacing:
                        value = spell;
                        break;
                }
            }
        }

        if (value != null) {
            sb.append(value);
        }
        return sb;
    }

    // ======================= End: 输入按键相关处理 ======================

    // ======================= Start: 配对符号输入 ======================

    public CharInput getPair() {
        return this.pair;
    }

    public void setPair(CharInput pair) {
        if (pair == null) {
            clearPair();
            return;
        }

        this.pair = pair;
        // 同步设置对端关联
        this.pair.pair = this;
    }

    public void clearPair() {
        if (this.pair != null) {
            this.pair.pair = null;
        }
        this.pair = null;
    }

    public boolean hasPair() {
        return this.pair != null;
    }

    // ======================= End: 配对符号输入 ======================

    // ======================= Start: 输入字 ======================

    /** 获取{@link InputWord 输入字} */
    public InputWord getWord() {
        return this.word.value;
    }

    /** 设置{@link InputWord 输入字} */
    public void setWord(InputWord word) {
        this.word = new CharInput.ConfirmableWord(word);
    }

    /** 是否有{@link #getWord() 输入字} */
    public boolean hasWord() {
        return getWord() != null;
    }

    /** 确认{@link #getWord() 输入字} */
    public void confirmWord() {
        this.word.confirmed = true;
    }

    /** {@link #getWord() 输入字}是否已确认，已确认的字不会被词组预测等替换 */
    public boolean isWordConfirmed() {
        return this.word.confirmed;
    }

    // ======================= End: 输入字 ======================

    // ======================= Start: 拼音输入转换 ======================

    /** 是否为拼音 平/翘舌 开头 */
    public boolean is_Pinyin_SCZ_Starting() {
        List<String> chars = getKeyChars();
        if (chars.isEmpty()) {
            return false;
        }

        String ch = chars.get(0);
        return ch.startsWith("s") || ch.startsWith("c") || ch.startsWith("z");
    }

    /** 切换拼音输入的平翘舌 */
    public void toggle_Pinyin_SCZ_Starting() {
        CharKey key = (CharKey) getFirstKey();
        String keyText = key.value;

        if (keyText.startsWith("sh") || keyText.startsWith("ch") || keyText.startsWith("zh")) {
            keyText = keyText.charAt(0) + keyText.substring(2);
        } else if (keyText.startsWith("s") || keyText.startsWith("c") || keyText.startsWith("z")) {
            keyText = keyText.charAt(0) + "h" + keyText.substring(1);
        }

        replaceCharKeyText(key, 0, keyText);
    }

    /** 是否为拼音 前/后鼻韵 */
    public boolean is_Pinyin_NG_Ending() {
        List<String> chars = getKeyChars();
        if (chars.isEmpty()) {
            return false;
        }

        String ch = chars.get(chars.size() - 1);
        return ch.endsWith("eng") || ch.endsWith("ing") || ch.endsWith("ang") //
               || ch.endsWith("en") || ch.endsWith("in") || ch.endsWith("an");
    }

    /** 切换拼音输入的前/后鼻韵 */
    public void toggle_Pinyin_NG_Ending() {
        CharKey key = (CharKey) getLastKey();
        String keyText = key.value;

        if (keyText.endsWith("eng") || keyText.endsWith("ing") || keyText.endsWith("ang")) {
            keyText = keyText.substring(0, keyText.length() - 1);
        } else if (keyText.endsWith("en") || keyText.endsWith("in") || keyText.endsWith("an")) {
            keyText += "g";
        }

        replaceCharKeyText(key, this.keys.size() - 1, keyText);
    }

    /** 是否为拼音 n/l 开头 */
    public boolean is_Pinyin_NL_Starting() {
        List<String> chars = getKeyChars();
        if (chars.isEmpty()) {
            return false;
        }

        String ch = chars.get(0);
        return ch.startsWith("n") || ch.startsWith("l");
    }

    /** 切换拼音输入的 n/l */
    public void toggle_Pinyin_NL_Starting() {
        CharKey key = (CharKey) getFirstKey();
        String keyValue = key.value;

        if (keyValue.startsWith("n")) {
            keyValue = "l" + keyValue.substring(1);
        } else if (keyValue.startsWith("l")) {
            keyValue = "n" + keyValue.substring(1);
        }

        replaceCharKeyText(key, 0, keyValue);
    }

    protected void replaceCharKeyText(CharKey key, int keyIndex, String keyValue) {
        CharKey newKey = CharKey.build((b) -> b.from(key).value(keyValue).label(keyValue));

        this.keys.set(keyIndex, newKey);
    }

    // ======================= End: 拼音输入转换 ======================

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CharInput that = (CharInput) o;
        return this.keys.equals(that.keys) && Objects.equals(getWord(), that.getWord());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.keys, getWord());
    }

    /** 已确认的字不会被词组预测等替换 */
    public static class ConfirmableWord {
        private final InputWord value;
        private boolean confirmed;

        public ConfirmableWord(InputWord value) {
            this.value = value;
        }
    }
}
