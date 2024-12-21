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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import android.text.Html;
import android.text.Spanned;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.KeyImageRender;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

/**
 * {@link Exercise} 的步骤
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStep implements RecyclerViewData, InputMsgListener {
    private final KeyImageRender keyImageRender;
    /** 缓存显示文本 */
    private final Map<String, Spanned> spannedCache = new HashMap<>();

    private String name;
    private String content;
    private Action action;

    private boolean running;

    public ExerciseStep(KeyImageRender keyImageRender) {
        this.keyImageRender = keyImageRender;
    }

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

    public boolean running() {
        return this.running;
    }

    public boolean runnable() {
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
        this.running = false;
    }

    public void start() {
        this.running = true;

        if (this.action instanceof AutoAction) {
            ((AutoAction) this.action).start();
        }
    }

    public Spanned renderText(String text, int imageSize) {
        return this.spannedCache.computeIfAbsent(text, (k) -> {
            //
            return Html.fromHtml(text, FROM_HTML_MODE_COMPACT, (source) -> {
                if (this.keyImageRender == null) {
                    return null;
                }
                return this.keyImageRender.renderKey(source, imageSize, imageSize);
            }, null);
        });
    }

    @Override
    public boolean isSameWith(Object o) {
        return equals(o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExerciseStep that = (ExerciseStep) o;
        return this.running == that.running //
               && Objects.equals(this.name, that.name) //
               && Objects.equals(this.content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.content, this.running);
    }

    public interface Action extends InputMsgListener {}

    public interface AutoAction extends Action {
        void start();

        @Override
        default void onMsg(InputMsg msg) {}
    }

    public static class Final extends ExerciseStep {
        public final Runnable restartCallback;
        public final Runnable continueCallback;

        public Final(Runnable restartCallback, Runnable continueCallback) {
            super(null);

            this.restartCallback = restartCallback;
            this.continueCallback = continueCallback;
        }
    }
}
