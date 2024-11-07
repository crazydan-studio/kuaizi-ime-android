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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.EmojiWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.State;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;

/**
 * {@link State.Type#InputCandidate_Choose_Doing} 的状态数据
 * <p/>
 * 仅针对拼音候选字
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-10
 */
public class CandidatePinyinWordChooseDoingStateData extends PagingStateData<InputWord> {
    private final CharInput target;
    private final List<InputWord> candidates;
    private final List<PinyinWord.Spell> spells;

    private PinyinWord.Filter filter;
    private List<InputWord> cachedFilterCandidates;

    public CandidatePinyinWordChooseDoingStateData(CharInput target, List<InputWord> candidates, int pageSize) {
        super(pageSize);

        this.target = target;
        this.candidates = candidates;

        this.cachedFilterCandidates = candidates;
        this.filter = new PinyinWord.Filter();

        this.spells = candidates.stream()
                                .filter((word) -> word instanceof PinyinWord)
                                .map(InputWord::getSpell)
                                .filter((spell) -> spell.id != null)
                                .sorted(Comparator.comparingInt(a -> Integer.parseInt(a.id)))
                                .distinct()
                                .collect(Collectors.toList());
    }

    public CharInput getTarget() {
        return this.target;
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

    public PinyinWord.Filter getFilter() {
        return new PinyinWord.Filter(this.filter);
    }

    public void setFilter(PinyinWord.Filter filter) {
        PinyinWord.Filter oldFilter = this.filter;
        this.filter = new PinyinWord.Filter(filter);

        if (!oldFilter.equals(this.filter)) {
            this.cachedFilterCandidates = filterCandidates(this.candidates);
        }
    }

    private List<InputWord> filterCandidates(List<InputWord> candidates) {
        if (this.filter.isEmpty()) {
            return candidates;
        }

        int pageSize = getPageSize();
        // Note：第一页为最佳候选字，单独进行过滤
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
        // 不够一页时，做合并
        else if (totalPageDataSize <= pageSize) {
            firstPageData.addAll(restPageData);

            return firstPageData;
        }

        // 首页和剩余数据在单独页显示
        CollectionUtils.fillToSize(firstPageData, null, pageSize).addAll(restPageData);

        return firstPageData;
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