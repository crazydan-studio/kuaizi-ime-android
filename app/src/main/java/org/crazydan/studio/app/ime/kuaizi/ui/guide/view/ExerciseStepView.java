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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.view;

import java.util.Locale;

import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewHolder;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.ExerciseStep;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStepView extends RecyclerViewHolder<ExerciseStep> {
    private final ImageView pointerView;
    private final TextView contentView;

    public ExerciseStepView(@NonNull View itemView) {
        super(itemView);

        this.pointerView = itemView.findViewById(R.id.pointer_view);
        this.contentView = itemView.findViewById(R.id.content_view);
    }

    public void bind(ExerciseStep step, int position) {
        super.bind(step);

        ViewUtils.visible(this.pointerView, step.isRunning());

        String text = String.format(Locale.getDefault(), "%d. %s", position + 1, step.content);
        this.contentView.setText(html(step, step.isRunning() ? "<b>" + text + "</b>" : text));
    }

    private Spanned html(ExerciseStep step, String text) {
        return Html.fromHtml(text, FROM_HTML_MODE_COMPACT, (source) -> getImage(step, source), null);
    }

    private Drawable getImage(ExerciseStep step, String source) {
        if (step.imageGetter == null) {
            return null;
        }

        int size = (int) ScreenUtils.pxFromDimension(this.itemView.getContext(), R.dimen.guide_exercise_step_icon_size);
        return step.imageGetter.get(source, size, size);
    }
}
