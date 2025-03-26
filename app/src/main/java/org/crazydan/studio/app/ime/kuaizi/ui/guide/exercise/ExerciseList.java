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

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsg;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgType;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.data.ExerciseListStartDoneMsgData;

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
