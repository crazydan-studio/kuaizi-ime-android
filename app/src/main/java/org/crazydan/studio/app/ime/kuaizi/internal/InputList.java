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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.GapInput;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgListener;

/**
 * {@link Keyboard 键盘}输入列表，含零个或多个{@link Input 输入对象}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class InputList {
    private final Set<UserInputMsgListener> listeners = new HashSet<>();

    private final List<Input> inputs = new ArrayList<>();
    private final Cursor cursor = new Cursor();

    public InputList() {
        empty();
    }

    /**
     * 添加 {@link UserInputMsg} 消息监听
     * <p/>
     * 忽略重复加入的监听，且执行顺序与添加顺序无关
     */
    public void addUserInputMsgListener(UserInputMsgListener listener) {
        this.listeners.add(listener);
    }

    /** 处理{@link UserInputMsg 输入}消息 */
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        this.listeners.forEach(listener -> listener.onUserInputMsg(msg, data));
    }

    /** 清空输入列表 */
    public void empty() {
        this.inputs.clear();
        this.cursor.reset();

        this.inputs.add(new GapInput());
        this.cursor.selected = this.inputs.get(0);
    }

    /** 是否有待输入 */
    public boolean hasPending() {
        return this.cursor.pending != null;
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
        this.cursor.pending = input;

        return input;
    }

    /**
     * 在指定输入上新建待输入
     * <p/>
     * 注：<ul>
     * <li>若指定输入已选中且其待输入未确认，则不做处理；</li>
     * <li>自动确认当前的待输入；</li>
     * </ul>
     */
    public void newPendingOn(Input input) {
        if (indexOf(input) < 0 //
            || (this.cursor.selected == input //
                && this.cursor.pending != null)) {
            return;
        }

        if (input instanceof GapInput) {
            // 在间隙位置，需要新建待输入，以插入新的输入
            newPending();
            this.cursor.selected = input;
        } else {
            // 先确认当前的待输入，再将原输入复制一份做为待输入，
            // 从而可以对其进行输入替换，或做候选字修改
            confirmPending();

            this.cursor.selected = input;
            this.cursor.pending = ((CharInput) input).copy();
        }
    }

    /**
     * 确认待输入
     * <p/>
     * <ul>
     * <li>原位置为字符输入时，直接原地替换；</li>
     * <li>原位置为间隙时，插入待输入，并将光标移到新输入的后面；</li>
     * <li>无待输入或待输入为空时，不做处理；</li>
     * </ul>
     */
    public void confirmPending() {
        CharInput pending = this.cursor.pending;
        Input selected = this.cursor.selected;

        int selectedIndex = getSelectedIndex();
        if (pending == null || pending.isEmpty() || selectedIndex < 0) {
            return;
        }

        this.cursor.pending = null;
        if (selected instanceof CharInput) {
            this.inputs.set(selectedIndex, pending);
            this.cursor.selected = pending;
        } else if (selected instanceof GapInput) {
            // Note: 新的间隙位置自动后移，故无需更新光标的选中对象
            Input gap = new GapInput();
            this.inputs.addAll(selectedIndex, Arrays.asList(gap, pending));
        }
    }

    /** 丢弃待输入 */
    public void dropPending() {
        this.cursor.pending = null;
    }

    /** 获取已选中输入的位置 */
    public int getSelectedIndex() {
        Input selected = this.cursor.selected;
        return indexOf(selected);
    }

    /** 获取待输入 */
    public CharInput getPending() {
        return this.cursor.pending;
    }

    /** 获取指定输入上的待输入 */
    public CharInput getPendingOn(Input input) {
        return this.cursor.selected == input ? this.cursor.pending : null;
    }

    /** 获取已选中的输入 */
    public Input getSelected() {
        return this.cursor.selected;
    }

    /** 指定输入是否已选中 */
    public boolean isSelected(Input input) {
        return getSelected() == input;
    }

    /** 删除光标左边的输入 */
    public void backwardDelete() {
        int selectedIndex = getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }

        Input selected = this.cursor.selected;
        if (selected instanceof GapInput) {
            if (selectedIndex > 0) {
                // 删除当前光标之前的 输入和占位
                this.inputs.remove(selectedIndex - 1);
                this.inputs.remove(selectedIndex - 2);
            }
        } else if (selected instanceof CharInput) {
            Input newSelected = this.inputs.get(selectedIndex + 1);

            // 删除当前选中的输入及其配对的占位
            this.inputs.remove(selectedIndex);
            this.inputs.remove(selectedIndex - 1);
            // 再将当前光标后移
            this.cursor.selected = newSelected;
        }
    }

    public List<Input> getInputs() {
        return this.inputs;
    }

    /** 获取已选中输入之前的输入 */
    public Input getInputBeforeSelected() {
        int selectedIndex = getSelectedIndex();
        if (selectedIndex <= 0) {
            return null;
        }
        return this.inputs.get(selectedIndex - 1);
    }

    /**
     * 输入列表是否为空
     * <p/>
     * 包含有效的输入时，才不为空
     */
    public boolean isEmpty() {
        for (Input input : this.inputs) {
            if (input instanceof CharInput && !input.isEmpty()) {
                return false;
            }
        }
        return !hasPending() || getPending().isEmpty();
    }

    /** 获取输入文本内容 */
    public StringBuilder getText() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < this.inputs.size(); i++) {
            Input input = this.inputs.get(i);

            if (needPrevSpace(i)) {
                sb.append(" ");
            }

            sb.append(input.getText());

            if (needPostSpace(i)) {
                sb.append(" ");
            }
        }
        return sb;
    }

    /** 指定位置的输入是否需要前序空格 */
    public boolean needPrevSpace(int i) {
        Input input = this.inputs.get(i);
        if (!(input instanceof CharInput) || !input.isLatin()) {
            return false;
        }

        Input before = i > 1 ? this.inputs.get(i - 2) : null;
        return before != null && (before.isPinyin() || before.isEmotion());
    }

    /** 指定位置的输入是否需要后续空格 */
    public boolean needPostSpace(int i) {
        Input input = this.inputs.get(i);
        if (!(input instanceof CharInput) || !input.isLatin()) {
            return false;
        }

        Input after = i < this.inputs.size() - 2 ? this.inputs.get(i + 2) : null;
        return after != null && (after.isPinyin() || after.isEmotion());
    }

    /** 获取指定输入的位置 */
    public int indexOf(Input input) {
        // Note: 这里需要做对象引用的判断，以避免内容相同的输入被判定为已选择
        for (int i = 0; i < this.inputs.size(); i++) {
            Input ipt = this.inputs.get(i);
            if (ipt == input) {
                return i;
            }
        }
        return -1;
    }

    private static class Cursor {
        /** 光标位置已选中的输入 */
        private Input selected;
        /** 光标位置待插入的输入 */
        private CharInput pending;

        public void reset() {
            this.selected = null;
            this.pending = null;
        }
    }
}
