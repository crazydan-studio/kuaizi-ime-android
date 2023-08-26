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
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.State;

/**
 * {@link State.Type#Emoji_Choosing}的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-26
 */
public class ChoosingEmojiStateData implements State.Data {
    private final CharInput input;
    private final Emojis emojis;
    /** 分页大小 */
    private final int pageSize;

    private String group = Emojis.GROUP_GENERAL;

    /** 分页开始序号 */
    private int pageStart;

    public ChoosingEmojiStateData(CharInput input, Emojis emojis, int pageSize) {
        this.input = input;
        this.emojis = emojis;
        this.pageSize = pageSize;
    }

    public CharInput getInput() {
        return this.input;
    }

    public List<String> getGroups() {
        return new ArrayList<>(this.emojis.groups.keySet());
    }

    public String getGroup() {
        return this.group;
    }

    public void setGroup(String group) {
        this.pageStart = 0;
        this.group = group;
    }

    public List<InputWord> getData() {
        return this.emojis.groups.getOrDefault(getGroup(), new ArrayList<>());
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public int getPageStart() {
        return this.pageStart;
    }

    public int getDataSize() {
        return getData().size();
    }

    /**
     * 下一页
     *
     * @return 若有翻页，则返回 <code>true</code>
     */
    public boolean nextPage() {
        int start = this.pageStart + this.pageSize;

        if (start < getDataSize()) {
            this.pageStart = start;
        } else {
            // 进行轮播
            this.pageStart = 0;
        }
        return true;
    }

    /**
     * 上一页
     *
     * @return 若有翻页，则返回 <code>true</code>
     */
    public boolean prevPage() {
        int start = this.pageStart - this.pageSize;

        if (start < 0) {
            int total = getDataSize();
            int left = total % this.pageSize;
            // 翻到最后一页
            if (left > 0) {
                this.pageStart = total - left;
            } else {
                this.pageStart = total - this.pageSize;
            }
        } else {
            this.pageStart = start;
        }
        return true;
    }
}
