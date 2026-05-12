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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise;

import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewHolder;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.KeyImageRender;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStepViewHolder extends RecyclerViewHolder {
    private final ImageView pointerView;
    private final TextView contentView;

    public ExerciseStepViewHolder(@NonNull View itemView) {
        super(itemView);

        this.pointerView = itemView.findViewById(R.id.pointer);
        this.contentView = itemView.findViewById(R.id.content);
    }

    /** 视图与数据的初始绑定 */
    public void bind(KeyImageRender keyImageRender, ExerciseStep.ViewData data, int position) {
        whenViewReady(this.pointerView, (view) -> {
            ViewUtils.visible(view, data.active);
        });

        whenViewReady(this.contentView, (view) -> {
            int imageSize = (int) ScreenUtils.pxFromDimension(getContext(), R.dimen.guide_exercise_step_icon_size);
            String text = ExerciseStep.createViewContent(data, position);
            Spanned spannedText = renderText(keyImageRender, text, imageSize);

            view.setText(spannedText);
        });
    }

    private Spanned renderText(KeyImageRender keyImageRender, String text, int imageSize) {
        return Html.fromHtml(text,
                             Html.FROM_HTML_MODE_COMPACT,
                             (source) -> keyImageRender != null
                                         ? keyImageRender.renderKey(source, imageSize, imageSize)
                                         : null,
                             null);
    }
}
