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

import org.crazydan.studio.app.ime.kuaizi.internal.data.CandidateFilters;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.State;

/**
 * {@link State.Type#InputCandidate_AdvanceFilter_Doing}的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-27
 */
public class InputCandidateAdvanceFilterDoingStateData extends PagingStateData<String> {
    private final CandidateFilters filters;

    public InputCandidateAdvanceFilterDoingStateData(CandidateFilters filters, int pageSize) {
        super(pageSize);

        this.filters = filters;
    }

    @Override
    public List<String> getPagingData() {
        return this.filters.radicals;
    }

    public List<String> getSpells() {
        return this.filters.spells;
    }
}