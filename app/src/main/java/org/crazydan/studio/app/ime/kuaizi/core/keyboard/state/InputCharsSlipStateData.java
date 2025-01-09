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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.State;

/**
 * {@link State.Type#InputChars_Slip_Doing} 的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-15
 */
public class InputCharsSlipStateData implements State.Data {
    private Key level0Key;
    private Key level1Key;
    private Key level2Key;

    private Map<Integer, List<String>> level2NextChars;

    public Key getLevel0Key() {
        return this.level0Key;
    }

    public void setLevel0Key(Key level0Key) {
        this.level0Key = level0Key;
    }

    public Key getLevel1Key() {
        return this.level1Key;
    }

    public void setLevel1Key(Key level1Key) {
        this.level1Key = level1Key;
    }

    public Key getLevel2Key() {
        return this.level2Key;
    }

    public void setLevel2Key(Key level2Key) {
        this.level2Key = level2Key;
    }

    public Map<Integer, List<String>> getLevel2NextChars() {
        return this.level2NextChars;
    }

    public void setLevel2NextChars(Collection<String> level2NextChars) {
        this.level2NextChars = new HashMap<>();

        for (String text : level2NextChars) {
            this.level2NextChars.computeIfAbsent(text.length(), (k) -> new ArrayList<>()).add(text);
        }
    }
}
