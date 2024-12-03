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

import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.ExerciseStep;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-19
 */
public class ExerciseStepFinalView extends ExerciseStepView {
    private final Button restartBtn;
    private final Button continueBtn;

    public ExerciseStepFinalView(@NonNull View itemView) {
        super(itemView);

        this.restartBtn = itemView.findViewById(R.id.restart_btn);
        this.continueBtn = itemView.findViewById(R.id.continue_btn);
    }

    @Override
    public void bind(ExerciseStep step, int position) {
        super.bind(step);

        ExerciseStep.Final finalStep = (ExerciseStep.Final) step;

        this.restartBtn.setOnClickListener(null);
        if (finalStep.restartCallback != null) {
            this.restartBtn.setOnClickListener((v) -> finalStep.restartCallback.run());
        } else {
            ViewUtils.hide(this.restartBtn);
        }

        this.continueBtn.setOnClickListener(null);
        if (finalStep.continueCallback != null) {
            ViewUtils.show(this.continueBtn);
            this.continueBtn.setOnClickListener((v) -> finalStep.continueCallback.run());
        } else {
            ViewUtils.hide(this.continueBtn);
        }
    }
}
