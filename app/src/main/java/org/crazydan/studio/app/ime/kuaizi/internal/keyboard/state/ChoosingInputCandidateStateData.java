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
import java.util.List;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.State;

/**
 * {@link State.Type#Input_Candidate_Choosing}的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-10
 */
public class ChoosingInputCandidateStateData implements State.Data {
    private final CharInput input;
    private final List<InputWord> candidates;
    private final List<String> strokes;

    /** 分页大小 */
    private final int pageSize;

    /** 数据总量 */
    private int dataSize;
    /** 分页开始序号 */
    private int pageStart;

    public ChoosingInputCandidateStateData(CharInput input, List<InputWord> candidates, int pageSize) {
        this.input = input;
        this.candidates = candidates;
        this.strokes = new ArrayList<>();

        this.pageSize = pageSize;
        this.dataSize = candidates.size();
    }

    public CharInput getInput() {
        return this.input;
    }

    public List<InputWord> getCandidates() {
        int totalSize = this.candidates.size();
        if (!this.strokes.isEmpty() && totalSize > this.pageSize) {
            // 第一页为最佳候选字，不做过滤
            List<InputWord> results = new ArrayList<>(this.candidates.subList(0, this.pageSize));
            List<InputWord> filtered = this.candidates.subList(this.pageSize, totalSize)
                                                      .stream()
                                                      .filter(this::hasStrokes)
                                                      .collect(Collectors.toList());

            results.addAll(filtered);

            this.dataSize = results.size();
            return results;
        }

        this.dataSize = totalSize;
        return this.candidates;
    }

    public void addStroke(String stroke) {
        if (stroke != null) {
            this.strokes.add(stroke);
        }
    }

    public int getPageStart() {
        return this.pageStart;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    /**
     * 下一页
     *
     * @return 若有翻页，则返回 <code>true</code>
     */
    public boolean nextPage() {
        int start = this.pageStart + this.pageSize;

        if (start < this.dataSize) {
            this.pageStart = start;
            return true;
        }
        return false;
    }

    /**
     * 上一页
     *
     * @return 若有翻页，则返回 <code>true</code>
     */
    public boolean prevPage() {
        int start = this.pageStart - this.pageSize;
        this.pageStart = Math.max(start, 0);

        return start >= 0;
    }

    private boolean hasStrokes(InputWord word) {
        // 占位用，不做过滤
        if (word == null) {
            return true;
        }

        String strokeOrder = String.join("", this.strokes);
        return word instanceof PinyinInputWord && ((PinyinInputWord) word).getStrokeOrder().contains(strokeOrder);
    }
}
