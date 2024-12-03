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

package org.crazydan.studio.app.ime.kuaizi.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.keyboard.conf.Configuration;
import org.crazydan.studio.app.ime.kuaizi.keyboard.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.keyboard.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.keyboard.input.GapInput;
import org.crazydan.studio.app.ime.kuaizi.keyboard.input.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserInputMsgListener;

/**
 * {@link Keyboard 键盘}输入列表，含零个或多个{@link Input 输入对象}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class InputList {
    private final List<Input<?>> inputs = new ArrayList<>();
    private final Cursor cursor = new Cursor();

    private Supplier<Configuration> configGetter;
    private UserInputMsgListener listener;
    /** 暂存器，用于临时记录已删除、已提交输入，以支持撤销删除和提交操作 */
    private Staged staged;

    private Input.Option option;
    private List<CompletionInput> phraseCompletions;

    public InputList() {
        this.staged = doReset(Staged.Type.none);
    }

    public Configuration getConfig() {
        return this.configGetter.get();
    }

    public void setConfig(Supplier<Configuration> getter) {
        this.configGetter = getter;
    }

    public void setListener(UserInputMsgListener listener) {
        this.listener = listener;
    }

    public void fireUserInputMsg(UserInputMsg msg, UserInputMsgData msgData) {
        this.listener.onMsg(this, msg, msgData);
    }

    /** 重置输入列表 */
    public void reset(boolean canBeCanceled) {
        this.staged = doReset(canBeCanceled ? Staged.Type.deleted : Staged.Type.none);

        UserInputMsgData msgData = new UserInputMsgData();
        fireUserInputMsg(UserInputMsg.Inputs_Clean_Done, msgData);
    }

    /** 清空输入列表 */
    public void clear() {
        clearCommitRevokes();
        clearDeleteCancels();

        clearPhraseCompletions();
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

    /** 尝试选中指定输入：实际仅发送 {@link UserInputMsg#Input_Choose_Doing} 消息 */
    public void trySelect(Input<?> input) {
        if (input == null) {
            return;
        }

        UserInputMsg msg = UserInputMsg.Input_Choose_Doing;
        UserInputMsgData msgData = new UserInputMsgData(input);

        fireUserInputMsg(msg, msgData);
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
    public void clearCommitRevokes() {
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

            UserInputMsgData msgData = new UserInputMsgData();
            fireUserInputMsg(UserInputMsg.Inputs_Cleaned_Cancel_Done, msgData);
        }
    }

    /** 清除 删除撤销数据 */
    public void clearDeleteCancels() {
        if (canCancelDelete()) {
            this.staged = Staged.none();
        }
    }

    /** 重置列表，并返回指定类型的暂存器（存储重置前的输入数据） */
    private Staged doReset(Staged.Type stagedType) {
        Staged staged = Staged.store(stagedType, this);

        this.inputs.clear();
        this.cursor.reset();

        clearPhraseCompletions();

        Input<?> gap = new GapInput();
        this.inputs.add(gap);
        doSelect(gap);

        setOption(null);
        this.staged = Staged.none();

        return staged;
    }

    /** 当前的待输入是否为空 */
    public boolean hasEmptyPending() {
        return Input.isEmpty(getPending());
    }

    public Input.Option getOption() {
        if (this.option == null) {
            Configuration config = getConfig();

            this.option = new Input.Option(null, config.isCandidateVariantFirstEnabled());
        }
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
        int confirmedPendingIndex = doConfirmPending();
        if (confirmedPendingIndex < 0) {
            return;
        }

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
     *
     * @return 若未作确认（空白待输入无需确认），则返回 <code>-1</code>
     */
    private int doConfirmPending() {
        int selectedIndex = getSelectedIndex();
        if (selectedIndex < 0) {
            return -1;
        }

        if (hasEmptyPending()) {
            // 若当前为空白算术输入，则直接将其移除
            if (getSelected().isMathExpr() && getSelected().isEmpty()) {
                removeCharInputAt(selectedIndex);
                // 选中相邻的后继 Gap
                doSelect(selectedIndex - 1);
            }

            // Note：当前待输入可能为空白的算术输入，
            // 为了确保确认后能继续接受普通输入，需重置当前的待输入
            newPending();

            return -1;
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
        Input<?> input = getInput(index);
        if (input == null || isSelected(input)) {
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

    /** 获取指定位置的输入 */
    public Input<?> getInput(int index) {
        return getInput(index, false);
    }

    /**
     * 获取指定位置的输入
     *
     * @param pendingFirst
     *         {@link #getPending() 待输入}优先，
     *         即，当指定位置为{@link #getSelected() 已选中输入}时，
     *         返回待输入
     */
    private Input<?> getInput(int index, boolean pendingFirst) {
        if (index < 0 || index >= this.inputs.size()) {
            return null;
        }

        Input<?> input = this.inputs.get(index);
        return pendingFirst && input == getSelected() ? getPending() : input;
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
    public List<String> getLatins() {
        return getInputs().stream()
                          .filter(Input::isLatin)
                          .map(Input::getText)
                          .filter(Objects::nonNull)
                          .map(StringBuilder::toString)
                          .collect(Collectors.toList());
    }

    /** 获取全部的拼音短语 */
    public List<List<PinyinWord>> getPinyinPhraseWords() {
        List<List<PinyinWord>> phrases = new ArrayList<>();

        List<PinyinWord> phrase = new ArrayList<>();
        for (int i = 0; i < this.inputs.size(); i++) {
            Input<?> input = getInput(i, true);

            if (isPinyinPhraseEndAt(i)) {
                if (!phrase.isEmpty()) {
                    phrases.add(phrase);
                    phrase = new ArrayList<>();
                }
            } else if (input.isPinyin()) {
                phrase.add((PinyinWord) input.getWord());
            }
        }

        if (!phrase.isEmpty()) {
            phrases.add(phrase);
        }

        return phrases;
    }

    /** 获取从指定输入开始及其之前的连续拼音字 */
    public List<PinyinWord> getPinyinPhraseWordsFrom(Input<?> fromInput) {
        int fromIndex = indexOf(fromInput, true);
        if (fromIndex < 0) {
            return List.of();
        }

        List<PinyinWord> words = new ArrayList<>();
        for (int i = fromIndex; i >= 0; i--) {
            Input<?> input = getInput(i, true);

            if (!input.isPinyin()) {
                break;
            }
            words.add((PinyinWord) input.getWord());
        }

        Collections.reverse(words);

        return words;
    }

    /**
     * 获取指定输入所在的拼音短语输入
     * <p/>
     * 段落为不包含逗号、分号、冒号、句号等符号的连续输入
     *
     * @return 不返回 null
     */
    public List<CharInput> getPinyinPhraseInputWhichContains(Input<?> fromInput) {
        int fromIndex = indexOf(fromInput, true);
        if (fromIndex < 0) {
            return List.of();
        }

        List<CharInput> phrase = new ArrayList<>(this.inputs.size());
        // @return false - 中止添加；true - 继续添加
        Function<Integer, Boolean> addInputToPhrase = (index) -> {
            if (isPinyinPhraseEndAt(index)) {
                return false;
            }

            Input<?> input = getInput(index, true);
            if (!input.isGap()) {
                phrase.add((CharInput) input);
            }
            return true;
        };

        // 先找之前的（不含起点）
        for (int i = fromIndex - 1; i >= 0; i--) {
            if (!addInputToPhrase.apply(i)) {
                break;
            }
        }
        Collections.reverse(phrase);

        // 再找之后的（包含起点）
        for (int i = fromIndex; i < this.inputs.size(); i++) {
            if (!addInputToPhrase.apply(i)) {
                break;
            }
        }

        return phrase;
    }

    /** 指定的输入是否代表段落结束 */
    private boolean isPinyinPhraseEndAt(int index) {
        Input<?> input = getInput(index, true);
        if (input == null || input.isSpace()) {
            return true;
        } else if (!input.isSymbol()) {
            return false;
        }

        String chars = input.getJoinedChars();
        // 英文结束标点符号左右两边为中文时，则从该符号处结束短语
        if (List.of(new String[] {
                ",", ".", ";", ":", "?", "!", //
        }).contains(chars)) {
            Input<?> left = getInput(index - 1, true);
            Input<?> right = getInput(index + 1, true);

            return (left == null || left.isPinyin()) && (right == null || right.isPinyin());
        }

        return List.of(new String[] {
                "，", "。", "；", "：", "？", "！", //
                "∶", "…", //
        }).contains(chars);
    }

    /** 获取已选中输入之前的输入 */
    public Input<?> getInputBeforeSelected() {
        return getInputBefore(getSelected());
    }

    /** 获取指定输入之前的输入 */
    public Input<?> getInputBefore(Input<?> input) {
        int index = indexOf(input);
        if (index <= 0) {
            return null;
        }
        return this.inputs.get(index - 1);
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

            sb.append(input.getText(getOption()));

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

        Input.Option option = getOption();
        if ((left.isMathExpr() && !left.isEmpty()) //
            || (right.isMathExpr() && !right.isEmpty())) {
            return true;
        } else if (left.isLatin()) {
            return !right.isSymbol();
        } else if (right.isLatin()) {
            return !left.isSymbol();
        } else if (left.isMathOperator() || right.isMathOperator()) {
            // 数学运算符前后都有空格
            return true;
        } else if (left.isTextOnlyWordSpell(option)) {
            return !right.isSymbol();
        } else if (right.isTextOnlyWordSpell(option)) {
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
        return indexOf(input, false);
    }

    /**
     * 获取指定输入所在的位置
     *
     * @param matchPending
     *         在确定位置时，是否匹配{@link #getPending() 待输入}，
     *         即，若指定输入为待输入，则返回{@link #getSelected() 已选中的输入}的位置
     */
    private int indexOf(Input<?> input, boolean matchPending) {
        if (input == null) {
            return -1;
        } else if (matchPending && getPending() == input) {
            input = getSelected();
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

    /** 选中指定的输入，但其待输入不变 */
    private void doSelect(Input<?> input) {
        this.cursor.select(input);
    }

    /** 选中指定的输入，但其待输入不变 */
    private void doSelect(int index) {
        Input<?> input = this.inputs.get(index);
        doSelect(input);
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

    @NonNull
    @Override
    public String toString() {
        return getText().toString();
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
