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

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewAdapter;

/**
 * {@link Keyboard 键盘}{@link InputWord 输入候选字}的{@link RecyclerView}适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-08
 */
public class InputCandidateViewAdapter extends RecyclerViewAdapter<InputCandidateView> {
    private InputList inputList;

    public void setInputList(InputList inputList) {
        this.inputList = inputList;
    }

    public Input getInput() {
        if (this.inputList == null) {
            return null;
        }

        Input selected = this.inputList.cursor().selected();
        Input pending = this.inputList.cursor().pending();

        if (pending != null) {
            return pending;
        } else if (selected instanceof CharInput) {
            return selected;
        }
        return null;
    }

    public int getSelectedWordPosition() {
        Input input = getInput();

        if (input != null) {
            InputWord word = input.word();

            return word == null || input.candidates().isEmpty() ? 0 : input.candidates().indexOf(word);
        }
        return 0;
    }

    @Override
    public int getItemCount() {
        Input input = getInput();
        return input == null ? 0 : input.candidates().size();
    }

    @Override
    public void onBindViewHolder(@NonNull InputCandidateView view, int position) {
        Input input = getInput();

        if (input != null) {
            InputWord word = input.word();
            InputWord candidate = input.candidates().get(position);

            view.bind(candidate, word == candidate);
        }
    }

    @NonNull
    @Override
    public InputCandidateView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new InputCandidateView(inflateHolderView(parent, R.layout.char_input_candidate_view));
    }
}
