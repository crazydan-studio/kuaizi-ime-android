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
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;

/**
 * 题型为 {@link Exercise.Mode#introduce} 的练习题视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseIntroduceViewHolder extends ExerciseViewHolder {

    public ExerciseIntroduceViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    public void onMsg(InputMsg msg) {
        // Note: 介绍模式下，无输入框，不对输入消息做处理
    }

    @Override
    public void activateStep(Exercise.ViewData data, int stepIndex, boolean needToReset) {
        // Note: 介绍模式下，仅需要更新步骤列表即可，因为，仅涉及主题样式的更新
        updateSteps(data);
    }
}
