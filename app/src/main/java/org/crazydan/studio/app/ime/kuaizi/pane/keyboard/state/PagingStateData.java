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

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.State;

/**
 * 分页状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-28
 */
public abstract class PagingStateData<T> implements State.Data {
    /** 分页大小 */
    private final int pageSize;

    /** 分页开始序号 */
    private int pageStart;

    protected PagingStateData(int pageSize) {
        this.pageSize = pageSize;
    }

    /** 获取用于分页的全量数据 */
    public abstract List<T> getPagingData();

    /** 获取用于分页的数据总量 */
    public int getPagingDataSize() {
        return getPagingData().size();
    }

    /** 获取分页大小：单页的数据量 */
    public int getPageSize() {
        return this.pageSize;
    }

    /** 获取当前分页的数据起始序号 */
    public int getPageStart() {
        correctPageStart();

        return this.pageStart;
    }

    public void resetPageStart() {
        this.pageStart = 0;
    }

    /**
     * 下一页
     *
     * @return 若有翻页，则返回 <code>true</code>
     */
    public boolean nextPage() {
        int total = getPagingDataSize();
        int start = this.pageStart + getPageSize();

        if (start >= total) {
            // 进行轮播
            start = 0;
        }
        return updatePageStart(start);
    }

    /**
     * 上一页
     *
     * @return 若有翻页，则返回 <code>true</code>
     */
    public boolean prevPage() {
        int pageSize = getPageSize();
        int start = this.pageStart - pageSize;

        if (start < 0) {
            start = getLastPageStart();
        }
        return updatePageStart(start);
    }

    /** 纠正分页起始序号 */
    private void correctPageStart() {
        int total = getPagingDataSize();
        int start = this.pageStart;

        // 若起始序号超过数据总量，则回到最后一页
        if (start >= total) {
            start = getLastPageStart();
        }
        updatePageStart(start);
    }

    private boolean updatePageStart(int start) {
        if (this.pageStart != start) {
            this.pageStart = start;
            return true;
        }
        return false;
    }

    private int getLastPageStart() {
        int total = getPagingDataSize();
        int pageSize = getPageSize();

        int left = total % pageSize;
        if (left > 0) {
            return total - left;
        } else {
            // Note：total 可能为 0
            return Math.max(0, total - pageSize);
        }
    }
}
