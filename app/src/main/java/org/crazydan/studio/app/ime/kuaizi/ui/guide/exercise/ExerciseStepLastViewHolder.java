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
import android.widget.Button;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.KeyImageRender;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-19
 */
public class ExerciseStepLastViewHolder extends ExerciseStepViewHolder {
    private final Button restartBtn;
    private final Button continueBtn;

    public ExerciseStepLastViewHolder(@NonNull View itemView) {
        super(itemView);

        this.restartBtn = itemView.findViewById(R.id.restart_btn);
        this.continueBtn = itemView.findViewById(R.id.continue_btn);
    }

    /** 视图与数据的初始绑定 */
    @Override
    public void bind(KeyImageRender keyImageRender, ExerciseStep.ViewData data, int position) {
        super.bind(keyImageRender, data, position);

        whenViewReady(this.restartBtn, (btn) -> {
            ViewUtils.visible(btn, data.restartCallback != null);

            btn.setOnClickListener(data.restartCallback == null //
                                   ? null : (v) -> data.restartCallback.run());
        });

        whenViewReady(this.continueBtn, (btn) -> {
            ViewUtils.visible(btn, data.continueCallback != null);

            btn.setOnClickListener(data.continueCallback == null //
                                   ? null : (v) -> data.continueCallback.run());
        });
    }
}
