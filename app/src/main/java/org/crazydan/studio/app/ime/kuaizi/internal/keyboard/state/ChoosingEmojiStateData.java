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

import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.data.Emojis;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.State;

/**
 * {@link State.Type#Emoji_Choosing}的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-26
 */
public class ChoosingEmojiStateData extends PagingStateData<InputWord> {
    private final Emojis emojis;

    private String group = Emojis.GROUP_GENERAL;

    public ChoosingEmojiStateData(Emojis emojis, int pageSize) {
        super(pageSize);
        this.emojis = emojis;
    }

    public List<String> getGroups() {
        return new ArrayList<>(this.emojis.groups.keySet());
    }

    public String getGroup() {
        return this.group;
    }

    public void setGroup(String group) {
        this.group = group;

        resetPageStart();
    }

    public List<InputWord> getPagingData() {
        return this.emojis.groups.getOrDefault(getGroup(), new ArrayList<>());
    }
}
