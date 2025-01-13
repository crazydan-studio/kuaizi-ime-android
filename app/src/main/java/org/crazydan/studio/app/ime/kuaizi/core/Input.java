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

package org.crazydan.studio.app.ime.kuaizi.core;

import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;

/**
 * 用户输入
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public abstract class Input {

    /** 指定输入是否为 null 或{@link #isEmpty() 空白} */
    public static boolean isEmpty(Input input) {
        return input == null || input.isEmpty();
    }

    /** 是否为空白输入 */
    protected abstract boolean isEmpty();

    /** 确认输入，一般用于包含 输入列表 的输入 */
    public void confirm() {}

    /** 创建副本 */
    public abstract Input copy();

    /**
     * 获取输入的文本内容
     *
     * @param option
     *         可能为 null
     * @return 不返回 null
     */
    public abstract StringBuilder getText(Option option);

    /**
     * 获取输入的文本内容
     *
     * @return 不返回 null
     */
    public StringBuilder getText() {return getText(null);}

    @Override
    public String toString() {return getText().toString();}

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
