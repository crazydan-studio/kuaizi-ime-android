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

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewHolder;
import org.crazydan.studio.app.ime.kuaizi.utils.ColorUtils;

/**
 * {@link Keyboard 键盘}{@link InputWord 输入候选字}的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-08
 */
public class InputCandidateView extends RecyclerViewHolder {
    private TextView notationView;
    private TextView wordView;

    private InputWord word;

    public InputCandidateView(@NonNull View itemView) {
        super(itemView);

        this.notationView = itemView.findViewById(R.id.notation_view);
        this.wordView = itemView.findViewById(R.id.word_view);
    }

    public void bind(InputWord word, boolean selected) {
        this.word = word;

        int bgColor;
        if (selected) {
            bgColor = ColorUtils.getByAttrId(getContext(), R.attr.input_candidates_selection_bg_color);
        } else {
            bgColor = ColorUtils.getByAttrId(getContext(), R.attr.input_candidates_bg_color);
        }
        this.itemView.setBackgroundColor(bgColor);

        int fgColor;
        if (selected) {
            fgColor = ColorUtils.getByAttrId(getContext(), R.attr.input_candidates_selection_fg_color);
        } else {
            fgColor = ColorUtils.getByAttrId(getContext(), R.attr.input_candidates_fg_color);
        }
        this.notationView.setTextColor(fgColor);
        this.wordView.setTextColor(fgColor);

        this.notationView.setText(word.getNotation());
        this.wordView.setText(word.getValue());
    }
}
