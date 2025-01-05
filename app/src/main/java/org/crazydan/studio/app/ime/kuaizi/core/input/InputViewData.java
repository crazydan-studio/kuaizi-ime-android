/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.core.input;

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.core.Input;

/**
 * {@link Input} 的视图数据，仅用于构造视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-09
 */
public class InputViewData {
    public enum Type {
        /** 默认均为 {@link CharInput} */
        Char,
        /** 占位输入：{@link Input#isGap()} */
        Gap,
        /** 空格输入：{@link Input#isSpace()} */
        Space,
        /** 算术输入：{@link Input#isMathExpr()} */
        MathExpr
    }

    /** 当前输入在 {@link InputList} 中的位置（序号） */
    public final int position;
    /** 当前输入的类型 */
    public final Type type;

    /**
     * 当前输入是否为待输入
     * <p/>
     * 只有含有{@link InputList#getPending() 待输入}的才是待输入，
     * 并且，在 {@link InputList} 中必然存在唯一的待输入
     */
    public final boolean pending;
    /**
     * 当前输入是否已被选中
     * <p/>
     * 在配对符号中，若其中一个为{@link #pending 待输入}，
     * 则另一个也会被选中，否则，只有待输入才会被选中
     */
    public final boolean selected;

    /** 当前输入所需的空白数 */
    public final int gapSpaces;
    /** 嵌套的输入列表：只有{@link MathExprInput 算术输入}才存在输入列表嵌套 */
    public final List<InputViewData> inputs;

    /** 当前输入的候选字 */
    public final String word;
    /** 当前输入的候选字读音 */
    public final String spell;
    /*
    word = data.word != null ? data.word.value : input.getJoinedChars();
    spell = data.word instanceof PinyinWord ? ((PinyinWord) data.word).spell.value : null;
    if (option != null && data.word != null) {
            value = input.getText(option).toString();

            if (spell != null && value.contains(spell)) {
                spell = null;
            }
        }
    * */

    InputViewData(
            Input input, CharInput pending, int position, //
            Input.Option option, boolean chosen, int gapSpaces
    ) {
    }

    /** 创建 {@link InputViewData} */
    public static InputViewData create(InputList inputList, Input.Option option, int position) {
        // TODO 被 selected 的输入，使用其 pending 输入填充输入信息
        Input input = inputList.getInput(position);
        Input preInput = inputList.getInput(position - 1);

        CharInput pending = inputList.getPendingOn(input);
        CharInput prePending = inputList.getPendingOn(preInput);

        MathExprInput mathExprInput = tryGetMathExprInput(inputList, input);

        boolean selected = needToBeSelected(inputList, input);
        boolean needGapSpace = inputList.needGapSpace(input);

        // 前序正在输入的 Gap 位为算术待输入，则当前位置需多加一个空白位
        boolean preGapIsMathExprInput = Input.isGap(preInput) && !Input.isEmpty(prePending) && prePending.isMathExpr();
        int gapSpaces = needGapSpace ? preGapIsMathExprInput ? 2 : 1 : 0;

        if (mathExprInput != null) {
            // 第一个普通输入不需要添加空白，
            // 但是对于第一个不为空的算术待输入则需要提前添加，
            // 因为，在输入过程中，算术待输入的前面没有 Gap 占位，
            // 输入完毕后才会添加 Gap 占位
            if (position == 0) {
                gapSpaces = !Input.isEmpty(mathExprInput) ? 1 : 0;
            }
            // 算术输入 在输入完毕后会在其内部的开头位置添加一个 Gap 占位，从而导致该输入发生后移，
            // 为避免视觉干扰，故在该算术的待输入之前先多附加一个空白
            else if (needGapSpace && input.isGap() && !Input.isEmpty(mathExprInput)) {
                gapSpaces = 2;
            }

            // Note：视图始终与待输入的算术输入绑定，
            // 以确保在 MathKeyboard#onTopUserInputMsg 中能够选中正在输入的算术表达式中的字符
            return new InputViewData(mathExprInput, pending, position, option, selected, gapSpaces);
        } else if (input.isGap()) {
            if (!Input.isEmpty(pending)) {
                gapSpaces = needGapSpace ? 2 : 1;
            }

            return new InputViewData(input, pending, position, option, selected, gapSpaces);
        } else {
            return new InputViewData(input, pending, position, option, selected, gapSpaces);
        }
    }

    private static MathExprInput tryGetMathExprInput(InputList inputList, Input input) {
        CharInput pending = inputList.getPendingOn(input);

        return isMathExprInput(pending)
               // 待输入的算术不能为空，否则，原输入需为空，才能将待输入作为算术输入，
               // 从而确保在未修改非算术输入时能够正常显示原始输入
               && (!Input.isEmpty(pending) || Input.isEmpty(input)) //
               ? (MathExprInput) pending //
               : isMathExprInput(input)
                 // 若算术输入 没有 被替换为非算术输入，则返回其自身
                 && Input.isEmpty(pending) //
                 ? (MathExprInput) input : null;
    }

    private static boolean needToBeSelected(InputList inputList, Input input) {
        boolean selected = inputList.isSelected(input);

        // 若配对符号的另一侧符号被选中，则该侧符号也同样需被选中
        if (!selected && !input.isGap() && ((CharInput) input).hasPair()) {
            if (inputList.isSelected(((CharInput) input).getPair())) {
                selected = true;
            }
        }
        return selected;
    }

    private static boolean isMathExprInput(Input input) {
        return input != null && input.isMathExpr();
    }
}
