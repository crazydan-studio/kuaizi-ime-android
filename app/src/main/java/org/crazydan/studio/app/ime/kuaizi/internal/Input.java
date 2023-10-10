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

package org.crazydan.studio.app.ime.kuaizi.internal;

import java.util.List;

/**
 * {@link Keyboard 键盘}输入对象，包含零个或多个字符
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public interface Input<T extends Input<?>> extends ViewData {

    /** 创建副本 */
    T copy();

    /** 确认输入，一般用于包含 输入列表 的输入 */
    void confirm();

    /** 是否为占位输入 */
    boolean isGap();

    /** 是否为英文、数字或二者的组合输入 */
    boolean isLatin();

    /** 是否为拼音输入 */
    boolean isPinyin();

    /** 是否为标点符号 */
    boolean isSymbol();

    /** 是否为表情符号 */
    boolean isEmoji();

    /** 是否为数学计算符 */
    boolean isMathOperator();

    /** 是否为数学计算式 */
    boolean isMathExpr();

    /** 是否为空输入 */
    boolean isEmpty();

    /** 获取输入按键列表 */
    List<Key<?>> getKeys();

    /** 获取第一个按键 */
    Key<?> getFirstKey();

    /** 获取最后一个按键 */
    Key<?> getLastKey();

    /**
     * 是否包含与指定按键相同的按键
     * <p/>
     * 通过 {@link #isSameWith} 判断按键是否相同
     */
    boolean hasSameKey(Key<?> key);

    /** 追加输入按键 */
    void appendKey(Key<?> key);

    /** 丢弃最后一个按键 */
    void dropLastKey();

    /**
     * 替换指定{@link Key.Level 按键级别}之后的按键
     * <p/>
     * 先删除指定级别之后的按键，再追加新按键
     */
    void replaceKeyAfterLevel(Key.Level level, Key<?> newKey);

    /** 替换指定按键的最近添加位置的按键 */
    void replaceLatestKey(Key<?> oldKey, Key<?> newKey);

    /**
     * 替换最后一个按键
     * <p/>
     * 若其按键列表为空，则追加新按键
     */
    void replaceLastKey(Key<?> newKey);

    /** 获取输入字符列表 */
    List<String> getChars();

    /** 获取输入文本内容 */
    StringBuilder getText();

    /** 获取输入文本内容 */
    StringBuilder getText(Option option);

    /** 输入文本内容是否只有{@link InputWord#getNotation() 候选字的标注} */
    boolean isTextOnlyWordNotation(Option option);

    /** 是否有可输入字 */
    boolean hasWord();

    /**
     * 获取已选择候选字
     *
     * @return 若为<code>null</code>，则表示未选择
     */
    InputWord getWord();

    static boolean isEmpty(Input<?> input) {
        return input == null || input.isEmpty();
    }

    class Option {
        public final InputWord.NotationType wordNotationType;
        /** 是否使用候选字变体 */
        public final boolean wordVariantUsed;

        public Option(InputWord.NotationType wordNotationType, boolean wordVariantUsed) {
            this.wordNotationType = wordNotationType;
            this.wordVariantUsed = wordVariantUsed;
        }
    }
}
