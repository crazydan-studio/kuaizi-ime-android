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
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;

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
