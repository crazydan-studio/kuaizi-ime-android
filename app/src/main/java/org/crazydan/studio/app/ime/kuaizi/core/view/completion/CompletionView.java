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

package org.crazydan.studio.app.ime.kuaizi.core.view.completion;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.core.view.input.CharInputView;
import org.crazydan.studio.app.ime.kuaizi.widget.ShadowDrawable;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewHolder;

/**
 * {@link CompletionInput} 的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class CompletionView extends RecyclerViewHolder<CompletionInput> {
    private final HorizontalScrollView scrollView;
    private final ViewGroup inputsView;

    public CompletionView(@NonNull View itemView) {
        super(itemView);

        this.inputsView = itemView.findViewById(R.id.inputs_view);
        this.scrollView = itemView.findViewById(R.id.scroll_view);

        String shadow = getStringByAttrId(R.attr.input_completions_shadow_style);
        ShadowDrawable bg = new ShadowDrawable(this.itemView.getBackground(), shadow);
        this.itemView.setBackground(bg);
    }

    public HorizontalScrollView getScrollView() {
        return this.scrollView;
    }

    @Override
    public void bind(CompletionInput data) {
        super.bind(data);

        this.inputsView.removeAllViews();

        data.inputs.forEach(this::createInputView);
    }

    private void createInputView(CharInput input) {
        // Note：若设置了 root，则返回值也为该 root，
        // 这里需直接处理 R.layout.char_input_view 视图，故设置为 null
        View inputView = LayoutInflater.from(getContext()).inflate(R.layout.input_char_view, null);

        // Note：在 layout xml 中设置的布局不会生效，需显式设置
        ViewGroup.MarginLayoutParams layoutParams
                = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                   ViewGroup.LayoutParams.MATCH_PARENT);
        inputView.setLayoutParams(layoutParams);

        new CharInputView(inputView).bind(null, input, null, false, true, 0);

        // 采用 itemView 的背景
        inputView.setBackgroundColor(Color.TRANSPARENT);

        this.inputsView.addView(inputView);
    }
}
