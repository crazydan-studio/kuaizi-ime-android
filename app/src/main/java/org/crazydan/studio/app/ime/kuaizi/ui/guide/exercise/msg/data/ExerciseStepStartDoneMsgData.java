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
