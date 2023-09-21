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
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.ExerciseMain;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.ExerciseStep;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.ExerciseStepListView;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStepView extends RecyclerView.ViewHolder {
    private final TextView contentView;
    private final ExerciseStepListView subStepListView;

    public ExerciseStepView(@NonNull View itemView) {
        super(itemView);

        this.contentView = itemView.findViewById(R.id.content_view);
        this.subStepListView = itemView.findViewById(R.id.sub_step_list_view);
    }

    public void bind(ExerciseStep step, int position) {
        String text = String.format(Locale.getDefault(), "%d. %s", position + 1, step.content);
        this.contentView.setText(html(text));

        if (step.subs.isEmpty()) {
            ViewUtils.hide(this.subStepListView);
        } else {
            ViewUtils.show(this.subStepListView);

            this.subStepListView.adapter.bind(step.subs);
        }
    }

    private Spanned html(String text) {
        return Html.fromHtml(text,
                             FROM_HTML_MODE_COMPACT,
                             (source) -> getImage(ExerciseMain.sandboxView, source),
                             null);
    }

    private Drawable getImage(ViewGroup sandboxView, String source) {
        View view = sandboxView.getChildAt(0);
        Drawable d = ViewUtils.toDrawable(view, 80, 80);

        return d;
    }
}
