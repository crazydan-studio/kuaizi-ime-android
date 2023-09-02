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

    private InputList inputList;
    private List<Integer> inputHashes = new ArrayList<>();

    public int getSelectedInputPosition() {
        return this.inputList.getSelectedIndex();
    }

    /** 更新输入列表，并对发生变更的输入发送变更消息，以仅对变化的输入做渲染 */
    public void updateInputList(InputList inputList) {
        this.inputList = inputList;

        List<Integer> oldInputHashes = this.inputHashes;
        List<Integer> newInputHashes = getInputHashes();
        this.inputHashes = new ArrayList<>(newInputHashes);

        updateItems(oldInputHashes, newInputHashes);
    }

    @Override
    public int getItemCount() {
        return this.inputList == null ? 0 : this.inputList.getInputs().size();
    }

    @Override
    public void onBindViewHolder(@NonNull InputView<?> view, int position) {
        Input input = this.inputList.getInputs().get(position);
        CharInput pending = this.inputList.getPendingOn(input);
        boolean selected = this.inputList.isSelected(input);

        if (input instanceof CharInput) {
            ((CharInputView) view).bind((CharInput) input,
                                        pending,
                                        selected,
                                        this.inputList.needPrevSpace(position),
                                        this.inputList.needPostSpace(position));
        } else {
            ((GapInputView) view).bind((GapInput) input, pending, selected);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Input input = this.inputList.getInputs().get(position);

        if (input instanceof CharInput) {
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
        } else {
            return new GapInputView(inflateHolderView(parent, R.layout.gap_input_view));
        }
    }

    private List<Integer> getInputHashes() {
        return this.inputList.getInputs().stream().map(input -> {
            CharInput pending = this.inputList.getPendingOn(input);
            boolean selected = this.inputList.isSelected(input);
            // 只能做引用比较，因为输入内容可能是相同的
            int hash = pending != null
                       ? pending.hashCode() + System.identityHashCode(pending)
                       : System.identityHashCode(input);

            return hash + (selected ? 0 : 1);
        }).collect(Collectors.toList());
    }
}
