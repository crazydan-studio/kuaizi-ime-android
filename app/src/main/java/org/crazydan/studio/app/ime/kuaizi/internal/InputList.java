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
        getInputs().clear();
        this.cursor.reset();

        getInputs().add(new GapInput());
        this.cursor.setSelected(getInputs().get(0));
    }

    /** 是否有待输入 */
    public boolean hasPending() {
        return this.cursor.getPending() != null;
    }

    /**
     * 新建待输入
     * <p/>
     * 自动确认当前的待输入
     */
    public CharInput newPending() {
        // 先确认当前的待输入
        confirmPending();

        // 再创建新的待输入
        CharInput input = new CharInput();
        this.cursor.setPending(input);

        return input;
    }

    /**
     * 确认待输入
     * <p/>
     * 原位置为字符输入时，直接原地替换；
     * 原位置为间隙时，插入待输入，并将光标移到新输入的后面；
     */
    public void confirmPending() {
        CharInput pending = this.cursor.getPending();
        Input selected = this.cursor.getSelected();

        int cursorIndex = getCursorIndex();
        if (pending == null || pending.isEmpty() || cursorIndex < 0) {
            return;
        }

        this.cursor.setPending(null);
        if (selected instanceof CharInput) {
            getInputs().set(cursorIndex, pending);
            this.cursor.setSelected(pending);
        } else if (selected instanceof GapInput) {
            // Note: 新的间隙位置自动后移，故无需更新光标的选中对象
            Input gap = new GapInput();
            getInputs().addAll(cursorIndex, Arrays.asList(gap, pending));
        }
    }

    /** 丢弃待输入 */
    public void dropPending() {
        this.cursor.setPending(null);
    }

    /** 获取光标位置 */
    public int getCursorIndex() {
        Input selected = this.cursor.getSelected();
        return getInputs().indexOf(selected);
    }

    /** 获取待输入 */
    public CharInput getPending() {
        return this.cursor.getPending();
    }

    /** 获取以选中的输入 */
    public Input getSelected() {
        return this.cursor.getSelected();
    }

    /** 删除光标左边的输入 */
    public void backwardDelete() {
        int cursorIndex = getCursorIndex();
        if (cursorIndex < 0) {
            return;
        }

        Input selected = this.cursor.getSelected();
        if (selected instanceof GapInput) {
            if (cursorIndex > 0) {
                // 删除当前光标之前的 输入和占位
                getInputs().remove(cursorIndex - 1);
                getInputs().remove(cursorIndex - 2);
            }
        } else if (selected instanceof CharInput) {
            Input newSelected = getInputs().get(cursorIndex + 1);

            // 删除当前选中的输入及其配对的占位
            getInputs().remove(cursorIndex);
            getInputs().remove(cursorIndex - 1);
            // 再将当前光标后移
            this.cursor.setSelected(newSelected);
        }
    }

    public List<Input> getInputs() {
        return this.inputs;
    }
}
