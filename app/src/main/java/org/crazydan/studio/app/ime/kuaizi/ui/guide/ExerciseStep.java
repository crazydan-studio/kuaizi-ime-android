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

package org.crazydan.studio.app.ime.kuaizi.ui.guide;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import org.crazydan.studio.app.ime.kuaizi.internal.ViewData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStep implements ViewData {
    public final String content;
    public final Action action;
    public final ImageGetter imageGetter;

    public final List<ExerciseStep> subs = new ArrayList<>();

    private Status status;

    public static ExerciseStep create(String content, Action action, ImageGetter imageGetter) {
        return new ExerciseStep(content, action, imageGetter);
    }

    private ExerciseStep(String content, Action action, ImageGetter imageGetter) {
        this.content = content;
        this.action = action;
        this.imageGetter = imageGetter;
    }

    public void reset() {
        this.status = Status.noop;
    }

    public void start() {
        this.status = Status.running;
    }

    public void restart() {
        start();
    }

    public void finish() {
        this.status = Status.finished;
    }

    public boolean onInputMsg(InputMsg msg, InputMsgData data) {
        if (this.action == null) {
            return false;
        }

        return this.action.onInputMsg(this, msg, data);
    }

    /** 返回当前对象本身 */
    public ExerciseStep subStep(String content) {
        return subStep(content, null);
    }

    /** 返回当前对象本身 */
    public ExerciseStep subStep(String content, Action action) {
        ExerciseStep step = create(content, action, this.imageGetter);
        this.subs.add(step);

        return this;
    }

    public boolean isRunnable() {
        return this.action != null;
    }

    public boolean isRunning() {
        return this.status == Status.running;
    }

    public boolean isFinished() {
        return this.status == Status.finished;
    }

    @Override
    public boolean isSameWith(Object o) {
        return false;
    }

    public interface Action {
        boolean onInputMsg(ExerciseStep step, InputMsg msg, InputMsgData data);
    }

    public enum Status {
        noop,
        running,
        finished,
    }

    public interface ImageGetter {
        Drawable get(String id, int width, int height);
    }
}
