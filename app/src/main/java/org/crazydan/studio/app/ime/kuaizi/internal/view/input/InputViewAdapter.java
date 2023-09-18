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

package org.crazydan.studio.app.ime.kuaizi.internal.view.input;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharMathExprInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.GapInput;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewAdapter;

/**
 * {@link Keyboard 键盘}{@link Input 输入}的{@link RecyclerView}适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class InputViewAdapter extends RecyclerViewAdapter<InputView<?>> {
    private static final int VIEW_TYPE_CHAR_INPUT = 0;
    private static final int VIEW_TYPE_GAP_INPUT = 1;
    private static final int VIEW_TYPE_CHAR_MATH_EXPR_INPUT = 2;

    private InputList inputList;
    private List<Integer> inputHashes = new ArrayList<>();

    /** 更新输入列表，并对发生变更的输入发送变更消息，以仅对变化的输入做渲染 */
    public void updateInputList(InputList inputList) {
        this.inputList = inputList;

//        List<Integer> oldInputHashes = this.inputHashes;
//        List<Integer> newInputHashes = getInputHashes();
//        this.inputHashes = new ArrayList<>(newInputHashes);
//
//        updateItems(oldInputHashes, newInputHashes);

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

        Input<?> input = this.inputList.getInputs().get(position);
        CharInput pending = this.inputList.getPendingOn(input);
        CharMathExprInput mathExprInput = determineMathExprInput(position);

        boolean selected = needToBeSelected(input);
        boolean needGapSpace = this.inputList.needGapSpace(input);

        if (mathExprInput != null) {
            // Note：视图始终与待输入的算数输入绑定，
            // 以确保在 MathKeyboard#onTopUserInputMsg 中能够选中正在输入的算数表达式中的字符
            ((CharMathExprInputView) view).bind(option, mathExprInput, mathExprInput, needGapSpace, selected);
        } else if (input instanceof CharInput) {
            ((CharInputView) view).bind(option, (CharInput) input, pending, needGapSpace, selected);
        } else {
            ((GapInputView) view).bind(option, (GapInput) input, pending, needGapSpace, selected);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Input<?> input = this.inputList.getInputs().get(position);
        CharMathExprInput mathExprInput = determineMathExprInput(position);

        if (mathExprInput != null) {
            return VIEW_TYPE_CHAR_MATH_EXPR_INPUT;
        } else if (input instanceof CharInput) {
            return VIEW_TYPE_CHAR_INPUT;
        } else {
            return VIEW_TYPE_GAP_INPUT;
        }
    }

    @NonNull
    @Override
    public InputView<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CHAR_INPUT) {
            return new CharInputView(inflateHolderView(parent, R.layout.char_input_view));
        } else if (viewType == VIEW_TYPE_CHAR_MATH_EXPR_INPUT) {
            return new CharMathExprInputView(inflateHolderView(parent, R.layout.char_math_expr_input_view));
        } else {
            return new GapInputView(inflateHolderView(parent, R.layout.gap_input_view));
        }
    }

    private List<Integer> getInputHashes() {
        if (this.inputList == null) {
            return new ArrayList<>();
        }

        return this.inputList.getInputs().stream().map(input -> {
            Input.Option option = this.inputList.getOption();
            CharInput pending = this.inputList.getPendingOn(input);
            Input<?> target = pending != null ? pending : input;

            boolean selected = needToBeSelected(input);
            boolean needGapSpace = this.inputList.needGapSpace(input);
            int textHash = option != null && target.hasWord() ? target.getText(option).hashCode() : 0;

            // 因为输入内容可能是相同的，故而，需要对其内容和引用做 Hash
            int hash = target.hashCode() + System.identityHashCode(target);

            return hash + (selected ? 1 : 0) + (needGapSpace ? 1 : 0) + textHash;
        }).collect(Collectors.toList());
    }

    private boolean needToBeSelected(Input<?> input) {
        boolean selected = this.inputList.isSelected(input);

        // 若配对符号的另一侧符号被选中，则该侧符号也同样需被选中
        if (!selected && input instanceof CharInput && ((CharInput) input).hasPair()) {
            if (this.inputList.isSelected(((CharInput) input).getPair())) {
                selected = true;
            }
        }

        return selected;
    }

    private CharMathExprInput determineMathExprInput(int position) {
        Input<?> input = this.inputList.getInputs().get(position);
        CharInput pending = this.inputList.getPendingOn(input);

        return isMathExprInput(pending)
               // 待输入的算数不能为空，否则，原输入需为空，才能将待输入作为算数输入，
               // 从而确保在未修改非算数输入时能够正常显示原始输入
               && (!Input.isEmpty(pending) || Input.isEmpty(input)) //
               ? (CharMathExprInput) pending //
               : isMathExprInput(input)
                 // 若算数输入 没有 被替换为非算数输入，则返回其自身
                 && Input.isEmpty(pending) //
                 ? (CharMathExprInput) input : null;
    }

    private boolean isMathExprInput(Input<?> input) {
        return input != null && input.isMathExpr();
    }
}
