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

import java.util.List;

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
    private List<Input> oldInputs;

    public void setInputList(InputList inputList) {
        this.inputList = inputList;
//        this.oldInputs = new ArrayList<>(inputList.getInputs());
    }

    public int getSelectedInputPosition() {
        return this.inputList.getCursorIndex();
    }

    public void updateItems() {
//        List<Input> newInputs = this.inputList.getInputs();
//
//        updateItems(this.oldInputs, newInputs);
//        notifyItemChanged(this.inputList.getCursorIndex());
//
//        this.oldInputs = new ArrayList<>(newInputs);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return this.inputList == null ? 0 : this.inputList.getInputs().size();
    }

    @Override
    public void onBindViewHolder(@NonNull InputView<?> view, int position) {
        Input input = getInputAt(position);
        boolean selected = this.inputList.getCursorIndex() == position;

        if (input instanceof CharInput) {
            boolean pending = this.inputList.getPending() == input;

            ((CharInputView) view).bind((CharInput) input,
                                        selected,
                                        pending,
                                        this.inputList.needPrevSpace(position),
                                        this.inputList.needPostSpace(position));
        } else {
            ((GapInputView) view).bind((GapInput) input, selected);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Input input = getInputAt(position);

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

    private Input getInputAt(int position) {
        Input input = this.inputList.getInputs().get(position);
        Input selected = this.inputList.getSelected();
        Input pending = this.inputList.getPending();

        if (selected == input && pending != null) {
            return pending;
        } else {
            return input;
        }
    }
}
