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

import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.State;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.Symbol;

/**
 * {@link State.Type#SymbolEmoji_Choosing}的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-24
 */
public class ChoosingSymbolEmojiStateData implements State.Data {
    private Symbol[] symbols;

    /** 分页大小 */
    private final int pageSize;
    /** 分页开始序号 */
    private int pageStart;

    public ChoosingSymbolEmojiStateData(int pageSize) {
        this.pageSize = pageSize;
    }

    public Symbol[] getSymbols() {
        return this.symbols;
    }

    public void setSymbols(Symbol[] symbols) {
        this.symbols = symbols;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public int getPageStart() {
        return this.pageStart;
    }

    public int getDataSize() {
        return this.symbols.length;
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
