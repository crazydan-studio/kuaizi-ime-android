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
import org.crazydan.studio.app.ime.kuaizi.internal.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.State;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;

/**
 * {@link State.Type#Input_Candidate_Choosing}的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-10
 */
public class ChoosingInputCandidateStateData extends PagingStateData<InputWord> {
    private final CharInput input;
    private final List<InputWord> candidates;
    private final Map<String, Integer> strokes;

    private List<InputWord> cachedFilterCandidates;

    public ChoosingInputCandidateStateData(CharInput input, List<InputWord> candidates, int pageSize) {
        super(pageSize);

        this.input = input;
        this.candidates = candidates;

        this.strokes = new HashMap<>();
        this.cachedFilterCandidates = candidates;
    }

    public CharInput getInput() {
        return this.input;
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

    private List<InputWord> filterCandidates(List<InputWord> candidates) {
        if (this.strokes.isEmpty()) {
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
        // 占位用，不做过滤
        if (!(word instanceof PinyinInputWord)) {
            return true;
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
}
