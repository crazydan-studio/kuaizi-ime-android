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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin.state;

import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin.State;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-10
 */
public class ChoosingInputCandidateData implements State.Data {
    /** 数据总量 */
    private final int dataSize;

    /** 分页开始序号 */
    private int pageStart;
    /** 分页大小 */
    private final int pageSize;

    public ChoosingInputCandidateData(int dataSize, int pageSize) {
        this.dataSize = dataSize;
        this.pageSize = pageSize;
    }

    public int getPageStart() {
        return this.pageStart;
    }

    /** 下一页 */
    public void nextPage() {
        int start = this.pageStart + this.pageSize;
        if (start < this.dataSize) {
            this.pageStart = start;
        }
    }

    /** 上一页 */
    public void prevPage() {
        int start = this.pageStart - this.pageSize;
        this.pageStart = Math.max(start, 0);
    }
}
