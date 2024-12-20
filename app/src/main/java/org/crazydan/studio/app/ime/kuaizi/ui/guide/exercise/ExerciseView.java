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

import java.util.Locale;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewHolder;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;

/**
 * {@link Exercise} 视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseView extends RecyclerViewHolder<Exercise> implements InputMsgListener {
    protected final TextView titleView;
    protected final ExerciseStepListView stepListView;
    private final ExerciseEditText textView;

    public static String createTitle(Exercise exercise, int position) {
        return String.format(Locale.getDefault(), "%d. %s", position + 1, exercise.title);
    }

    public ExerciseView(@NonNull View itemView) {
        super(itemView);

        this.titleView = itemView.findViewById(R.id.title_view);
        this.stepListView = itemView.findViewById(R.id.step_list_view);
        this.textView = itemView.findViewById(R.id.text_view);
    }

    @Override
    public void onMsg(InputMsg msg) {
        this.textView.onMsg(msg);
    }

    /** 视图与数据的初始绑定 */
    public void bind(Exercise exercise, int position) {
        super.bind(exercise);

        String title = createTitle(exercise, position);
        this.titleView.setText(title);

        // Note: 可能会因为渲染时间过长而导致试题翻页出现迟滞，
        // 并且易出现选中的页与实际切换的页不相符的问题
        update();
    }

    /** 激活指定位置的步骤 */
    public void activateStepAt(int index) {
        update();

        this.stepListView.scrollTo(index);
    }

    /** 更新视图 */
    private void update() {
        Exercise exercise = getData();

        // Note: 初始绑定时，该视图为 null
        if (this.textView != null) {
            String text = exercise.getSampleText();
            this.textView.requestFocus();
            this.textView.setText(text);
            this.textView.setSelection(text != null ? text.length() : 0);
        }

        // Note: 初始绑定时，该视图为 null
        if (this.stepListView != null) {
            this.stepListView.update(exercise.steps);
        }
    }
}
