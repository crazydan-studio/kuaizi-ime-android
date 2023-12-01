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

import android.graphics.drawable.Drawable;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.ViewData;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStep implements ViewData {
    public final String name;
    public final String content;
    public final Action action;
    public final ImageGetter imageGetter;

    private boolean running;

    private ExerciseStep(String name, String content, Action action, ImageGetter imageGetter) {
        this.name = name;
        this.content = content;
        this.action = action;
        this.imageGetter = imageGetter;
    }

    public static ExerciseStep create(String name, String content, Action action, ImageGetter imageGetter) {
        return new ExerciseStep(name, content, action, imageGetter);
    }

    public void reset() {
        this.running = false;
    }

    public void start() {
        this.running = true;

        if (this.action instanceof AutoAction) {
            ((AutoAction) this.action).run();
        }
    }

    public void onInputMsg(InputMsg msg, InputMsgData data) {
        if (this.action != null) {
            this.action.onInputMsg(msg, data);
        }
    }

    public boolean isRunnable() {
        return this.action != null;
    }

    public boolean isRunning() {
        return this.running;
    }

    @Override
    public boolean isSameWith(Object o) {
        return false;
    }

    public interface Action {
        void onInputMsg(InputMsg msg, InputMsgData data);
    }

    public interface AutoAction extends Action {
        void run();

        @Override
        default void onInputMsg(InputMsg msg, InputMsgData data) {}
    }

    public interface ImageGetter {
        Drawable get(String id, int width, int height);
    }

    public static class Final extends ExerciseStep {
        public final Runnable restartCallback;
        public final Runnable continueCallback;

        public Final(Runnable restartCallback, Runnable continueCallback) {
            super(null, null, null, null);

            this.restartCallback = restartCallback;
            this.continueCallback = continueCallback;
        }
    }
}
