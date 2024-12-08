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

package org.crazydan.studio.app.ime.kuaizi.pane.view.input;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharMathExprInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.GapInput;

/**
 * {@link Input} 的 {@link RecyclerView} 适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class InputViewAdapter extends RecyclerViewAdapter<InputView<?>> {
    private static final int VIEW_TYPE_CHAR_INPUT = 0;
    private static final int VIEW_TYPE_GAP_INPUT = 1;
    private static final int VIEW_TYPE_CHAR_MATH_EXPR_INPUT = 2;

    private InputList inputList;
    private boolean canBeSelected;

    /** 更新输入列表，并对发生变更的输入发送变更消息，以仅对变化的输入做渲染 */
    public void updateInputList(InputList inputList, boolean canBeSelected) {
        this.canBeSelected = canBeSelected;
        this.inputList = inputList;

        // Note：在 Gap 添加空格后，涉及对与其相邻的输入视图的更新，
        // 其判断逻辑较复杂且容易遗漏更新，故而，直接对列表视图做全量更新
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return this.inputList == null ? 0 : this.inputList.getInputs().size();
    }

    @Override
    public void onBindViewHolder(@NonNull InputView<?> view, int position) {
        Input.Option option = this.inputList.getOption();

        Input<?> input = this.inputList.getInput(position);
        Input<?> preInput = this.inputList.getInput(position - 1);
        CharInput pending = this.inputList.getPendingOn(input);
        CharInput prePending = this.inputList.getPendingOn(preInput);

        CharMathExprInput mathExprInput = determineMathExprInput(position);

        boolean selected = this.canBeSelected && needToBeSelected(input);
        boolean needGapSpace = this.inputList.needGapSpace(input);

        // 前序正在输入的 Gap 位为算术待输入，则当前位置需多加一个空白位
        boolean preGapIsMathExprInput = Input.isGap(preInput) && !Input.isEmpty(prePending) && prePending.isMathExpr();
        int gapSpaceCount = needGapSpace ? preGapIsMathExprInput ? 2 : 1 : 0;

        if (mathExprInput != null) {
            // 第一个普通输入不需要添加空白，
            // 但是对于第一个不为空的算术待输入则需要提前添加，
            // 因为，在输入过程中，算术待输入的前面没有 Gap 占位，
            // 输入完毕后才会添加 Gap 占位
            if (position == 0) {
                gapSpaceCount = !Input.isEmpty(mathExprInput) ? 1 : 0;
            }
            // 算术输入 在输入完毕后会在其内部的开头位置添加一个 Gap 占位，从而导致该输入发生后移，
            // 为避免视觉干扰，故在该算术的待输入之前先多附加一个空白
            else if (needGapSpace && input.isGap() && !Input.isEmpty(mathExprInput)) {
                gapSpaceCount = 2;
            }

            // Note：视图始终与待输入的算术输入绑定，
            // 以确保在 MathKeyboard#onTopUserInputMsg 中能够选中正在输入的算术表达式中的字符
            ((CharMathExprInputView) view).bind(option, mathExprInput, mathExprInput, selected, gapSpaceCount);
        } else if (input.isGap()) {
            if (!Input.isEmpty(pending)) {
                gapSpaceCount = needGapSpace ? 2 : 1;
            }

            ((GapInputView) view).bind(option, (GapInput) input, pending, selected, gapSpaceCount);
        } else {
            ((CharInputView) view).bind(option, (CharInput) input, pending, selected, gapSpaceCount);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Input<?> input = this.inputList.getInput(position);
        CharMathExprInput mathExprInput = determineMathExprInput(position);

        if (mathExprInput != null) {
            return VIEW_TYPE_CHAR_MATH_EXPR_INPUT;
        } else if (input.isGap()) {
            return VIEW_TYPE_GAP_INPUT;
        } else {
            return VIEW_TYPE_CHAR_INPUT;
        }
    }

    @NonNull
    @Override
    public InputView<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CHAR_INPUT) {
            return new CharInputView(inflateItemView(parent, R.layout.input_char_view));
        } else if (viewType == VIEW_TYPE_CHAR_MATH_EXPR_INPUT) {
            return new CharMathExprInputView(inflateItemView(parent, R.layout.input_char_math_expr_view));
        } else {
            return new GapInputView(inflateItemView(parent, R.layout.input_gap_view));
        }
    }

    private boolean needToBeSelected(Input<?> input) {
        boolean selected = this.inputList.isSelected(input);

        // 若配对符号的另一侧符号被选中，则该侧符号也同样需被选中
        if (!selected && !input.isGap() && ((CharInput) input).hasPair()) {
            if (this.inputList.isSelected(((CharInput) input).getPair())) {
                selected = true;
            }
        }

        return selected;
    }

    private CharMathExprInput determineMathExprInput(int position) {
        Input<?> input = this.inputList.getInput(position);
        CharInput pending = this.inputList.getPendingOn(input);

        return isMathExprInput(pending)
               // 待输入的算术不能为空，否则，原输入需为空，才能将待输入作为算术输入，
               // 从而确保在未修改非算术输入时能够正常显示原始输入
               && (!Input.isEmpty(pending) || Input.isEmpty(input)) //
               ? (CharMathExprInput) pending //
               : isMathExprInput(input)
                 // 若算术输入 没有 被替换为非算术输入，则返回其自身
                 && Input.isEmpty(pending) //
                 ? (CharMathExprInput) input : null;
    }

    private boolean isMathExprInput(Input<?> input) {
        return input != null && input.isMathExpr();
    }
}
