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
public class ExerciseViewHolder extends RecyclerViewHolder<Exercise.ViewData> implements InputMsgListener {
    protected final TextView titleView;
    protected final ExerciseStepListView stepListView;
    private final ExerciseEditText textView;

    public ExerciseViewHolder(@NonNull View itemView) {
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
    public void bind(Exercise.ViewData data, int position) {
        whenViewReady(this.titleView, (view) -> {
            String title = Exercise.createViewTitle(data.title, position);
            view.setText(title);
        });

        update(data);
    }

    /** 激活指定位置的步骤 */
    public void activateStep(Exercise.ViewData data, int stepIndex) {
        update(data);

        this.stepListView.scrollTo(stepIndex);
        // Note: 捕获输入焦点必须在 ExerciseView 视图就绪后进行，而不能在初始绑定时，
        // 否则，其会迟滞页面的滚动，容易造成实际激活的页与选定的不一致
        this.textView.requestFocus();
    }

    /** 更新视图 */
    public void update(Exercise.ViewData data) {
        whenViewReady(this.textView, (view) -> {
            String text = data.sampleText;
            view.setText(text);
            view.setSelection(text != null ? text.length() : 0);
        });

        whenViewReady(this.stepListView, (view) -> {
            view.update(data);
        });
    }
}
