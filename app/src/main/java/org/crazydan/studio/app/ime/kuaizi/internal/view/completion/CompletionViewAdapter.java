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

package org.crazydan.studio.app.ime.kuaizi.internal.view.completion;

import java.util.ArrayList;
import java.util.List;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewAdapter;

/**
 * {@link CompletionInput} 的{@link RecyclerView}适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class CompletionViewAdapter extends RecyclerViewAdapter<CompletionView> {
    private List<CompletionInput> completions;

    public CompletionViewAdapter() {
//        this.completions = new ArrayList<>();
//
//        for (String s : new String[] {
//                "阿克", "国", "Android", "Loooooooooooooooooooooooong", "长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长"
//        }) {
//            CompletionInput completion = new CompletionInput();
//
//            for (int i = 0; i < s.length(); i++) {
//                String ch = s.charAt(i) + "";
//                List<Key<?>> keys = CharKey.from(ch);
//                if (!keys.isEmpty()) {
//                    CharInput input = CharInput.from(keys);
//
//                    completion.add(input);
//                }
//            }
//            this.completions.add(completion);
//        }
    }

    public void updateDataList(List<CompletionInput> completions) {
        this.completions = completions;

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return this.completions == null ? 0 : this.completions.size();
    }

    @Override
    public void onBindViewHolder(@NonNull CompletionView view, int position) {
        CompletionInput completion = this.completions.get(position);

        view.bind(completion);
    }

    @NonNull
    @Override
    public CompletionView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CompletionView(inflateItemView(parent, R.layout.input_completion_view));
    }
}
