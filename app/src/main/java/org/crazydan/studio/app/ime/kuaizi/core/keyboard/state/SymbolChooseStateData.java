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

import java.util.Arrays;
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.State;
import org.crazydan.studio.app.ime.kuaizi.dict.Symbol;
import org.crazydan.studio.app.ime.kuaizi.dict.SymbolGroup;

/**
 * 符号选择 {@link State.Type#InputCandidate_Choose_Doing} 的状态数据
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
