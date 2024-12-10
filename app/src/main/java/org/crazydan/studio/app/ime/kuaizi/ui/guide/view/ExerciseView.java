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
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.Exercise;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.ExerciseStepListView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputPaneView;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewHolder;

/**
 * {@link Exercise 练习题}视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseView extends RecyclerViewHolder<Exercise> implements KeyboardMsgListener {
    protected final TextView titleView;
    protected final ExerciseStepListView stepListView;
    private final ExerciseEditText textView;

    public ExerciseView(@NonNull View itemView) {
        super(itemView);

        this.titleView = itemView.findViewById(R.id.title_view);
        this.stepListView = itemView.findViewById(R.id.step_list_view);
        this.textView = itemView.findViewById(R.id.text_view);
    }

    public static String createTitle(Exercise exercise, int position) {
        return String.format(Locale.getDefault(), "%d. %s", position + 1, exercise.title);
    }

    public void bind(Exercise exercise, int position) {
        super.bind(exercise);

        String title = createTitle(exercise, position);
        this.titleView.setText(title);

        updateSteps();
    }

    public void withIme(InputPaneView ime) {
        Exercise exercise = getData();

        this.textView.requestFocus();

        String text = exercise.getSampleText();
        this.textView.setText(text);
        this.textView.setSelection(text != null ? text.length() : 0);

        exercise.setProgressListener((step, position) -> {
            updateSteps();
            scrollTo(position);
        });

        exercise.restart();
    }

    @Override
    public void onMsg(Keyboard keyboard, KeyboardMsg msg, KeyboardMsgData msgData) {
        this.textView.onMsg(keyboard, msg, msgData);

        getData().onMsg(keyboard, msg, msgData);
    }

    protected void scrollTo(int position) {
        // 提前定位到最后一个 step
        if (this.stepListView.adapter.isFinalStep(position + 1)) {
            position += 1;
        }

        this.stepListView.smoothScrollToPosition(position);
    }

    protected void updateSteps() {
        if (this.stepListView == null) {
            return;
        }

        Exercise exercise = getData();
        this.stepListView.adapter.bind(exercise.steps);
    }
}
