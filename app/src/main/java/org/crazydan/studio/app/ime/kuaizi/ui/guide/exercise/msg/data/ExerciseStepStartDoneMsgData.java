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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.data;

import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.Exercise;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgType;

/**
 * {@link ExerciseMsgType#Step_Start_Done} 消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-20
 */
public class ExerciseStepStartDoneMsgData extends ExerciseMsgData {
    /** 当前的练习 */
    public final Exercise exercise;
    /** 已开始步骤的序号，在无可运行的步骤时（主要针对自由模式与介绍模式），该值为 -1 */
    public final int stepIndex;
    /** 是否为已重启练习 */
    public final boolean restarted;

    public ExerciseStepStartDoneMsgData(Exercise exercise, int stepIndex, boolean restarted) {
        this.exercise = exercise;
        this.stepIndex = stepIndex;
        this.restarted = restarted;
    }
}
