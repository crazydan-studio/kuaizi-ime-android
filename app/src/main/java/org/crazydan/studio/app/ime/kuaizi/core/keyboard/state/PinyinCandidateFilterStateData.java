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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard.state;

import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;

/**
 * 支持过滤的拼音候选状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-22
 */
public abstract class PinyinCandidateFilterStateData<T> extends PagingStateData<T> {
    protected PinyinWord.Filter filter;

    public PinyinCandidateFilterStateData(CharInput input, int pageSize) {
        super(input, pageSize);

        this.filter = new PinyinWord.Filter();
    }

    /** @return 返回过滤器的副本，可被直接修改 */
    public PinyinWord.Filter getFilter() {
        return new PinyinWord.Filter(this.filter);
    }

    /** @return 若存在变更，则返回 true，否则，返回 false */
    public boolean updateFilter(PinyinWord.Filter filter) {
        return updateFilter(filter, true);
    }

    /** @return 若存在变更，则返回 true，否则，返回 false */
    public boolean updateFilter(PinyinWord.Filter filter, boolean resetPage) {
        PinyinWord.Filter oldFilter = this.filter;
        this.filter = new PinyinWord.Filter(filter);

        if (!Objects.equals(oldFilter, this.filter)) {
            if (resetPage) {
                resetPageStart();
            }
            return true;
        }
        return false;
    }

    /** 按声调比较两个拼音 */
    protected int compareSpell(PinyinWord.Spell a, PinyinWord.Spell b) {
        return getSpellToneOrder(a) - getSpellToneOrder(b);
    }

    /** 获取拼音的声调排序序号 */
    private int getSpellToneOrder(PinyinWord.Spell spell) {
        String[][] tonesArray = new String[][] {
                // Note: 按声调升序排列，轻声的忽略
                new String[] { "ā", "á", "ǎ", "à" },
                new String[] { "ō", "ó", "ǒ", "ò" },
                new String[] { "ē", "é", "ě", "è" },
                new String[] { "ê̄", "ế", "ê̌", "ề", "ê" },
                new String[] { "ī", "í", "ǐ", "ì" },
                new String[] { "ū", "ú", "ǔ", "ù" },
                new String[] { "ǖ", "ǘ", "ǚ", "ǜ" },
                new String[] { "ń", "ň", "ǹ" },
                new String[] { "m̄", "ḿ", "m̀" }
        };

        for (int i = 0; i < tonesArray.length; i++) {
            String[] tones = tonesArray[i];
            for (int j = 0; j < tones.length; j++) {
                String tone = tones[j];
                // 一个拼音内只存在一个带声调的字符
                if (spell.value.contains(tone)) {
                    return i * 10 + j;
                }
            }
        }
        // Note: 轻声放最后
        return 1000;
    }
}
