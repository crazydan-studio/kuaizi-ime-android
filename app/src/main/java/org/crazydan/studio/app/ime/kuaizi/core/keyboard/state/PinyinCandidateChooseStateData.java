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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.EmojiWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.State;

/**
 * {@link State.Type#InputCandidate_Choose_Doing} 的状态数据
 * <p/>
 * 仅针对拼音候选字
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-10
 */
public class PinyinCandidateChooseStateData extends PinyinCandidateFilterStateData<InputWord> {
    private final List<InputWord> candidates;
    private final List<PinyinWord.Spell> spells;

    private List<InputWord> cachedFilterCandidates;

    public PinyinCandidateChooseStateData(CharInput input, List<InputWord> candidates, int pageSize) {
        super(input, pageSize);

        this.candidates = candidates;
        this.cachedFilterCandidates = candidates;

        this.spells = candidates.stream()
                                .filter((w) -> w instanceof PinyinWord)
                                .map((w) -> ((PinyinWord) w).spell)
                                .filter((spell) -> spell.id != null)
                                .sorted(this::compareSpell)
                                .distinct()
                                .collect(Collectors.toList());
    }

    @Override
    public List<InputWord> getPagingData() {
        return this.cachedFilterCandidates;
    }

    public List<InputWord> getCandidates() {
        return this.candidates;
    }

    public List<PinyinWord.Spell> getSpells() {
        return this.spells;
    }

    @Override
    public boolean updateFilter(PinyinWord.Filter filter) {
        if (super.updateFilter(filter)) {
            this.cachedFilterCandidates = filterCandidates(this.candidates);
            return true;
        }
        return false;
    }

    private List<InputWord> filterCandidates(List<InputWord> candidates) {
        if (this.filter.isEmpty()) {
            return candidates;
        }

        int pageSize = getPageSize();
        // Note：存在过滤条件时，仅过滤匹配的拼音字（不含表情），
        // 并合并高频字和其他字，高频字不再单独占用首页
        List<InputWord> firstPageData = CollectionUtils.subList(candidates, 0, pageSize)
                                                       .stream()
                                                       .filter(this::matched)
                                                       .filter(Objects::nonNull)
                                                       .collect(Collectors.toList());
        List<InputWord> restPageData = CollectionUtils.subList(candidates, pageSize, candidates.size())
                                                      .stream()
                                                      .filter(this::matched)
                                                      .collect(Collectors.toList());
        restPageData.removeAll(firstPageData);

        int totalPageDataSize = firstPageData.size() + restPageData.size();
        if (totalPageDataSize == 0) {
            return new ArrayList<>();
        }
        // 首页为空，则返回剩余数据
        else if (firstPageData.isEmpty()) {
            return restPageData;
        }
        // 高频字不单独占用一页
        else {
            firstPageData.addAll(restPageData);
            return firstPageData;
        }
    }

    private boolean matched(InputWord word) {
        // 表情符号不做匹配
        if (word instanceof EmojiWord) {
            return false;
        }
        // 占位用的，不做过滤
        else if (word == null) {
            return true;
        }

        return this.filter.matched(word);
    }
}
