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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewData;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.Step;

/**
 * 练习题
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class Exercise implements RecyclerViewData, InputMsgListener {
    public final Mode mode;
    public final String title;
    public final List<Step> steps = new ArrayList<>();
    private final Function<Key<?>, String> renderKeyRegister;

    private boolean enableXInputPad;
    private String sampleText;

    private Step runningStep;

    private ProgressListener progressListener;

    public static Exercise free(String title) {
        return new Exercise(Mode.free, title, null);
    }

    public static Exercise normal(String title, Function<Key<?>, String> renderKeyRegister) {
        return new Exercise(Mode.normal, title, renderKeyRegister);
    }

    public static Exercise introduce(String title, Function<Key<?>, String> renderKeyRegister) {
        return new Exercise(Mode.introduce, title, renderKeyRegister);
    }

    Exercise(Mode mode, String title, Function<Key<?>, String> renderKeyRegister) {
        this.mode = mode;
        this.title = title;
        this.renderKeyRegister = renderKeyRegister;
    }

    public void restart() {
        reset();
        gotoNextStep();
    }

    public void reset() {
        this.runningStep = null;
        for (Step step : this.steps) {
            step.reset();
        }
    }

    @Override
    public void onMsg(InputMsg msg) {
        Step current = this.runningStep;
        if (current == null) {
            return;
        }

        Key<?> key = msg.data.key;
        switch (msg.type) {
            case InputChars_Input_Doing:
                if (key == null || key.getText() == null) {
                    return;
                } else {
                    break;
                }
            case Keyboard_State_Change_Done:
                if (key == null) {
                    return;
                } else {
                    break;
                }
        }

        current.onInputMsg(msg);
    }

    public void gotoNextStep() {
        gotoStep(nextRunnableStep(this.runningStep));
    }

    public void gotoStep(String name) {
        gotoStep(getRunningStep(name));
    }

    /** 在模板参数为 {@link Key} 时，自动转换为 &lt;img/&gt; 图片标签 */
    public Step newStep(String content, Object... args) {
        Step step = new Step();
        addStep(0, step);

        if (args.length > 0) {
            content = String.format(content, Arrays.stream(args).map((arg) -> {
                if (arg instanceof Key) {
                    return "<img src=\"" + this.renderKeyRegister.apply((Key<?>) arg) + "\"/>";
                }
                return arg;
            }).toArray());
        }
        return step.content(content);
    }

    public Step newStep(Object[] args) {
        return newStep((String) args[0], Arrays.copyOfRange(args, 1, args.length));
    }

    public void addStep(Step step) {
        addStep(0, step);
    }

    public void addStep(int offset, Step step) {
        int index = this.steps.size() + offset;
        this.steps.add(index, step);
    }

    public boolean isEnableXInputPad() {
        return this.enableXInputPad;
    }

    public void setEnableXInputPad(boolean enableXInputPad) {
        this.enableXInputPad = enableXInputPad;
    }

    public String getSampleText() {
        return this.sampleText;
    }

    public void setSampleText(String sampleText) {
        this.sampleText = sampleText;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    public boolean isSameWith(Object o) {
        return false;
    }

    private void gotoStep(Step step) {
        if (this.runningStep != null) {
            this.runningStep.reset();
        }

        this.runningStep = step;
        if (this.runningStep != null) {
            this.runningStep.start();
        }

        fireProgressListener(this.runningStep);
    }

    private void fireProgressListener(Step step) {
        if (this.progressListener != null && step != null) {
            this.progressListener.onStep(step, this.steps.indexOf(step));
        }
    }

    private Step getRunningStep(String name) {
        for (Step step : this.steps) {
            if (Objects.equals(step.name(), name) && step.isRunnable()) {
                return step;
            }
        }
        return null;
    }

    private Step nextRunnableStep(Step prev) {
        int start = this.steps.indexOf(prev);
        for (int i = start + 1; i < this.steps.size(); i++) {
            Step step = this.steps.get(i);
            if (step.isRunnable()) {
                return step;
            }
        }
        return null;
    }

    public enum Mode {
        /** 自由模式 */
        free,
        /** 互动模式 */
        normal,
        /** 介绍模式，无互动 */
        introduce,
    }

    public interface ProgressListener {
        void onStep(Step step, int position);
    }
}
