/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
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
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.MathExprInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.completion.InputCompletion;
import org.crazydan.studio.app.ime.kuaizi.core.input.completion.InputCompletions;
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
 * 所引用的输入，该方式还可用于判断待输入{@link #hasChangedPending() 是否已被修改}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class InputList {
    private final List<Input> inputs = new ArrayList<>();
    private final Cursor cursor = new Cursor();

    /** 输入补全 */
    private InputCompletions completions;

    private boolean frozen;
    private Input.Option inputOption;

    public InputList() {
        // 确保始终至少有一个 GapInput
        reset();
    }

    /** 是否已被冻结 */
    public boolean isFrozen() {
        return this.frozen;
    }

    /**
     * 输入列表是否为空
     * <p/>
     * 包含有效的输入时，才不为空
     */
    public boolean isEmpty() {
        for (Input input : this.inputs) {
            if (!(input instanceof GapInput) && !Input.isEmpty(input)) {
                return false;
            }
        }
        return hasEmptyPending();
    }

    public Input.Option getInputOption() {
        return this.inputOption;
    }

    public void setInputOption(Input.Option option) {
        this.inputOption = option;
    }

    // =================== Start: 整体性处理 ====================

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

    /** 是否冻结输入列表？ */
    public void freeze(boolean frozen) {
        this.frozen = frozen;
    }

    // =================== End: 整体性处理 ====================

    // ======================== Start: 处理输入补全 ==========================

    /** 获取输入补全的视图数据 */
    public List<InputCompletion.ViewData> getCompletionViewDataList() {
        if (this.completions == null) {
            return List.of();
        }
        return this.completions.data.stream().map((completion) -> {
            // Note: 使用输入选项，以确保汉字的繁/简转换符合应用的配置要求
            return InputCompletion.ViewData.create(completion, getInputOption());
        }).collect(Collectors.toList());
    }

    /**
     * 新建 {@link InputCompletions.Type#Latin} 类型的输入补全
     * <p/>
     * Note: 当前为单输入替换，仅需指定待应用补全的输入即可
     */
    public InputCompletions newLatinCompletions(Input input) {
        int index = getInputIndex(input);
        this.completions = new InputCompletions(InputCompletions.Type.Latin, index, index + 1);

        return this.completions;
    }

    /** 新建 {@link InputCompletions.Type#Phrase_Word} 类型的输入补全 */
    public InputCompletions newPhraseWordCompletions(Input start, Input end) {
        int startIndex = getInputIndex(start);
        int endIndex = getInputIndex(end);
        this.completions = new InputCompletions(InputCompletions.Type.Phrase_Word, startIndex, endIndex + 1);

        return this.completions;
    }

    /**
     * 检查输入补全，若针对{@link #getSelected() 当前选中输入}无有效的输入补全，
     * 则清空已有的输入补全数据，否则，保留现有的输入补全数据
     *
     * @return 若有有效的输入补全数据，则返回 <code>true</code>，
     * 否则，返回 <code>false</code>
     */
    public boolean verifyCompletions() {
        if (this.completions == null || this.frozen) {
            return false;
        }

        int selectedIndex = getSelectedIndex();
        boolean isGapSelected = isGapSelected();

        // 当前选中输入在补全的应用范围之外时，对其不做补全
        if (selectedIndex < this.completions.applyRange.start //
            || selectedIndex >= this.completions.applyRange.end //
        ) {
            this.completions = null;
            return false;
        }

        Input selected = getSelected();
        Input pending = getPending();

        switch (this.completions.type) {
            // 拉丁文补全仅针对拉丁文输入
            case Latin: {
                if ((Input.isEmpty(pending) && !CharInput.isLatin(selected)) //
                    || (!Input.isEmpty(pending) && !CharInput.isLatin(pending)) //
                ) {
                    this.completions = null;
                    return false;
                }
                break;
            }
            // 短语补全仅针对拼音输入
            case Phrase_Word: {
                // 拼音是单输入并直接确认的，因此，只有光标在 Gap 上时，才会构造输入补全
                if (!isGapSelected) {
                    this.completions = null;
                    return false;
                }
                break;
            }
        }
        return !this.completions.data.isEmpty();
    }

    /** 应用指定位置的输入补全。应用后，当前的输入补全将被清空，并将光标移到补全内容的尾部 */
    public void applyCompletion(int position) {
        InputCompletions completions = this.completions;
        this.completions = null;

        int selectedIndex = getSelectedIndex();
        boolean isGapSelected = isGapSelected();
        int rangeStart = completions.applyRange.start;
        int rangeEnd = completions.applyRange.end;

        // 先确认当前的待输入，以确保补全位置是确定的
        confirmPending();
        // 补充在 Gap 上的待输入确认后的补全应用位置的偏移量
        // Note: 当前选中输入的位置必然已在补全的应用范围内，否则，是不会气泡显示补全内容的
        if (isGapSelected) {
            rangeStart += selectedIndex == rangeStart ? 1 : 0;
            rangeEnd += 1; // Note: 当光标在列表尾部时，该值可能会超出列表长度
        }

        // 在补全应用范围内的非空 CharInput 列表
        List<CharInput> charInputsInRange = CollectionUtils.subList(this.inputs, rangeStart, rangeEnd)
                                                           .stream()
                                                           .filter(input -> input instanceof CharInput
                                                                            && !Input.isEmpty(input))
                                                           .map(input -> (CharInput) input)
                                                           .collect(Collectors.toList());

        InputCompletion completion = completions.data.get(position);
        // 在应用范围内的输入，实施补全替换
        for (int i = 0; i < charInputsInRange.size(); i++) {
            CharInput target = charInputsInRange.get(i);
            CharInput source = completion.inputs.get(i);

            switch (completions.type) {
                case Latin: {
                    select(target);
                    withPending(source);
                    confirmPending();
                    break;
                }
                case Phrase_Word: {
                    if (!target.isWordConfirmed() && source.hasWord()) {
                        target.setWord(source.getWord());
                        // 直接确认输入字，以避免后续进行的短语预测对其的修改
                        target.confirmWord();
                    }
                    break;
                }
            }
        }

        // 在应用范围之外多余的补全，做输入新增：倒序新增，以确保新增位置始终不变
        for (int i = completion.inputs.size() - 1; i >= charInputsInRange.size(); i--) {
            CharInput source = completion.inputs.get(i);

            select(rangeEnd);
            withPending(source);
            confirmPending();
        }

        // 确保光标移动到后续位置
        confirmPendingAndSelectNext();
    }

    // ======================== End: 处理输入补全 ==========================

    // ======================== Start: 处理待输入 ==========================

    /**
     * 获取{@link #getSelected() 当前选中输入}的待输入
     * <p/>
     * 有可能为 {@link MathExprInput}，默认均为 {@link CharInput}
     *
     * @return 不会为 null
     */
    public Input getPending() {
        return this.cursor.pending;
    }

    /**
     * 获取{@link #getSelected() 当前选中输入}的待输入
     * <p/>
     * 待输入必须为 {@link CharInput} 类型
     */
    public CharInput getCharPending() {
        Input pending = getPending();
        assert pending instanceof CharInput;

        return (CharInput) pending;
    }

    /**
     * 获取{@link #getSelected() 当前选中输入}的待输入
     * <p/>
     * 待输入必须为 {@link MathExprInput} 类型
     */
    public MathExprInput getMathExprPending() {
        Input pending = getPending();
        assert pending instanceof MathExprInput;

        return (MathExprInput) pending;
    }

    /** 获取指定输入上的待输入 */
    public Input getPendingOn(Input input) {
        return isSelected(input) ? getPending() : null;
    }

    /** 获取指定输入上的非空待输入 */
    public Input getNoneEmptyPendingOn(Input input) {
        Input pending = getPendingOn(input);
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

    /**
     * 当前的待输入是否已被修改
     * <p/>
     * 待输入内容与 {@link #getSelected()} 内容不一致，则表示其已被修改
     */
    public boolean hasChangedPending() {
        Input selected = getSelected();
        Input pending = getPending();

        return !selected.equals(pending);
    }

    /**
     * 为{@link #getSelected() 当前选中输入}创建空白的 {@link CharInput} 类型的{@link #getPending() 待输入}
     * <p/>
     * Note: 现有的待输入不做{@link #confirmPending() 确认}
     */
    public CharInput newCharPending() {
        CharInput pending = new CharInput();
        withPending(pending);

        return pending;
    }

    /**
     * 为{@link #getSelected() 当前选中输入}创建空白的 {@link MathExprInput} 类型的{@link #getPending() 待输入}
     * <p/>
     * Note: 现有的待输入不做{@link #confirmPending() 确认}
     */
    public MathExprInput newMathExprPending() {
        MathExprInput pending = new MathExprInput();
        withPending(pending);

        return pending;
    }

    /**
     * 使用{@link #getSelected() 当前选中输入}的{@link Input#copy() 副本}重建其{@link #getPending() 待输入}
     * <p/>
     * Note: 现有的待输入不做{@link #confirmPending() 确认}
     */
    public Input newSelectedPending() {
        Input pending = getSelected().copy();
        withPending(pending);

        return pending;
    }

    /**
     * 将{@link #getSelected() 当前选中输入}的{@link #getPending() 待输入}设置为指定输入
     * <p/>
     * Note: 现有的待输入不做{@link #confirmPending() 确认}
     */
    private void withPending(Input input) {
        this.cursor.withPending(input);
    }

    /**
     * 丢弃{@link #getSelected() 当前选中输入}的{@link #getPending() 待输入}
     * <p/>
     * 根据当前选中输入的类型构造新的空白待输入，若当前选中输入为 {@link GapInput}，则构造
     * {@link CharInput} 类型的待输入
     */
    public void dropPending() {
        Input selected = getSelected();

        if (selected instanceof MathExprInput) {
            newMathExprPending();
        } else {
            newCharPending();
        }
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
        int confirmedPendingIndex = confirmPending();
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
     * 与 {@link #confirmPending(boolean)} 的逻辑一致，
     * 只是待输入的 {@link Input#confirm()} 接口将被调用，
     * 也即，始终传参 <code>false</code>
     */
    public int confirmPending() {
        return confirmPending(false);
    }

    /**
     * 确认当前待输入，并返回确认后的{@link #getSelectedIndex() 当前选中输入位置}
     * <p/>
     * <ul>
     * <li>待输入为空时，不做处理；</li>
     * <li>原位置为 Gap 时，插入待输入；</li>
     * <li>原位置为字符输入时，将其替换为待输入；</li>
     * </ul>
     *
     * @param notConfirmPendingSelf
     *         是否不调用待输入的 {@link Input#confirm()} 接口？
     *         对于 {@link MathExprInput}，调用该接口会使得其内嵌输入列表的待输入也被确认，
     *         这在输入过程中手动选择其他输入时，会造成点击时的视图状态与确认后的数据状态不一致，
     *         因此，需保持内嵌输入列表的待输入状态
     * @return 若未作确认（空白待输入无需确认），则返回 <code>-1</code>，否则，返回当前的已选择输入的位置
     */
    public int confirmPending(boolean notConfirmPendingSelf) {
        int selectedIndex = getSelectedIndex();
        if (selectedIndex < 0) {
            return -1;
        }

        Input selected = getSelected();
        Input pending = getPending();

        if (Input.isEmpty(pending)) {
            // 若当前为空白算术输入，则直接将其移除
            if (selected instanceof MathExprInput && Input.isEmpty(selected)) {
                removeNonGapInputAt(selectedIndex);

                // 选中相邻的后继 Gap
                doSelect(selectedIndex - 1);
            }

            // Note：当前待输入可能为空白的算术输入，
            // 为了确保确认后能继续接受普通输入，需重置当前的待输入
            newCharPending();

            return -1;
        }

        // 先由待输入做其内部确认
        if (!notConfirmPendingSelf) {
            pending.confirm();
        }

        if (selected instanceof GapInput) {
            // Note: 新的 Gap 位置自动后移，故无需更新光标的选中对象
            Input gap = new GapInput();

            this.inputs.addAll(selectedIndex, Arrays.asList(gap, pending));
        } else {
            // 保持对配对符号的引用
            if (selected instanceof CharInput && pending instanceof CharInput) {
                ((CharInput) pending).setPair(((CharInput) selected).getPair());
            }

            this.inputs.set(selectedIndex, pending);
        }

        doSelect(pending);

        return getSelectedIndex();
    }

    // ======================== End: 处理待输入 ==========================

    // ======================== Start: 处理当前选中输入 ==========================

    /**
     * 获取当前选中输入，即，{@link Cursor 输入光标}指向的 {@link #getInputs()} 中的输入，
     * 其将通过{@link #getPending() 待输入}接受输入按键
     *
     * @return 始终不为 null，可能为 {@link GapInput}
     */
    public Input getSelected() {
        return this.cursor.selected;
    }

    /** 获取{@link #getSelected() 当前选中输入}的位置 */
    public int getSelectedIndex() {
        Input selected = getSelected();
        return getInputIndex(selected);
    }

    /** 指定输入是否已选中 */
    public boolean isSelected(Input input) {
        return getSelected() == input;
    }

    /** {@link #getSelected() 当前选中输入}是否为 Gap 位 */
    public boolean isGapSelected() {
        return getSelected() instanceof GapInput;
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

        if (selected instanceof CharInput) {
            ((CharInput) selected).clearPair();
        }
    }

    /**
     * 删除{@link #getSelected() 当前选中输入}
     * <p/>
     * 对于 Gap 输入，则仅清空其{@link #getPending() 待输入}内容
     */
    public void deleteSelected() {
        Input selected = getSelected();

        if (!(selected instanceof GapInput)) {
            doDeleteBackward(false);
        }

        // 重建待输入
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

    /** 判断当前输入列表是否有唯一的配对输入 */
    public boolean hasOnlyOnePairInputs() {
        if (this.inputs.size() == 5) {
            Input first = this.inputs.get(1);
            Input middle = getNoneEmptyPendingOrSelf(this.inputs.get(2));
            Input last = this.inputs.get(3);

            return (middle == null || middle instanceof GapInput) //
                   && first instanceof CharInput //
                   && last instanceof CharInput //
                   && ((CharInput) first).getPair() == last //
                   && ((CharInput) last).getPair() == first;
        }
        return false;
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

    /** 获取全部 {@link CharInput}：不包含 {@link #getCharPending() 待输入} */
    public List<CharInput> getCharInputs() {
        return this.inputs.stream()
                          .filter(input -> input instanceof CharInput)
                          .map(input -> (CharInput) input)
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
     *         即，若指定输入为待输入，则返回{@link #getSelected() 当前选中输入}的位置
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
        for (Input input : this.inputs) {
            if (input instanceof CharInput) {
                return (CharInput) input;
            }
        }
        return null;
    }

    public CharInput getLastCharInput() {
        for (int i = this.inputs.size() - 1; i >= 0; i--) {
            Input input = this.inputs.get(i);
            if (input instanceof CharInput) {
                return (CharInput) input;
            }
        }
        return null;
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
        if (byStep && CharInput.isLatin(current)) {
            CharInput input = (CharInput) current;
            if (input.countKeys() > 1) {
                input.dropLastKey();
                return;
            }
        }

        // 删除当前光标之前的输入或其待输入
        if (selected instanceof GapInput) {
            if (selectedIndex > 0 && hasEmptyPending()) {
                int prevIndex = selectedIndex - 1;
                Input prev = this.inputs.get(prevIndex);

                if (CharInput.isLatin(prev) && ((CharInput) prev).countKeys() > 1) {
                    doSelect(prev);
                } else {
                    removePairCharInputOf(prev);

                    // Note：当光标在 中间没有字符输入的配对符号 的中间位置时，
                    // 先删除其后的 配对字符输入 会导致该光标也被删除，
                    // 且其所在位置由 被删除的配对字符输入 的后继 Gap 填充，
                    // 故而，新光标位置不变但对应的 Gap 引用需更新
                    if (getSelectedIndex() < 0) {
                        doSelect(this.inputs.get(selectedIndex));
                    }

                    removeNonGapInput(prev);
                }
            } else {
                dropPending();
            }
        }
        // 删除当前选中输入及其 Gap 位
        else {
            removePairCharInputOf(selected);

            // Note：当选中 左侧配对符号输入 且 配对符号中间 没有其他字符输入时，
            // 先删除右侧的 配对符号输入 会导致待选中的 Gap 一并被删除，
            // 且该光标所在位置由 被删除的配对字符输入 的后继 Gap 填充，
            // 故而，新光标位置不变但对应的 Gap 引用需更新
            selectedIndex = getSelectedIndex();
            Input newSelected = this.inputs.get(selectedIndex + 1);

            removeNonGapInput(selected);

            // 再将当前光标后移
            doSelect(newSelected);
        }
    }

    /** 删除指定的非 Gap 输入 */
    private void removeNonGapInput(Input input) {
        int index = getInputIndex(input);

        removeNonGapInputAt(index);
    }

    /** 删除指定位置的非 Gap 输入 */
    private void removeNonGapInputAt(int index) {
        if (index <= 0) {
            return;
        }

        Input input = this.inputs.get(index);
        if (input instanceof GapInput) {
            return;
        }

        // 输入位
        this.inputs.remove(index);
        // Gap 位
        this.inputs.remove(index - 1);
    }

    /** 删除指定输入的{@link CharInput#getPair() 配对输入} */
    private void removePairCharInputOf(Input input) {
        if (!(input instanceof CharInput)) {
            return;
        }

        CharInput pairInput = ((CharInput) input).getPair();

        int pairInputIndex = getInputIndex(pairInput);
        if (pairInputIndex < 0) {
            return;
        }

        pairInput.clearPair();
        removeNonGapInputAt(pairInputIndex);
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
        if (!(input instanceof GapInput)) {
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
        else if (CharInput.isSpace(left) || CharInput.isSpace(right)) {
            return false;
        }

        // Note: 算术输入的结构更复杂，优先检查该类型
        if ((left instanceof MathExprInput && !Input.isEmpty(left)) //
            || (right instanceof MathExprInput && !Input.isEmpty(right)) //
        ) {
            return true;
        }
        // 数学运算符左右都需有空格
        else if (CharInput.isMathOp(left) || CharInput.isMathOp(right)) {
            return true;
        } //
        else if (CharInput.isLatin(left)) {
            return !CharInput.isSymbol(right);
        } //
        else if (CharInput.isLatin(right)) {
            return !CharInput.isSymbol(left);
        }

        Input.Option option = getInputOption();
        if (CharInput.useWordSpellAsText(left, option)) {
            return !CharInput.isSymbol(right);
        } //
        else if (CharInput.useWordSpellAsText(right, option)) {
            return !CharInput.isSymbol(left);
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
        return this.inputs.stream() //
                          .filter(CharInput::isEmoji) //
                          .map((input) -> (CharInput) input) //
                          .map((input) -> {
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
                          .filter(CharInput::isLatin)
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
            } else if (CharInput.isPinyin(input)) {
                phrase.add((PinyinWord) ((CharInput) input).getWord());
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

            if (!CharInput.isPinyin(input)) {
                break;
            }
            words.add((PinyinWord) ((CharInput) input).getWord());
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
            if (input instanceof CharInput && !Input.isEmpty(input)) {
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
        if (input instanceof GapInput) {
            return false;
        } else if (!(input instanceof CharInput) || CharInput.isSpace(input)) {
            return true;
        } else if (!CharInput.isSymbol(input)) {
            return false;
        }

        String chars = ((CharInput) input).getJoinedKeyChars();
        // 英文结束标点符号左右两边为中文时，则从该符号处结束短语
        if (List.of(new String[] {
                ",", ".", ";", ":", "?", "!", //
        }).contains(chars)) {
            Input left = getInput(index - 1, true);
            Input right = getInput(index + 1, true);

            return (left == null || CharInput.isPinyin(left)) //
                   && (right == null || CharInput.isPinyin(right));
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
        /** 光标位置指向的已选中输入 */
        private Input selected;
        /** 光标位置待插入的输入，不能为 {@link GapInput} */
        private Input pending;

        /** 替换为指定的 {@link Cursor}，使二者数据相同，其属性的引用相同 */
        private void replaceBy(Cursor source) {
            this.selected = source.selected;
            this.pending = source.pending;
        }

        /** 重置 */
        private void reset() {
            this.selected = null;
            this.pending = null;
        }

        /**
         * 选中指定输入，并将其复制一份作为其待输入，
         * 若输入为 Gap，则为其创建空白的 {@link CharInput} 类型的待输入
         */
        public void select(Input input) {
            assert input != null;

            Input pending = input instanceof GapInput ? new CharInput() : input.copy();
            this.selected = input;

            withPending(pending);
        }

        /** 设置指定的待输入（不能为 {@link GapInput}） */
        public void withPending(Input input) {
            assert input != null;
            assert !(input instanceof GapInput);

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
