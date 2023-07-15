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
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class CharInputView extends InputView<CharInput> {
    private final TextView notationView;
    private final TextView wordView;

    public CharInputView(@NonNull View itemView) {
        super(itemView);

        this.notationView = itemView.findViewById(R.id.notation_view);
        this.wordView = itemView.findViewById(R.id.word_view);
    }

    public void bind(
            CharInput input, boolean selected, boolean needToAddMargin, //
            boolean needToAddPrevSpace, boolean needToAddPostSpace
    ) {
        super.bind(input, selected);

        int margin = 0;
        // 预留一个 Gap 位
        if (needToAddMargin) {
            margin = getContext().getResources().getDimensionPixelSize(R.dimen.gap_input_width);
        }
        ((RecyclerView.LayoutParams) this.itemView.getLayoutParams()).setMargins(margin, 0, margin, 0);

        int fgColor = selected ? R.attr.input_selection_fg_color : R.attr.input_fg_color;
        setTextColorByAttrId(this.wordView, fgColor);
        setTextColorByAttrId(this.notationView, fgColor);

        InputWord word = input.getWord();
        if (word == null) {
            String text = (needToAddPrevSpace ? " " : "") //
                          + String.join("", input.getChars()) //
                          + (needToAddPostSpace ? " " : "");

            this.wordView.setText(text);
            this.notationView.setText("");
        } else {
            this.wordView.setText(word.getValue());
            this.notationView.setText(word.getNotation());
        }
    }
}
