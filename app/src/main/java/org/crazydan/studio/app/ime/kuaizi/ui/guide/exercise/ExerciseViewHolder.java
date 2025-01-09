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

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewHolder;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.common.ImeSupportEditText;

/**
 * {@link Exercise} 视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseViewHolder extends RecyclerViewHolder implements InputMsgListener {
    protected final TextView titleView;
    protected final ImeSupportEditText editorView;

    protected final ExerciseStepListView stepListView;

    public ExerciseViewHolder(@NonNull View itemView) {
        super(itemView);

        this.titleView = itemView.findViewById(R.id.title_view);
        this.editorView = itemView.findViewById(R.id.editor_view);

        this.stepListView = itemView.findViewById(R.id.step_list_view);
    }

    @Override
    public void onMsg(InputMsg msg) {
        this.editorView.onMsg(msg);
    }

    /** 视图与数据的初始绑定 */
    public void bind(Exercise.ViewData data, int position) {
        whenViewReady(this.titleView, (view) -> {
            String title = Exercise.createViewTitle(data.title, position);
            view.setText(title);
        });

        resetEditorView(data);
        updateSteps(data);
    }

    /** 激活指定位置的步骤 */
    public void activateStep(Exercise.ViewData data, int stepIndex, boolean needToReset) {
        if (needToReset) {
            resetEditorView(data);
        }
        updateSteps(data);

        this.stepListView.scrollTo(stepIndex);

        // Note: 捕获输入焦点必须在 ExerciseView 视图就绪后进行，而不能在初始绑定时，
        // 否则，其会迟滞页面的滚动，容易造成实际激活的页与选定的不一致
        focusOnEditor();
    }

    /** 更新步骤视图 */
    public void updateSteps(Exercise.ViewData data) {
        whenViewReady(this.stepListView, (view) -> {
            view.update(data);
        });
    }

    protected void focusOnEditor() {
        whenViewReady(this.editorView, View::requestFocus);
    }

    protected void resetEditorView(Exercise.ViewData data) {
        whenViewReady(this.editorView, (view) -> {
            String text = data.sampleText;
            view.setText(text);
            // 将光标移到内容尾部
            view.setSelection(text != null ? text.length() : 0);
        });
    }
}
