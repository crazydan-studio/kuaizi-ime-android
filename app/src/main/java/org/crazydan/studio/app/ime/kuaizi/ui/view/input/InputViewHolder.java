/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.ui.view.input;

import java.util.function.BiConsumer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewHolder;
import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData;

/**
 * {@link Input} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public abstract class InputViewHolder extends RecyclerViewHolder {

    public InputViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(InputViewData data) {
        addGapSpaceMargin(this.itemView, data.gapSpaces);
    }

    protected void setSelectedBgColor(View view, boolean selected) {
        int bgColor = selected ? R.attr.input_selection_bg_color : R.attr.input_bg_color;
        setBackgroundColorByAttrId(view, bgColor);
    }

    protected void setSelectedTextColor(TextView view, boolean selected) {
        int fgColor = selected ? R.attr.input_selection_fg_color : R.attr.input_fg_color;
        setTextColorByAttrId(view, fgColor);
    }

    protected void addGapSpaceMargin(View view, float[] gapSpaces) {
        ViewGroup.MarginLayoutParams layout = (ViewGroup.MarginLayoutParams) view.getLayoutParams();

        withGapSpaces(gapSpaces, (left, right) -> {
            layout.leftMargin = left;
            layout.rightMargin = right;
        });
    }

    protected void addGapSpacePadding(View view, float[] gapSpaces) {
        withGapSpaces(gapSpaces, (left, right) -> view.setPadding(left, 0, right, 0));
    }

    private void withGapSpaces(float[] gapSpaces, BiConsumer<Integer, Integer> c) {
        if (gapSpaces == null) {
            gapSpaces = new float[] { 0f, 0f };
        }

        float margin = ScreenUtils.pxFromDimension(getContext(), R.dimen.gap_input_width);
        int left = Math.round(margin * gapSpaces[0]);
        int right = Math.round(margin * gapSpaces[1]);

        c.accept(left, right);
    }
}
