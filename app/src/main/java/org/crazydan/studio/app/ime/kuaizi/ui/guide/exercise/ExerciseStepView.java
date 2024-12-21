/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise;

import android.text.Spanned;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewHolder;

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

    /** 视图与数据的初始绑定 */
    public void bind(ExerciseStep step, int position) {
        super.bind(step);

        ViewUtils.visible(this.pointerView, step.running());

        String text = (position + 1) + ". " + step.content();
        if (step.running()) {
            text = "<b>" + text + "</b>";
        }

        int imageSize = (int) ScreenUtils.pxFromDimension(getContext(), R.dimen.guide_exercise_step_icon_size);
        Spanned spannedText = step.renderText(text, imageSize);

        this.contentView.setText(spannedText);
    }
}
