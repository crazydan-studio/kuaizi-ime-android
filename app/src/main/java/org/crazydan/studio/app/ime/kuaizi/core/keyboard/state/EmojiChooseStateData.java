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
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.State;
import org.crazydan.studio.app.ime.kuaizi.dict.Emojis;

/**
 * 表情选择 {@link State.Type#InputCandidate_Choose_Doing} 的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-26
 */
public class EmojiChooseStateData extends PagingStateData<InputWord> {
    private final Emojis emojis;

    private String group;

    public EmojiChooseStateData(CharInput input, Emojis emojis, int pageSize) {
        super(input, pageSize);

        this.emojis = emojis;
        this.group = getGroups().get(0);
    }

    @Override
    public List<InputWord> getPagingData() {
        return this.emojis.groups.getOrDefault(getGroup(), new ArrayList<>());
    }

    public List<String> getGroups() {
        return new ArrayList<>(this.emojis.groups.keySet());
    }

    public String getGroup() {
        return this.group;
    }

    public void setGroup(String group) {
        if (group != null) {
            this.group = group;
            resetPageStart();
        }
    }
}
