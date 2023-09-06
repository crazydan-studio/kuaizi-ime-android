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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final Map<String, Map<String, InputWord>> candidateWordsCache = new LinkedHashMap<>(50);

    private final List<Input> inputs = new ArrayList<>();
    private final Cursor cursor = new Cursor();

    public InputList() {
        reset();
    }

    /**
     * 添加 {@link UserInputMsg} 消息监听
     * <p/>
     * 忽略重复加入的监听，且执行顺序与添加顺序无关
     */
    public void addUserInputMsgListener(UserInputMsgListener listener) {
        this.listeners.add(listener);
    }

    /** 移除 {@link UserInputMsg} 消息监听 */
    public void removeUserInputMsgListener(UserInputMsgListener listener) {
        this.listeners.remove(listener);
    }

    /** 处理{@link UserInputMsg 输入}消息 */
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        // Note: 存在在监听未执行完毕便移除监听的情况，故，在监听列表的副本中执行监听
        Set<UserInputMsgListener> listeners = new HashSet<>(this.listeners);
        listeners.forEach(listener -> listener.onUserInputMsg(msg, data));
    }

    /** 重置输入列表 */
    public void reset() {
        this.inputs.clear();
        this.cursor.reset();
        this.candidateWordsCache.clear();

        this.inputs.add(new GapInput());
        this.cursor.selected = this.inputs.get(0);

        UserInputMsgData msgData = new UserInputMsgData(null);
        onUserInputMsg(UserInputMsg.Cleaning_Inputs, msgData);
    }

    /** 缓存输入的候选字列表 */
    public void cacheCandidateWords(CharInput input, List<InputWord> candidateWords) {
        String inputChars = String.join("", input.getChars());

        if (!this.candidateWordsCache.containsKey(inputChars)) {
            this.candidateWordsCache.put(inputChars,
                                         candidateWords.stream()
                                                       .collect(Collectors.toMap(InputWord::getUid,
                                                                                 Function.identity(),
                                                                                 (a, b) -> a,
                                                                                 // 保持候选字的顺序不变
                                                                                 LinkedHashMap::new)));
        }
    }

    /** 获取已缓存的输入的候选字列表 */
    public Map<String, InputWord> getCachedCandidateWords(CharInput input) {
        String inputChars = String.join("", input.getChars());

        return this.candidateWordsCache.get(inputChars);
    }

    /** 是否有空的待输入 */
    public boolean hasEmptyPending() {
        return getPending() == null || getPending().isEmpty();
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
     * <li>选中该指定的输入；</li>
     * </ul>
     */
    public void newPendingOn(Input input) {
        if (input == null || indexOf(input) < 0 //
            || (getSelected() == input //
                && getPending() != null)) {
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
     * 在指定输入上新建待输入
     * <p/>
     * 注：<ul>
     * <li>若指定输入已选中且其待输入未确认，则不做处理；</li>
     * <li>自动确认当前的待输入；</li>
     * <li>选中该指定的输入；</li>
     * </ul>
     */
    public void newPendingOn(int inputIndex) {
        if (inputIndex < 0 || inputIndex >= this.inputs.size()) {
            return;
        }

        Input input = this.inputs.get(inputIndex);
        newPendingOn(input);
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
        CharInput pending = getPending();
        Input selected = getSelected();

        int selectedIndex = getSelectedIndex();
        if (pending == null || pending.isEmpty() || selectedIndex < 0) {
            return;
        }

        this.cursor.pending = null;
        if (selected instanceof CharInput) {
            // 保持对配对符号的引用
            pending.setPair(((CharInput) selected).getPair());

            this.inputs.set(selectedIndex, pending);
            this.cursor.selected = pending;
        } else if (selected instanceof GapInput) {
            // Note: 新的间隙位置自动后移，故无需更新光标的选中对象
            Input gap = new GapInput();
            this.inputs.addAll(selectedIndex, Arrays.asList(gap, pending));
        }
    }

    /** 移动到下一个输入位置或尾部 */
    public void moveToNextCharInput() {
        int index = getSelectedIndex();
        int lastIndex = this.inputs.size() - 1;
        if (index >= lastIndex) {
            return;
        }

        int nextIndex = index + 1;
        Input next = this.inputs.get(nextIndex);
        if (next instanceof GapInput && nextIndex < lastIndex) {
            next = this.inputs.get(nextIndex + 1);
        }
        this.cursor.selected = next;
        this.cursor.pending = null;
    }

    /** 丢弃待输入 */
    public void dropPending() {
        this.cursor.pending = null;
    }

    /** 获取已选中输入的位置 */
    public int getSelectedIndex() {
        Input selected = getSelected();
        return indexOf(selected);
    }

    /** 获取待输入 */
    public CharInput getPending() {
        return this.cursor.pending;
    }

    /** 获取指定输入上的待输入 */
    public CharInput getPendingOn(Input input) {
        return getSelected() == input ? getPending() : null;
    }

    /** 获取已选中的输入 */
    public Input getSelected() {
        return this.cursor.selected;
    }

    /** 指定输入是否已选中 */
    public boolean isSelected(Input input) {
        return getSelected() == input;
    }

    /** 当前选中的是否为光标位 */
    public boolean isGapSelected() {
        return getSelected() instanceof GapInput;
    }

    /** 清除在当前输入上的{@link CharInput#getPair() 配对符号输入} */
    public void clearPairOnSelected() {
        Input selected = getSelected();
        if (selected instanceof CharInput) {
            ((CharInput) selected).clearPair();
        }
    }

    /** 删除当前选中的输入：对于光标位置，仅删除其正在输入的内容 */
    public void deleteSelected() {
        Input selected = getSelected();
        if (selected instanceof CharInput) {
            deleteBackward();
        }

        this.cursor.pending = null;
    }

    /**
     * 回删输入
     * <p/>
     * 若当前为光标位，则删除其前面的输入；
     * 若当前为拉丁字符输入，则从其尾部逐字删除该输入；
     * 若当前为配对符号输入，则同时删除其另一侧的符号输入；
     * 若为其他输入，则直接删除该输入
     */
    public void deleteBackward() {
        int selectedIndex = getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }

        Input selected = getSelected();
        Input current = getPending() != null ? getPending() : selected;
        // 逐字删除拉丁字符输入的最后一个字符
        if (current.isLatin() && current.getKeys().size() > 1) {
            current.dropLastKey();
            return;
        }

        // 删除当前光标之前的 输入和占位
        if (selected instanceof GapInput) {
            if (selectedIndex > 0 && hasEmptyPending()) {
                int prevIndex = selectedIndex - 1;
                Input prev = this.inputs.get(prevIndex);

                if (prev.isLatin() && prev.getKeys().size() > 1) {
                    this.cursor.selected = prev;
                } else {
                    removeCharInputPair(prev);

                    // Note：当光标在 中间没有字符输入的配对符号 的中间位置时，
                    // 先删除其后的 配对字符输入 会导致该光标也被删除，
                    // 且其所在位置由 被删除的配对字符输入 的后继 Gap 填充，
                    // 故而，新光标位置不变但对应的 Gap 引用需更新
                    if (getSelectedIndex() < 0) {
                        this.cursor.selected = this.inputs.get(selectedIndex);
                    }

                    removeCharInput(prev);
                }
            } else {
                dropPending();
            }
        }
        // 删除当前选中的输入及其配对的占位
        else {
            removeCharInputPair(selected);

            // Note：当选中 左侧配对符号输入 且 配对符号中间 没有其他字符输入时，
            // 先删除右侧的 配对符号输入 会导致待选中的 Gap 一并被删除，
            // 且该光标所在位置由 被删除的配对字符输入 的后继 Gap 填充，
            // 故而，新光标位置不变但对应的 Gap 引用需更新
            selectedIndex = getSelectedIndex();
            Input newSelected = this.inputs.get(selectedIndex + 1);

            removeCharInput(selected);

            // 再将当前光标后移
            this.cursor.selected = newSelected;
            this.cursor.pending = null;
        }
    }

    public List<Input> getInputs() {
        return this.inputs;
    }

    public List<CharInput> getCharInputs() {
        return getInputs().stream()
                          .filter(input -> input instanceof CharInput)
                          .map(input -> ((CharInput) input))
                          .collect(Collectors.toList());
    }

    /** 获取全部的表情符号 */
    public List<InputWord> getEmojis() {
        return getInputs().stream()
                          .filter(Input::isEmoji)
                          .map(Input::getWord)
                          .filter(Objects::nonNull)
                          .collect(Collectors.toList());
    }

    /** 获取全部的拼音短语（未被非拼音输入隔开的输入均视为短语，但可能为单字） */
    public List<List<InputWord>> getPinyinPhraseWords() {
        return getPinyinPhraseWordsBefore(null);
    }

    /** 获取指定输入之前的拼音短语（未被非拼音输入隔开的输入均视为短语，但可能为单字） */
    public List<List<InputWord>> getPinyinPhraseWordsBefore(Input untilToInput) {
        return getPinyinPhraseInputsBefore(untilToInput).stream()
                                                        .map(phrase -> phrase.stream()
                                                                             .map(Input::getWord)
                                                                             .collect(Collectors.toList()))
                                                        .collect(Collectors.toList());
    }

    /**
     * 获取指定输入之前的拼音短语输入（未被非拼音输入隔开的输入均视为短语，但可能为单字输入）
     * <p/>
     * 注：不包含参数本身
     */
    public List<List<CharInput>> getPinyinPhraseInputsBefore(Input untilToInput) {
        List<List<CharInput>> list = new ArrayList<>();

        List<CharInput> phrase = new ArrayList<>();
        for (Input input : this.inputs) {
            if (untilToInput != null //
                && (input == untilToInput //
                    || (getPending() == untilToInput //
                        && getSelected() == input))) {
                break;
            }

            if (input.isPinyin()) {
                phrase.add((CharInput) input);
            } else if (!(input instanceof GapInput)) {
                if (!phrase.isEmpty()) {
                    list.add(phrase);
                }
                // 出现非拼音输入，重新开始下一个短语
                phrase = new ArrayList<>();
            }
        }

        // 最后一个可能为空，表示没有能与 指定输入 组成 短语 的输入
        list.add(phrase);

        return list;
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
        return hasEmptyPending();
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
        return before != null && (before.isPinyin() || before.isEmoji());
    }

    /** 指定位置的输入是否需要后续空格 */
    public boolean needPostSpace(int i) {
        Input input = this.inputs.get(i);
        if (!(input instanceof CharInput) || !input.isLatin()) {
            return false;
        }

        Input after = i < this.inputs.size() - 2 ? this.inputs.get(i + 2) : null;
        return after != null && (after.isPinyin() || after.isEmoji());
    }

    /** 获取指定输入的位置 */
    public int indexOf(Input input) {
        if (input == null) {
            return -1;
        }

        // Note: 这里需要做对象引用的判断，以避免内容相同的输入被判定为已选择
        for (int i = 0; i < this.inputs.size(); i++) {
            Input ipt = this.inputs.get(i);
            if (ipt == input) {
                return i;
            }
        }
        return -1;
    }

    public Input getFirstInput() {
        return this.inputs.size() > 0 ? this.inputs.get(0) : null;
    }

    public Input getLastInput() {
        return this.inputs.size() > 0 ? this.inputs.get(this.inputs.size() - 1) : null;
    }

    /** 删除指定的字符输入（包括与其配对的前序 Gap 位） */
    private void removeCharInput(Input input) {
        int index = input instanceof CharInput ? indexOf(input) : -1;

        removeCharInputAt(index);
    }

    /** 删除指定位置的字符输入（包括与其配对的前序 Gap 位） */
    private void removeCharInputAt(int index) {
        if (index <= 0) {
            return;
        }

        // 输入位
        this.inputs.remove(index);
        // Gap 位
        this.inputs.remove(index - 1);
    }

    /** 删除配对符号的另一侧输入 */
    private void removeCharInputPair(Input input) {
        if (input instanceof CharInput) {
            CharInput pairInput = ((CharInput) input).getPair();

            int pairInputIndex = indexOf(pairInput);
            if (pairInputIndex < 0) {
                return;
            }

            pairInput.clearPair();
            removeCharInputAt(pairInputIndex);
        }
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
