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
        PinyinWord.Filter oldFilter = this.filter;
        this.filter = new PinyinWord.Filter(filter);

        if (!Objects.equals(oldFilter, this.filter)) {
            resetPageStart();
            return true;
        }
        return false;
    }
}
