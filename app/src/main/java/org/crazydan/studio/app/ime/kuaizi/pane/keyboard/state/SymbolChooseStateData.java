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

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state;

import java.util.Arrays;
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.dict.Symbol;
import org.crazydan.studio.app.ime.kuaizi.dict.SymbolGroup;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.State;

/**
 * {@link State.Type#Symbol_Choose_Doing} 状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-24
 */
public class SymbolChooseStateData extends PagingStateData<Symbol> {
    private final boolean onlyPair;
    private SymbolGroup group = SymbolGroup.han;

    public SymbolChooseStateData(CharInput input, int pageSize, boolean onlyPair) {
        super(input, pageSize);
        this.onlyPair = onlyPair;
    }

    @Override
    public List<Symbol> getPagingData() {
        return Arrays.asList(this.group.symbols);
    }

    public boolean isOnlyPair() {
        return this.onlyPair;
    }

    public SymbolGroup getGroup() {
        return this.group;
    }

    public void setGroup(SymbolGroup group) {
        if (group != null) {
            this.group = group;
            resetPageStart();
        }
    }
}
