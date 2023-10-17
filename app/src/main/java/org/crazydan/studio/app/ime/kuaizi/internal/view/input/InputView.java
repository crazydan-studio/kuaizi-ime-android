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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewHolder;

/**
 * {@link Input 键盘输入}的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public abstract class InputView<I extends Input<?>> extends RecyclerViewHolder<I> {
    private final TextView notationView;
    private final TextView wordView;
    private final ImageView spaceView;

    public InputView(@NonNull View itemView) {
        super(itemView);

        this.notationView = itemView.findViewById(R.id.notation_view);
        this.wordView = itemView.findViewById(R.id.word_view);
        this.spaceView = itemView.findViewById(R.id.space_view);
    }

    protected void setSelectedBgColor(View view, boolean selected) {
        int bgColor = selected ? R.attr.input_selection_bg_color : R.attr.input_bg_color;
        setBackgroundColorByAttrId(view, bgColor);
    }

    protected void setSelectedTextColor(TextView view, boolean selected) {
        int fgColor = selected ? R.attr.input_selection_fg_color : R.attr.input_fg_color;
        setTextColorByAttrId(view, fgColor);
    }

    protected void showWord(Input.Option option, CharInput input, boolean selected) {
        showWord(option, input, selected, false);
    }

    protected void showWord(Input.Option option, CharInput input, boolean selected, boolean hideWordNotation) {
        InputWord word = input.getWord();
        String value = word != null ? word.getValue() : String.join("", input.getChars());
        String notation = word != null && !hideWordNotation ? word.getNotation() : null;

        if (option != null && word != null) {
            value = input.getText(option).toString();

            if (notation != null && value.contains(notation)) {
                notation = null;
            }
        }

        if (input.isSpace()) {
            this.wordView.setText(null);
            ViewUtils.show(this.spaceView);
        } else {
            this.wordView.setText(value);
            setSelectedTextColor(this.wordView, selected);

            ViewUtils.hide(this.spaceView);
        }

        if (notation != null) {
            ViewUtils.show(this.notationView).setText(notation);
            setSelectedTextColor(this.notationView, selected);
        } else {
            ViewUtils.hide(this.notationView);
        }
    }

    protected void addLeftSpaceMargin(View view, int times) {
        int margin = (int) ScreenUtils.pxFromDimension(getContext(), R.dimen.gap_input_width);
        ViewGroup.MarginLayoutParams layout = (ViewGroup.MarginLayoutParams) view.getLayoutParams();

        layout.leftMargin = margin * times;
    }
}
