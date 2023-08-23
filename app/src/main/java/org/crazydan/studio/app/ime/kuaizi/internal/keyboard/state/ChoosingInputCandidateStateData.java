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

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.State;

/**
 * {@link State.Type#Choosing_Input_Candidate}的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-10
 */
public class ChoosingInputCandidateStateData implements State.Data {
    private final List<InputWord> candidates;

    /** 数据总量 */
    private final int dataSize;
    /** 分页大小 */
    private final int pageSize;
    /** 分页开始序号 */
    private int pageStart;

    public ChoosingInputCandidateStateData(
            List<InputWord> candidates, int pageSize
    ) {
        this.candidates = candidates;

        this.dataSize = candidates.size();
        this.pageSize = pageSize;
    }

    public List<InputWord> getCandidates() {
        return this.candidates;
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
}
