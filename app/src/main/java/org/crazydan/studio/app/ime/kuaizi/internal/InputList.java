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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.GapInput;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;

/**
 * {@link Keyboard 键盘}输入列表，含零个或多个{@link Input 输入对象}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class InputList {
    private final Set<UserInputMsgListener> listeners = new HashSet<>();
    private final Map<String, Map<String, InputWord>> candidateWordsCache = new LinkedHashMap<>(50);

    private final List<Input<?>> inputs = new ArrayList<>();
    private final Cursor cursor = new Cursor();
    /** 暂存器，用于临时记录已删除、已提交输入，以支持撤销删除和提交操作 */
    private Staged staged = Staged.none();

    private Input.Option option;
    private List<CompletionInput> phraseCompletions;

    public InputList() {
        reset(false);
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
    public void reset(boolean canBeCanceled) {
        this.staged = doReset(canBeCanceled ? Staged.Type.deleted : Staged.Type.none);

        UserInputMsgData msgData = new UserInputMsgData(null);
        onUserInputMsg(UserInputMsg.Inputs_Clean_Done, msgData);
    }

    /**
     * 提交输入列表
     * <p/>
     * 返回{@link #getText() 输入文本}，并{@link #reset 重置}
     */
    public StringBuilder commit(boolean canBeRevoked) {
        StringBuilder text = getText();

        this.staged = doReset(canBeRevoked ? Staged.Type.committed : Staged.Type.none);

        return text;
    }

    /** 是否可撤回已提交输入 */
    public boolean canRevokeCommit() {
        return this.staged.type == Staged.Type.committed;
    }

    /** 撤回已提交的输入 */
    public void revokeCommit() {
        if (canRevokeCommit()) {
            Staged.restore(this, this.staged);
        }
    }

    /** 清除 提交撤回数据 */
    public void cleanCommitRevokes() {
        if (canRevokeCommit()) {
            this.staged = Staged.none();
        }
    }

    /** 是否可撤销已删除输入 */
    public boolean canCancelDelete() {
        return this.staged.type == Staged.Type.deleted;
    }

    /** 撤销已删除输入 */
    public void cancelDelete() {
        if (canCancelDelete()) {
            Staged.restore(this, this.staged);

            UserInputMsgData msgData = new UserInputMsgData(null);
            onUserInputMsg(UserInputMsg.Inputs_Cleaned_Cancel_Done, msgData);
        }
    }

    /** 清除 删除撤销数据 */
    public void cleanDeleteCancels() {
        if (canCancelDelete()) {
            this.staged = Staged.none();
        }
    }

    /** 重置列表，并返回指定类型的暂存器（存储重置前的输入数据） */
    private Staged doReset(Staged.Type stagedType) {
        Staged staged = Staged.store(stagedType, this);

        this.inputs.clear();
        this.cursor.reset();
        this.candidateWordsCache.clear();
        clearPhraseCompletions();

        Input<?> gap = new GapInput();
        this.inputs.add(gap);
        doSelect(gap);

        this.option = null;
        this.staged = Staged.none();

        return staged;
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

    /** 当前的待输入是否为空 */
    public boolean hasEmptyPending() {
        return Input.isEmpty(getPending());
    }

    public Input.Option getOption() {
        return this.option;
    }

    public void setOption(Input.Option option) {
        this.option = option;
    }

    /** 设置短语输入补全 */
    public void setPhraseCompletions(List<CompletionInput> phraseCompletions) {
        this.phraseCompletions = phraseCompletions != null && !phraseCompletions.isEmpty() ? phraseCompletions : null;
    }

    /** 清空短语输入补全 */
    public void clearPhraseCompletions() {
        this.phraseCompletions = null;
    }

    /** 是否有输入补全 */
    public boolean hasCompletions() {
        return (this.phraseCompletions != null) //
               || (getPending() != null && getPending().hasCompletions());
    }

    /** 获取输入补全 */
    public List<CompletionInput> getCompletions() {
        if (this.phraseCompletions != null) {
            return this.phraseCompletions;
        } else if (getPending() != null && getPending().hasCompletions()) {
            return getPending().getCompletions();
        }
        return null;
    }

    /** 应用输入补全 */
    public void applyCompletion(CompletionInput completion) {
        if (this.phraseCompletions != null) {
            applyPhraseCompletion(completion);
            clearPhraseCompletions();
        } else if (getPending() != null && getPending().hasCompletions()) {
            getPending().applyCompletion(completion);
        }
    }

    /**
     * 应用短语输入补全
     * <p/>
     * 在当前选中位置开始，逐个插入剩余部分
     */
    private void applyPhraseCompletion(CompletionInput completion) {
        int startIndex = completion.startIndex;

        // 从补全位置开始插入输入
        for (int i = startIndex; i < completion.inputs.size(); i++) {
            CharInput input = completion.inputs.get(i);
            withPending(input);
            confirmPendingAndSelectNext();
        }
    }

    /** 重新创建当前输入的待输入（不确认已有的待输入） */
    public CharInput newPending() {
        CharInput input = new CharInput();

        return withPending(input);
    }

    /** 将当前的待输入设置为指定输入（不确认已有的待输入） */
    public CharInput withPending(CharInput input) {
        this.cursor.withPending(input);

        return input;
    }

    /**
     * {@link #confirmPending() 确认当前待输入}，再后移光标，并在新位置新建待输入
     * <p/>
     * 即，{@link #confirmPendingAndSelectByOffset} 的参数为 <code>1</code>
     */
    public void confirmPendingAndSelectNext() {
        confirmPendingAndSelectByOffset(1);
    }

    /**
     * {@link #confirmPending() 确认当前待输入}，再前移光标，并在新位置新建待输入
     * <p/>
     * 即，{@link #confirmPendingAndSelectByOffset} 的参数为 <code>-1</code>
     */
    public void confirmPendingAndSelectPrevious() {
        confirmPendingAndSelectByOffset(-1);
    }

    /**
     * {@link #confirmPending() 确认当前待输入}，
     * 再后移光标到指定的偏移位置，并在该位置新建待输入
     */
    public void confirmPendingAndSelectByOffset(int offset) {
        int selectedIndex = getSelectedIndex();
        if (hasEmptyPending() || selectedIndex < 0) {
            return;
        }

        int confirmedPendingIndex = doConfirmPending();

        Input<?> newSelected = this.inputs.get(confirmedPendingIndex + offset);
        doSelect(newSelected);
    }

    /** {@link #confirmPending() 确认当前待输入}，并{@link #select(Input) 选中指定输入} */
    public void confirmPendingAndSelect(Input<?> input) {
        confirmPending();
        select(input);
    }

    /** {@link #confirmPending() 确认当前待输入}，并{@link #selectLast() 选中最后一个输入} */
    public void confirmPendingAndSelectLast() {
        confirmPending();
        selectLast();
    }

    /**
     * 确认当前待输入
     * <p/>
     * <ul>
     * <li>待输入为空时，不做处理；</li>
     * <li>原位置为 Gap 时，插入待输入；</li>
     * <li>原位置为字符输入时，将其替换为待输入；</li>
     * </ul>
     */
    public void confirmPending() {
        doConfirmPending();
    }

    /**
     * 确认当前待输入，并返回确认后的{@link #getSelectedIndex() 当前输入位置}
     * <p/>
     * <ul>
     * <li>待输入为空时，不做处理；</li>
     * <li>原位置为 Gap 时，插入待输入；</li>
     * <li>原位置为字符输入时，将其替换为待输入；</li>
     * </ul>
     */
    private int doConfirmPending() {
        int selectedIndex = getSelectedIndex();
        if (hasEmptyPending() || selectedIndex < 0) {
            return selectedIndex;
        }

        Input<?> selected = getSelected();
        CharInput pending = getPending();
        // 先由待输入做其内部确认
        pending.confirm();

        if (selected.isGap()) {
            // Note: 新的 Gap 位置自动后移，故无需更新光标的选中对象
            Input<?> gap = new GapInput();

            this.inputs.addAll(selectedIndex, Arrays.asList(gap, pending));
        } else {
            // 保持对配对符号的引用
            pending.setPair(((CharInput) selected).getPair());

            this.inputs.set(selectedIndex, pending);
        }

        doSelect(pending);

        return getSelectedIndex();
    }

    /** {@link #select(int) 选中指定的输入} */
    public void select(Input<?> input) {
        int index = indexOf(input);

        select(index);
    }

    /**
     * 选中指定位置的输入
     * <p/>
     * 注：<ul>
     * <li>若指定输入已选中，则不做处理；</li>
     * <li>自动确认当前的待输入；</li>
     * </ul>
     */
    public void select(int index) {
        if (index < 0 || index >= this.inputs.size()) {
            return;
        }

        Input<?> input = this.inputs.get(index);
        if (isSelected(input)) {
            return;
        }

        confirmPendingAndSelectNext();

        doSelect(input);
    }

    /**
     * 从当前位置向后选中匹配到的第一个输入
     * <p/>
     * 若未匹配到输入，则不做任何处理
     */
    public Input<?> selectNextFirstMatched(Predicate<Input<?>> filter) {
        int index = getSelectedIndex();
        int lastIndex = this.inputs.size() - 1;
        if (index >= lastIndex) {
            return null;
        }

        for (int i = index + 1; i <= lastIndex; i++) {
            Input<?> next = this.inputs.get(i);

            if (filter.test(next)) {
                doSelect(next);

                return next;
            }
        }
        return null;
    }

    /**
     * 选中最后一个输入
     * <p/>
     * 若已选中最后一个输入，则不做处理
     *
     * @return 返回选中的输入
     */
    public Input<?> selectLast() {
        Input<?> last = getLastInput();

        if (!isSelected(last)) {
            doSelect(last);
        }
        return last;
    }

    /** 丢弃当前的待输入 */
    public void dropPending() {
        newPending();
    }

    /** 获取已选中输入的位置 */
    public int getSelectedIndex() {
        Input<?> selected = getSelected();
        return indexOf(selected);
    }

    /** 获取待输入 */
    public CharInput getPending() {
        return this.cursor.pending;
    }

    /** 获取指定输入上的待输入 */
    public CharInput getPendingOn(Input<?> input) {
        return getSelected() == input ? getPending() : null;
    }

    /** 获取指定输入上的非空待输入 */
    public CharInput getNoneEmptyPendingOn(Input<?> input) {
        CharInput pending = getPendingOn(input);

        return Input.isEmpty(pending) ? null : pending;
    }

    /** 获取已选中的输入 */
    public Input<?> getSelected() {
        return this.cursor.selected;
    }

    /** 指定输入是否已选中 */
    public boolean isSelected(Input<?> input) {
        return getSelected() == input;
    }

    /** 当前选中的是否为光标位 */
    public boolean isGapSelected() {
        return Input.isGap(getSelected());
    }

    /** 丢弃已选择输入，光标处于游离态 */
    public void dropSelected() {
        this.cursor.reset();
    }

    /** 清除在当前输入上的{@link CharInput#getPair() 配对符号输入} */
    public void clearPairOnSelected() {
        Input<?> selected = getSelected();

        if (!Input.isGap(selected)) {
            ((CharInput) selected).clearPair();
        }
    }

    /** 删除当前选中的输入：对于 Gap 位置，仅删除其正在输入的内容 */
    public void deleteSelected() {
        Input<?> selected = getSelected();

        if (!Input.isGap(selected)) {
            doDeleteBackward(false);
        }

        dropPending();
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
        doDeleteBackward(true);
    }

    /**
     * 回删输入
     * <p/>
     * 若当前为光标位，则删除其前面的输入；
     * 若当前为拉丁字符输入，则从其尾部逐字删除该输入；
     * 若当前为配对符号输入，则同时删除其另一侧的符号输入；
     * 若为其他输入，则直接删除该输入
     */
    private void doDeleteBackward(boolean byStep) {
        int selectedIndex = getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }

        Input<?> selected = getSelected();
        Input<?> pending = getPending();
        Input<?> current = !Input.isEmpty(pending) ? pending : selected;
        // 逐字删除拉丁字符输入的最后一个字符
        if (byStep && current.isLatin() && current.getKeys().size() > 1) {
            current.dropLastKey();
            return;
        }

        // 删除当前光标之前的 输入和占位
        if (selected.isGap()) {
            if (selectedIndex > 0 && hasEmptyPending()) {
                int prevIndex = selectedIndex - 1;
                Input<?> prev = this.inputs.get(prevIndex);

                if (prev.isLatin() && prev.getKeys().size() > 1) {
                    doSelect(prev);
                } else {
                    removeCharInputPair(prev);

                    // Note：当光标在 中间没有字符输入的配对符号 的中间位置时，
                    // 先删除其后的 配对字符输入 会导致该光标也被删除，
                    // 且其所在位置由 被删除的配对字符输入 的后继 Gap 填充，
                    // 故而，新光标位置不变但对应的 Gap 引用需更新
                    if (getSelectedIndex() < 0) {
                        doSelect(this.inputs.get(selectedIndex));
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
            Input<?> newSelected = this.inputs.get(selectedIndex + 1);

            removeCharInput(selected);

            // 再将当前光标后移
            doSelect(newSelected);
        }
    }

    public List<Input<?>> getInputs() {
        return this.inputs;
    }

    public List<CharInput> getCharInputs() {
        return getInputs().stream()
                          .filter(input -> !input.isGap())
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

    /** 获取拉丁文输入 */
    public Set<String> getLatins() {
        return getInputs().stream()
                          .filter(Input::isLatin)
                          .map(Input::getText)
                          .filter(Objects::nonNull)
                          .map(StringBuilder::toString)
                          .collect(Collectors.toSet());
    }

    /** 获取全部的拼音短语（未被非拼音输入隔开的输入均视为短语，但可能为单字） */
    public List<List<InputWord>> getPinyinPhraseWords() {
        return getPinyinPhraseWordsBefore(null);
    }

    /** 获取指定输入之前的拼音短语（未被非拼音输入隔开的输入均视为短语，但可能为单字） */
    public List<List<InputWord>> getPinyinPhraseWordsBefore(Input<?> untilToInput) {
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
    public List<List<CharInput>> getPinyinPhraseInputsBefore(Input<?> untilToInput) {
        List<List<CharInput>> list = new ArrayList<>();

        List<CharInput> phrase = new ArrayList<>();
        for (Input<?> input : this.inputs) {
            if (untilToInput != null //
                && (input == untilToInput //
                    || (getPending() == untilToInput //
                        && getSelected() == input))) {
                break;
            }

            if (input.isPinyin()) {
                phrase.add((CharInput) input);
            } else if (!input.isGap()) {
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
    public Input<?> getInputBeforeSelected() {
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
        for (Input<?> input : this.inputs) {
            if (!input.isGap() && !input.isEmpty()) {
                return false;
            }
        }
        return hasEmptyPending();
    }

    /** 获取输入文本内容 */
    public StringBuilder getText() {
        StringBuilder sb = new StringBuilder();

        int total = this.inputs.size();
        for (int i = 0; i < total; i++) {
            Input<?> input = this.inputs.get(i);

            sb.append(input.getText(this.option));

            if (needGapSpace(i)) {
                sb.append(" ");
            }
        }
        return sb;
    }

    /** 是否需要添加 Gap 空格 */
    public boolean needGapSpace(int i) {
        int total = this.inputs.size();
        if (i <= 0 || i >= total) {
            return false;
        }

        Input<?> input = this.inputs.get(i);
        Input<?> left = this.inputs.get(i - 1);
        Input<?> right = null;
        if (!input.isGap()) {
            // Note：input 与其左侧的正在输入的 Gap 也需要检查空格（CharInput 左侧必然有一个 Gap）
            if (!isSelected(left) || Input.isEmpty(getPending())) {
                return false;
            } else {
                right = input;
            }
        }

        left = getNoneEmptyPendingOrSelf(left);
        if (right == null) {
            // Note：Gap 需判断其上的待输入
            Input<?> pendingOnGap = getNoneEmptyPendingOn(input);
            right = !Input.isEmpty(pendingOnGap) || i == total - 1
                    ? pendingOnGap
                    : getNoneEmptyPendingOrSelf(this.inputs.get(i + 1));
        }
        if (right == null) {
            return false;
        }

        Input.Option option = this.option;
        if (left.isMathExpr() || right.isMathExpr()) {
            return true;
        } else if (left.isLatin()) {
            return !right.isSymbol();
        } else if (right.isLatin()) {
            return !left.isSymbol();
        } else if (left.isMathOperator() || right.isMathOperator()) {
            // 数学运算符前后都有空格
            return true;
        } else if (left.isTextOnlyWordNotation(option)) {
            return !right.isSymbol();
        } else if (right.isTextOnlyWordNotation(option)) {
            return !left.isSymbol();
        }
        return false;
    }

    /** 是否需要添加 Gap 空格 */
    public boolean needGapSpace(Input<?> input) {
        int i = indexOf(input);
        return needGapSpace(i);
    }

    /** 获取指定输入的位置 */
    public int indexOf(Input<?> input) {
        if (input == null) {
            return -1;
        }

        // Note: 这里需要做对象引用的判断，以避免内容相同的输入被判定为已选择
        return CollectionUtils.indexOfRef(this.inputs, input);
    }

    public Input<?> getFirstInput() {
        return CollectionUtils.first(this.inputs);
    }

    public Input<?> getLastInput() {
        return CollectionUtils.last(this.inputs);
    }

    public CharInput getFirstCharInput() {
        return CollectionUtils.first(getCharInputs());
    }

    public CharInput getLastCharInput() {
        return CollectionUtils.last(getCharInputs());
    }

    /** 选中指定的输入，并为其新建待输入（空白输入或原输入的副本） */
    private void doSelect(Input<?> input) {
        this.cursor.select(input);
    }

    /** 若存在则获取非空待输入，否则，返回输入自身 */
    private Input<?> getNoneEmptyPendingOrSelf(Input<?> input) {
        Input<?> pending = getNoneEmptyPendingOn(input);
        return pending != null ? pending : input;
    }

    /** 删除指定的字符输入（包括与其配对的前序 Gap 位） */
    private void removeCharInput(Input<?> input) {
        int index = !Input.isGap(input) ? indexOf(input) : -1;

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
    private void removeCharInputPair(Input<?> input) {
        if (!Input.isGap(input)) {
            CharInput pairInput = ((CharInput) input).getPair();

            int pairInputIndex = indexOf(pairInput);
            if (pairInputIndex < 0) {
                return;
            }

            pairInput.clearPair();
            removeCharInputAt(pairInputIndex);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InputList that = (InputList) o;
        return this.inputs.equals(that.inputs) && this.cursor.equals(that.cursor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.inputs, this.cursor);
    }

    private static class Cursor {
        /** 光标位置已选中的输入 */
        private Input<?> selected;
        /** 光标位置待插入的输入 */
        private CharInput pending;

        public void reset() {
            this.selected = null;
            this.pending = null;
        }

        public Cursor copy() {
            return copyTo(new Cursor());
        }

        public Cursor copyTo(Cursor target) {
            target.selected = this.selected;
            target.pending = this.pending;

            return target;
        }

        /**
         * 选中指定输入，并将其复制一份作为其待输入，
         * 若输入为 Gap，则为其创建空的待输入
         */
        public void select(Input<?> input) {
            CharInput pending = Input.isGap(input) ? new CharInput() : ((CharInput) input).copy();

            this.selected = input;

            withPending(pending);
        }

        public void withPending(CharInput input) {
            this.pending = input;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Cursor that = (Cursor) o;
            return Objects.equals(this.selected, that.selected) //
                   && Objects.equals(this.pending, that.pending);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.selected, this.pending);
        }
    }

    private static class Staged {
        public final Type type;
        public final List<Input<?>> inputs;
        public final Cursor cursor;

        private Staged(Type type, List<Input<?>> inputs, Cursor cursor) {
            this.type = type;
            this.inputs = inputs;
            this.cursor = cursor;
        }

        public static Staged none() {
            return new Staged(Type.none, null, null);
        }

        public static Staged store(Type type, InputList inputList) {
            // 为空的输入列表的无需暂存
            if (type == Type.none || inputList.isEmpty()) {
                return none();
            }

            return new Staged(type, new ArrayList<>(inputList.inputs), inputList.cursor.copy());
        }

        public static void restore(InputList inputList, Staged staged) {
            if (staged.type == Type.none) {
                return;
            }

            inputList.doReset(Type.none);

            inputList.inputs.clear();
            inputList.inputs.addAll(staged.inputs);

            staged.cursor.copyTo(inputList.cursor);
        }

        public enum Type {
            none,
            deleted,
            committed,
        }
    }
}
