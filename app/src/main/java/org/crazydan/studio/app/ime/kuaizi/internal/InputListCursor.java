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

package org.crazydan.studio.app.ime.kuaizi.internal;

/**
 * {@link InputList 输入列表}中的光标
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class InputListCursor {
    /** 光标位置已选中的输入 */
    private Input selected;
    /** 光标位置待插入的输入 */
    private Input pending;

    public void reset() {
        this.selected = null;
        this.pending = null;
    }

    public Input getSelected() {
        return this.selected;
    }

    protected void setSelected(Input selected) {
        this.selected = selected;
    }

    public Input getPending() {
        return this.pending;
    }

    protected void setPending(Input pending) {
        this.pending = pending;
    }
}
