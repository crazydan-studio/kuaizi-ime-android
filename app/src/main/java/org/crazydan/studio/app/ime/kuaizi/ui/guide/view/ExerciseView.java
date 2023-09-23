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

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewHolder;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.Exercise;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.ExerciseStepListView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView;

/**
 * {@link Exercise 练习题}视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseView extends RecyclerViewHolder<Exercise> {
    private final TextView titleView;
    private final ExerciseStepListView stepListView;
    private final ExerciseEditText textView;

    public ExerciseView(@NonNull View itemView) {
        super(itemView);

        this.titleView = itemView.findViewById(R.id.title_view);
        this.stepListView = itemView.findViewById(R.id.step_list_view);
        this.textView = itemView.findViewById(R.id.text_view);
    }

    public void withIme(ImeInputView ime) {
        Exercise exercise = getData();
        exercise.reset();

        ime.removeInputMsgListenerByType(this.textView.getClass());
        ime.addInputMsgListener(this.textView);

        ime.removeInputMsgListenerByType(exercise.getClass());
        ime.addInputMsgListener(getData());

        ime.setDisableUserInputData(exercise.isDisableUserInputData());

        this.textView.requestFocus();

        exercise.setStepListener((step, position) -> {
            updateSteps();
            this.stepListView.smoothScrollToPosition(position);
        });
        exercise.start();
    }

    public void bind(Exercise exercise, int position) {
        super.bind(exercise);

        String title = String.format(Locale.getDefault(), "%d. %s", position + 1, exercise.title);
        this.titleView.setText(title);

        updateSteps();
    }

    private void updateSteps() {
        if (this.stepListView == null) {
            return;
        }

        Exercise exercise = getData();
        this.stepListView.adapter.bind(exercise.steps);
    }
}
