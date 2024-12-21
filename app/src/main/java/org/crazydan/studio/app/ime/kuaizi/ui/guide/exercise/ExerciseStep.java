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

import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;

/**
 * {@link Exercise} 的步骤
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStep implements InputMsgListener {
    private String name;
    private String content;
    private Object theme;
    private Action action;

    private boolean active;

    public ViewData createViewData() {
        return new ViewData(this);
    }

    // ================== Start: 链式调用 ==================

    public ExerciseStep name(String name) {
        this.name = name;
        return this;
    }

    public ExerciseStep content(String content) {
        this.content = content;
        return this;
    }

    public ExerciseStep theme(Object theme) {
        this.theme = theme;
        return this;
    }

    public ExerciseStep action(Action action) {
        this.action = action;
        return this;
    }

    public boolean hasSameName(String name) {
        return Objects.equals(this.name, name);
    }

    public boolean isRunnable() {
        return this.action != null;
    }

    // ================== End: 链式调用 ==================

    @Override
    public void onMsg(InputMsg msg) {
        if (this.action != null) {
            this.action.onMsg(msg);
        }
    }

    public void reset() {
        this.active = false;
    }

    public void activate() {
        this.active = true;

        if (this.action instanceof AutoAction) {
            ((AutoAction) this.action).start();
        }
    }

    public interface Action extends InputMsgListener {}

    public interface AutoAction extends Action {
        void start();

        @Override
        default void onMsg(InputMsg msg) {}
    }

    /** 最后一步 */
    public static class Last extends ExerciseStep {
        private final Runnable restartCallback;
        private final Runnable continueCallback;

        public Last(Runnable restartCallback, Runnable continueCallback) {
            this.restartCallback = restartCallback;
            this.continueCallback = continueCallback;
        }
    }

    public static class ViewData implements RecyclerViewData {
        public final String name;
        public final String content;
        /** 主题样式信息，主要用于触发视图更新 */
        public final Object theme;

        /** 是否已激活 */
        public final boolean active;
        /** 是否为最后一个步骤 */
        public final boolean last;

        public final Runnable restartCallback;
        public final Runnable continueCallback;

        ViewData(ExerciseStep step) {
            this.name = step.name;
            this.content = step.content;
            this.theme = step.theme;
            this.active = step.active;

            this.last = step instanceof Last;
            this.restartCallback = this.last ? ((Last) step).restartCallback : null;
            this.continueCallback = this.last ? ((Last) step).continueCallback : null;
        }

        @Override
        public boolean isSameWith(Object o) {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ViewData that = (ViewData) o;
            return this.active == that.active
                   && this.last == that.last
                   && Objects.equals(this.name, that.name)
                   && Objects.equals(this.content, that.content)
                   && Objects.equals(this.theme, that.theme);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.content, this.theme, this.active, this.last);
        }
    }
}
