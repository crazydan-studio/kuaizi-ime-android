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

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.ConfigUpdateMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsg;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgType;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.data.ExerciseListStartDoneMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.data.ExerciseThemeUpdateDoneMsgData;

/**
 * {@link Exercise} 列表
 * <p/>
 * 负责管理和维护所有的练习题
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-20
 */
public class ExerciseList implements InputMsgListener, ExerciseMsgListener {
    private List<Exercise> exercises;

    /** 已激活的 {@link Exercise} */
    private Exercise active;
    private ExerciseMsgListener listener;

    public Exercise get(int index) {
        return this.exercises.get(index);
    }

    /** 激活指定位置的 {@link Exercise} */
    public void activateAt(int index) {
        if (this.active != null) {
            this.active.setListener(null);
        }

        this.active = get(index);
        this.active.setListener(this);
        this.active.restart();
    }

    /** 启动练习 */
    public void start(List<Exercise> exercises) {
        this.exercises = exercises;

        ExerciseMsgData msgData = new ExerciseListStartDoneMsgData(exercises);
        ExerciseMsg msg = new ExerciseMsg(ExerciseMsgType.List_Start_Done, msgData);
        onMsg(msg);
    }

    // ================ Start: 消息处理 =================

    public void setListener(ExerciseMsgListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMsg(InputMsg msg) {
        if (msg.type == InputMsgType.Config_Update_Done) {
            ConfigUpdateMsgData data = msg.data();
            if (data.key == ConfigKey.theme) {
                this.exercises.forEach((exercise) -> {
                    exercise.updateStepTheme(data.newValue);
                });

                onMsg(new ExerciseMsg(ExerciseMsgType.Theme_Update_Done,
                                      new ExerciseThemeUpdateDoneMsgData(this.active)));
            }
        }

        if (this.active != null) {
            this.active.onMsg(msg);
        }
    }

    @Override
    public void onMsg(ExerciseMsg msg) {
        this.listener.onMsg(msg);
    }

    // ================ End: 消息处理 =================
}
