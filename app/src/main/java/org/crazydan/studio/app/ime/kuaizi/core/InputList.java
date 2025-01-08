/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.GapInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputCompletion;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.InputWordKey;

/**
 * 输入列表
 * <p/>
 * 输入列表由零个或多个 {@link Input} 组成，代表用户当前已输入和正在输入的内容
 * <p/>
 * {@link GapInput} 为两个可见输入之间的间隔，代表输入的一个插入位，也是一个光标位，
 * 其与可见输入成对添加和删除
 * <p/>
 * 通过 {@link #cursor} 指示当前的输入位置，其中，{@link Cursor#selected}
 * 引用的是 {@link #inputs} 列表中的输入对象（使用引用可消除位置变动所产生的位置同步需求），
 * 其引用的可以是 Gap，也可以是可见输入，但正在输入的内容是记录在 {@link Cursor#pending}
 * 中的，只有在输入完成并 {@link #confirmPending()} 后才会替换 {@link Cursor#selected}
 * 所引用的输入
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class InputList {
    private final List<Input> inputs = new ArrayList<>();
    private final Cursor cursor = new Cursor();
    /** 输入补全 */
    private final List<InputCompletion> completions = new ArrayList<>();

    private Input.Option inputOption;

    public InputList() {
        // 确保始终至少有一个 GapInput
        reset();
    }

    /**
     * 输入列表是否为空
     * <p/>
     * 包含有效的输入时，才不为空
     */
    public boolean isEmpty() {
        for (Input input : this.inputs) {
            if (!input.isGap() && !input.isEmpty()) {
                return false;
            }
        }
        return hasEmptyPending();
    }

    // =================== Start: 数据初始化 ====================

    public Input.Option getInputOption() {
        return this.inputOption;
    }

    public void setInputOption(Input.Option option) {
        this.inputOption = option;
    }

    /** 创建副本，对副本的第一层属性做变更不影响原始对象 */
    public InputList copy() {
        InputList target = new InputList();
        target.replaceBy(this);

        return target;
    }

    /** 替换为指定的 {@link InputList}，使二者数据相同，但第一层属性的引用是不相同的，可被直接修改 */
    public void replaceBy(InputList source) {
        this.inputs.clear();
        this.inputs.addAll(source.inputs);

        this.cursor.replaceBy(source.cursor);
    }

    /** 重置 */
    public void reset() {
        this.inputOption = null;

        this.inputs.clear();
        this.cursor.reset();

        // 始终包含并选中一个 Gap 位
        Input gap = new GapInput();
        this.inputs.add(gap);
        doSelect(gap);
    }

    // =================== End: 数据初始化 ====================

    // ======================== Start: 处理输入补全 ==========================

    /** 获取输入补全的视图数据 */
    public List<InputCompletion.ViewData> getCompletionViewDataList() {
        return this.completions.stream().map((completion) -> {
            // Note: 使用输入选项，以确保汉字的繁/简转换符合应用的配置要求
            return InputCompletion.ViewData.create(completion, getInputOption());
        }).collect(Collectors.toList());
    }

    /** 添加输入补全 */
    public void addCompletion(InputCompletion completion) {
        this.completions.add(completion);
    }

    /** 清空输入补全 */
    public void clearCompletions() {
        this.completions.clear();
    }

    /** 应用指定位置的输入补全。应用后，当前的输入补全将被清空，并将光标移到补全内容的尾部 */
    public void applyCompletion(int position) {
        InputCompletion completion = this.completions.get(position);
        int replaceEndIndex = getSelectedIndex();

        int total = completion.inputs.size();
        for (int i = 0; i < total; i++) {
            CharInput input = completion.inputs.get(i);
            // Note: 从补全开始位置到当前选中位置的输入执行替换，之后的输入则执行插入
            int index = completion.startPosition + i;
            if (index <= replaceEndIndex) {
                select(index);
            }

            withPending(input);
            confirmPending();
        }
        confirmPendingAndSelectNext();

        clearCompletions();
    }

    // ======================== End: 处理输入补全 ==========================

    // ======================== Start: 处理待输入 ==========================

    /** 获取待输入 */
    public CharInput getPending() {
        return this.cursor.pending;
    }

    /** 获取指定输入上的待输入 */
    public CharInput getPendingOn(Input input) {
        return getSelected() == input ? getPending() : null;
    }

    /** 获取指定输入上的非空待输入 */
    public CharInput getNoneEmptyPendingOn(Input input) {
        CharInput pending = getPendingOn(input);

        return Input.isEmpty(pending) ? null : pending;
    }

    /** 若存在则获取非空待输入，否则，返回输入自身 */
    private Input getNoneEmptyPendingOrSelf(Input input) {
        Input pending = getNoneEmptyPendingOn(input);
        return pending != null ? pending : input;
    }

    /** 当前的待输入是否为空 */
    public boolean hasEmptyPending() {
        return Input.isEmpty(getPending());
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

    /** 丢弃当前的待输入 */
    public void dropPending() {
        newPending();
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

        Input newSelected = this.inputs.get(confirmedPendingIndex + offset);
        doSelect(newSelected);
    }

    /** {@link #confirmPending() 确认当前待输入}，并{@link #select(Input) 选中指定输入} */
    public void confirmPendingAndSelect(Input input) {
        confirmPendingAndSelect(input, false);
    }

    /**
     * {@link #confirmPending() 确认当前待输入}，并{@link #select(Input) 选中指定输入}
     *
     * @param force
     *         若为 true，则对于已选中的 input，将强制重新{@link #doSelect 选择}
     */
    public void confirmPendingAndSelect(Input input, boolean force) {
        confirmPending();

        int index = getInputIndex(input);
        if (index < 0) {
            return;
        }

        if (force || !isSelected(input)) {
            doSelect(input);
        }
    }

    /** {@link #confirmPending() 确认当前待输入}，并{@link #selectLast() 选中最后一个输入} */
    public void confirmPendingAndSelectLast() {
        confirmPending();
        selectLast();
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

        Input selected = getSelected();
        CharInput pending = getPending();

        if (Input.isEmpty(pending)) {
            // 若当前为空白算术输入，则直接将其移除
            if (selected.isMathExpr() && selected.isEmpty()) {
                removeCharInputAt(selectedIndex);
                // 选中相邻的后继 Gap
                doSelect(selectedIndex - 1);
            }

            // Note：当前待输入可能为空白的算术输入，
            // 为了确保确认后能继续接受普通输入，需重置当前的待输入
            newPending();

            return -1;
        }

        // 先由待输入做其内部确认
        pending.confirm();

        if (selected.isGap()) {
            // Note: 新的 Gap 位置自动后移，故无需更新光标的选中对象
            Input gap = new GapInput();

            this.inputs.addAll(selectedIndex, Arrays.asList(gap, pending));
        } else {
            // 保持对配对符号的引用
            pending.setPair(((CharInput) selected).getPair());

            this.inputs.set(selectedIndex, pending);
        }

        doSelect(pending);

        return getSelectedIndex();
    }

    // ======================== End: 处理待输入 ==========================

    // ======================== Start: 处理当前选中输入 ==========================

    /** 获取已选中的输入 */
    public Input getSelected() {
        return this.cursor.selected;
    }

    /** 获取已选中输入的位置 */
    public int getSelectedIndex() {
        Input selected = getSelected();
        return getInputIndex(selected);
    }

    /** 指定输入是否已选中 */
    public boolean isSelected(Input input) {
        return getSelected() == input;
    }

    /** 当前选中的是否为光标位 */
    public boolean isGapSelected() {
        return Input.isGap(getSelected());
    }

    /** {@link #select(int) 选中指定的输入} */
    public void select(Input input) {
        int index = getInputIndex(input);
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
        Input input = getInput(index);
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
    public Input selectNextFirstMatched(Predicate<Input> filter) {
        int index = getSelectedIndex();
        int lastIndex = this.inputs.size() - 1;
        if (index >= lastIndex) {
            return null;
        }

        for (int i = index + 1; i <= lastIndex; i++) {
            Input next = this.inputs.get(i);

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
    public Input selectLast() {
        Input last = getLastInput();

        if (!isSelected(last)) {
            doSelect(last);
        }
        return last;
    }

    /** 清除在当前输入上的{@link CharInput#getPair() 配对符号输入} */
    public void clearPairOnSelected() {
        Input selected = getSelected();

        if (!Input.isGap(selected)) {
            ((CharInput) selected).clearPair();
        }
    }

    /** 删除当前选中的输入：对于 Gap 位置，仅删除其正在输入的内容 */
    public void deleteSelected() {
        Input selected = getSelected();

        if (!Input.isGap(selected)) {
            doDeleteBackward(false);
        }

        dropPending();
    }

    /** 选中指定的输入，并重建其待输入 */
    private void doSelect(Input input) {
        this.cursor.select(input);
    }

    /** 选中指定的输入，并重建其待输入 */
    private void doSelect(int index) {
        Input input = this.inputs.get(index);
        doSelect(input);
    }

    // ======================== End: 处理当前选中输入 ==========================

    // ======================== Start: 处理普通输入 ==========================

    /** 是否包含指定的输入 */
    public boolean hasInput(Input input) {
        return getInputIndex(input, true) >= 0;
    }

    /** 获取全部 {@link Input}，用于遍历列表中的输入 */
    public List<Input> getInputs() {
        return Collections.unmodifiableList(this.inputs);
    }

    /** 获取全部 {@link CharInput} */
    public List<CharInput> getCharInputs() {
        return this.inputs.stream()
                          .filter(input -> !input.isGap())
                          .map(input -> ((CharInput) input))
                          .collect(Collectors.toList());
    }

    /** 获取指定输入的位置 */
    public int getInputIndex(Input input) {
        return getInputIndex(input, false);
    }

    /**
     * 获取指定输入所在的位置
     *
     * @param matchPending
     *         在确定位置时，是否匹配{@link #getPending() 待输入}，
     *         即，若指定输入为待输入，则返回{@link #getSelected() 已选中的输入}的位置
     */
    private int getInputIndex(Input input, boolean matchPending) {
        if (input == null) {
            return -1;
        } else if (matchPending && getPending() == input) {
            input = getSelected();
        }

        // Note: 这里需要做对象引用的判断，以避免内容相同的输入被判定为已选择
        return CollectionUtils.indexOfRef(this.inputs, input);
    }

    /** 获取指定位置的输入 */
    public Input getInput(int index) {
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
    private Input getInput(int index, boolean pendingFirst) {
        if (index < 0 || index >= this.inputs.size()) {
            return null;
        }

        Input input = this.inputs.get(index);
        return pendingFirst && input == getSelected() ? getPending() : input;
    }

    public Input getFirstInput() {
        return CollectionUtils.first(this.inputs);
    }

    public Input getLastInput() {
        return CollectionUtils.last(this.inputs);
    }

    public CharInput getFirstCharInput() {
        return CollectionUtils.first(getCharInputs());
    }

    public CharInput getLastCharInput() {
        return CollectionUtils.last(getCharInputs());
    }

    /** 获取指定输入之前的输入 */
    public Input getInputBefore(Input input) {
        int index = getInputIndex(input);
        if (index <= 0) {
            return null;
        }
        return this.inputs.get(index - 1);
    }

    /** 获取已选中输入之前的输入 */
    public Input getInputBeforeSelected() {
        return getInputBefore(getSelected());
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

        Input selected = getSelected();
        Input pending = getPending();
        Input current = !Input.isEmpty(pending) ? pending : selected;
        // 逐字删除拉丁字符输入的最后一个字符
        if (byStep && current.isLatin() && current.getKeys().size() > 1) {
            current.dropLastKey();
            return;
        }

        // 删除当前光标之前的 输入和占位
        if (selected.isGap()) {
            if (selectedIndex > 0 && hasEmptyPending()) {
                int prevIndex = selectedIndex - 1;
                Input prev = this.inputs.get(prevIndex);

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
            Input newSelected = this.inputs.get(selectedIndex + 1);

            removeCharInput(selected);

            // 再将当前光标后移
            doSelect(newSelected);
        }
    }

    /** 删除指定的字符输入（包括与其配对的前序 Gap 位） */
    private void removeCharInput(Input input) {
        int index = !Input.isGap(input) ? getInputIndex(input) : -1;

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
        if (!Input.isGap(input)) {
            CharInput pairInput = ((CharInput) input).getPair();

            int pairInputIndex = getInputIndex(pairInput);
            if (pairInputIndex < 0) {
                return;
            }

            pairInput.clearPair();
            removeCharInputAt(pairInputIndex);
        }
    }

    // ======================== End: 处理普通输入 ==========================

    // ======================== Start: 处理输入间的空格 ==========================

    /**
     * 判断指定位置是否需要添加 Gap 空格
     * <p/>
     * 需要添加 Gap 空格的输入，在最终输出 {@link #getText() 文本内容} 时，
     * 会在其所在位置添加一个空格，用以表示两个输入的内容是不相连的
     * <p/>
     * 由于 Gap 空格是出现在两个可见输入之间的，因此，只需要为 {@link GapInput}
     * 添加空格，进而只需要判断在 {@link GapInput} 左右两侧的输入是否需要间隔即可
     * <p/>
     * 注意，该接口只针对已经确认输入后的 {@link #inputs} 列表，
     * 若存在待确认输入，则需要通过 {@link #needGapSpace(Input, Input)}
     * 确定待输入左右两侧是否需要添加空格
     */
    public boolean needGapSpace(int i) {
        int total = this.inputs.size();
        // 在首/尾位置均不需要空格
        if (i <= 0 || i >= total - 1) {
            return false;
        }

        Input input = this.inputs.get(i);
        if (!input.isGap()) {
            return false;
        }

        Input left = this.inputs.get(i - 1);
        Input right = this.inputs.get(i + 1);

        return needGapSpace(left, right);
    }

    /** 判断指定的左右两个输入之间是否需要 Gap 空格 */
    public boolean needGapSpace(Input left, Input right) {
        left = getNoneEmptyPendingOrSelf(left);
        right = getNoneEmptyPendingOrSelf(right);

        if (left == null || right == null) {
            return false;
        }
        // 已经有显式的空格，则不需要再添加空格
        else if (left.isSpace() || right.isSpace()) {
            return false;
        }

        // Note: 算术输入的结构更复杂，优先检查该类型
        if ((left.isMathExpr() && !left.isEmpty()) //
            || (right.isMathExpr() && !right.isEmpty())) {
            return true;
        }
        // 数学运算符左右都需有空格
        else if (left.isMathOp() || right.isMathOp()) {
            return true;
        } else if (left.isLatin()) {
            return !right.isSymbol();
        } else if (right.isLatin()) {
            return !left.isSymbol();
        }

        Input.Option option = getInputOption();
        if (left.isTextOnlyWordSpell(option)) {
            return !right.isSymbol();
        } else if (right.isTextOnlyWordSpell(option)) {
            return !left.isSymbol();
        }
        return false;
    }

    // ======================== End: 处理输入间的空格 ==========================

    // ====================== Start: 处理输入结果 ======================

    /** 获取输入文本内容：必须先确认当前输入，否则，会出现不能正确添加输入间的空格的问题 */
    public StringBuilder getText() {
        Input.Option option = getInputOption();
        return getText(option);
    }

    /** 获取输入文本内容：必须先确认当前输入，否则，会出现不能正确添加输入间的空格的问题 */
    public StringBuilder getText(Input.Option option) {
        StringBuilder sb = new StringBuilder();

        int total = this.inputs.size();
        for (int i = 0; i < total; i++) {
            Input input = this.inputs.get(i);

            sb.append(input.getText(option));

            if (needGapSpace(i)) {
                sb.append(" ");
            }
        }
        return sb;
    }

    /** 获取全部的表情符号 */
    public List<InputWord> getEmojis() {
        return this.inputs.stream().filter(Input::isEmoji).map((input) -> {
            InputWord word = input.getWord();
            Key key = input.getFirstKey();

            return word != null //
                   ? word : key instanceof InputWordKey //
                            ? ((InputWordKey) key).word : null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /** 获取拉丁文输入 */
    public List<String> getLatins() {
        return this.inputs.stream()
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
            Input input = getInput(i, true);

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
    public List<PinyinWord> getPinyinPhraseWordsFrom(Input fromInput) {
        int fromIndex = getInputIndex(fromInput, true);
        if (fromIndex < 0) {
            return List.of();
        }

        List<PinyinWord> words = new ArrayList<>();
        for (int i = fromIndex; i >= 0; i--) {
            Input input = getInput(i, true);

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
    public List<CharInput> getPinyinPhraseInputWhichContains(Input fromInput) {
        int fromIndex = getInputIndex(fromInput, true);
        if (fromIndex < 0) {
            return List.of();
        }

        List<CharInput> phrase = new ArrayList<>(this.inputs.size());
        // @return false - 中止添加；true - 继续添加
        Function<Integer, Boolean> addInputToPhrase = (index) -> {
            if (isPinyinPhraseEndAt(index)) {
                return false;
            }

            Input input = getInput(index, true);
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
        Input input = getInput(index, true);
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
            Input left = getInput(index - 1, true);
            Input right = getInput(index + 1, true);

            return (left == null || left.isPinyin()) && (right == null || right.isPinyin());
        }

        return List.of(new String[] {
                "，", "。", "；", "：", "？", "！", //
                "∶", "…", //
        }).contains(chars);
    }

    @Override
    public String toString() {
        return getText().toString();
    }

    // ====================== End: 处理输入结果 ======================

    // ========================= Start: 嵌套使用 =======================

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

    // ========================= End: 嵌套使用 =======================

    private static class Cursor {
        /** 光标位置已选中的输入 */
        private Input selected;
        /** 光标位置待插入的输入 */
        private CharInput pending;

        /** 替换为指定的 {@link Cursor}，使二者数据相同，其属性的引用相同 */
        public void replaceBy(Cursor source) {
            this.selected = source.selected;
            this.pending = source.pending;
        }

        /** 重置 */
        public void reset() {
            this.selected = null;
            this.pending = null;
        }

        /**
         * 选中指定输入，并将其复制一份作为其待输入，
         * 若输入为 Gap，则为其创建空的待输入
         */
        public void select(Input input) {
            CharInput pending = Input.isGap(input) ? new CharInput() : (CharInput) input.copy();

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
}
