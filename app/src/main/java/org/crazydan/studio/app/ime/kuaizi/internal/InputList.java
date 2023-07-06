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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.GapInput;

/**
 * {@link Keyboard 键盘}输入列表，含零个或多个{@link Input 输入对象}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class InputList {
    private final List<Input> inputs = new ArrayList<>();
    private final InputListCursor cursor = new InputListCursor();

    public InputList() {
        empty();
    }

    /** 清空输入列表 */
    public void empty() {
        inputs().clear();
        cursor().reset();

        inputs().add(new GapInput());
        cursor().selected(inputs().get(0));
    }

    /**
     * 确认待输入
     * <p/>
     * 原位置为字符输入时，直接原地替换；
     * 原位置为间隙时，插入待输入，并将光标移到新输入的后面；
     */
    public void confirmPending() {
        Input pending = cursor().pending();
        Input selected = cursor().selected();

        int selectedIndex = inputs().indexOf(selected);
        if (pending == null || selectedIndex < 0) {
            return;
        }

        cursor().pending(null);
        if (selected instanceof CharInput) {
            inputs().set(selectedIndex, pending);
            cursor().selected(pending);
        } else if (selected instanceof GapInput) {
            // Note: 新的间隙位置自动后移，故无需更新光标的选中对象
            Input gap = new GapInput();
            inputs().addAll(selectedIndex, Arrays.asList(gap, pending));
        }
    }

    /**
     * 初始化待输入
     */
    public void initPending() {
        // 先确认当前的待输入
        confirmPending();

        // 再创建新的待输入
        CharInput input = new CharInput();
        cursor().pending(input);
    }

    public List<Input> inputs() {
        return this.inputs;
    }

    public InputListCursor cursor() {
        return this.cursor;
    }
}
