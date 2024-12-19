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

import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;

/**
 * {@link Exercise} 的步骤
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStep implements RecyclerViewData {
    private String name;
    private String content;
    private Action action;

    private boolean running;

    // ================== Start: 链式调用 ==================

    public String name() {
        return this.name;
    }

    public ExerciseStep name(String name) {
        this.name = name;
        return this;
    }

    public String content() {
        return this.content;
    }

    public ExerciseStep content(String content) {
        this.content = content;
        return this;
    }

    public Action action() {
        return this.action;
    }

    public ExerciseStep action(Action action) {
        this.action = action;
        return this;
    }

    // ================== End: 链式调用 ==================

    public void reset() {
        this.running = false;
    }

    public void start() {
        this.running = true;

        if (this.action instanceof AutoAction) {
            ((AutoAction) this.action).start();
        }
    }

    public void onInputMsg(InputMsg msg) {
        if (this.action != null) {
            this.action.onInputMsg(msg);
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
        void onInputMsg(InputMsg msg);
    }

    public interface AutoAction extends Action {
        void start();

        @Override
        default void onInputMsg(InputMsg msg) {}
    }

    public static class Final extends ExerciseStep {
        public final Runnable restartCallback;
        public final Runnable continueCallback;

        public Final(Runnable restartCallback, Runnable continueCallback) {
            this.restartCallback = restartCallback;
            this.continueCallback = continueCallback;
        }
    }
}
