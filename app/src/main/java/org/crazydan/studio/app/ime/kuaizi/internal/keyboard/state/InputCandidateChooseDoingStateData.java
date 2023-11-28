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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.EmojiInputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.State;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;

/**
 * {@link State.Type#InputCandidate_Choose_Doing} 的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-10
 */
public class InputCandidateChooseDoingStateData extends PagingStateData<InputWord> {
    private final CharInput target;
    private final List<InputWord> candidates;
    private final Map<String, Integer> strokes;

    private AdvanceFilter advanceFilter;
    private List<InputWord> cachedFilterCandidates;

    public InputCandidateChooseDoingStateData(CharInput target, List<InputWord> candidates, int pageSize) {
        super(pageSize);

        this.target = target;
        this.candidates = candidates;

        this.strokes = new HashMap<>();
        this.cachedFilterCandidates = candidates;

        this.advanceFilter = new AdvanceFilter();
    }

    public CharInput getTarget() {
        return this.target;
    }

    @Override
    public List<InputWord> getPagingData() {
        return this.cachedFilterCandidates;
    }

    public Map<String, Integer> getStrokes() {
        return this.strokes;
    }

    /** @return 若添加笔画且其数量大于 0 则返回 <code>true</code>，否则返回 <code>false</code> */
    public boolean addStroke(String stroke, int increment) {
        if (stroke == null) {
            return false;
        }

        int count = this.strokes.getOrDefault(stroke, 0);
        count += increment;

        boolean changed = true;
        if (count > 0) {
            this.strokes.put(stroke, count);
        } else {
            changed = this.strokes.remove(stroke) != null;
        }

        if (changed) {
            this.cachedFilterCandidates = filterCandidates(this.candidates);
        }
        return changed;
    }

    public void newAdvanceFilter(List<PinyinInputWord.Spell> spells, List<PinyinInputWord.Radical> radicals) {
        AdvanceFilter oldFilter = this.advanceFilter;
        this.advanceFilter = new AdvanceFilter(spells, radicals);

        if (!oldFilter.equals(this.advanceFilter)) {
            this.cachedFilterCandidates = filterCandidates(this.candidates);
        }
    }

    public AdvanceFilter getAdvanceFilter() {
        return this.advanceFilter;
    }

    private List<InputWord> filterCandidates(List<InputWord> candidates) {
        if (this.strokes.isEmpty() && this.advanceFilter.isEmpty()) {
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
        if (word instanceof EmojiInputWord) {
            return false;
        }
        // 占位用的，不做过滤
        else if (word == null) {
            return true;
        }

        if (!this.advanceFilter.matched(word)) {
            return false;
        }

        Map<String, Integer> wordStrokes = ((PinyinInputWord) word).getStrokes();
        if (wordStrokes.isEmpty() || this.strokes.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Integer> entry : this.strokes.entrySet()) {
            String stroke = entry.getKey();
            int expectedCount = entry.getValue();
            int actualCount = wordStrokes.getOrDefault(stroke, 0);

            if (actualCount < expectedCount) {
                return false;
            }
        }
        return true;
    }

    public static class AdvanceFilter {
        public final List<PinyinInputWord.Spell> spells;
        public final List<PinyinInputWord.Radical> radicals;

        public AdvanceFilter() {
            this(new ArrayList<>(), new ArrayList<>());
        }

        public AdvanceFilter(List<PinyinInputWord.Spell> spells, List<PinyinInputWord.Radical> radicals) {
            this.spells = spells;
            this.radicals = radicals;
        }

        public boolean isEmpty() {
            return this.spells.isEmpty() //
                   && this.radicals.isEmpty();
        }

        public boolean matched(InputWord word) {
            if (!(word instanceof PinyinInputWord)) {
                return false;
            }

            return (this.spells.isEmpty() //
                    || this.spells.contains(((PinyinInputWord) word).getSpell())) //
                   && (this.radicals.isEmpty() //
                       || this.radicals.contains(((PinyinInputWord) word).getRadical()));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            AdvanceFilter that = (AdvanceFilter) o;
            return this.spells.equals(that.spells) && this.radicals.equals(that.radicals);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.spells, this.radicals);
        }
    }
}
