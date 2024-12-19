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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewData;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;

/**
 * 练习题
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class Exercise implements RecyclerViewData, InputMsgListener {
    public final Mode mode;
    public final String title;
    public final List<ExerciseStep> steps = new ArrayList<>();
    private final Function<Key<?>, String> renderKeyRegister;

    private boolean enableXInputPad;
    private String sampleText;

    private ExerciseStep runningStep;

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
        for (ExerciseStep step : this.steps) {
            step.reset();
        }
    }

    @Override
    public void onMsg(InputMsg msg) {
        ExerciseStep current = this.runningStep;
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
    public ExerciseStep newStep(String content, Object... args) {
        ExerciseStep step = new ExerciseStep();
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

    public ExerciseStep newStep(Object[] args) {
        return newStep((String) args[0], Arrays.copyOfRange(args, 1, args.length));
    }

    public void addStep(ExerciseStep step) {
        addStep(0, step);
    }

    public void addStep(int offset, ExerciseStep step) {
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

    private void gotoStep(ExerciseStep step) {
        if (this.runningStep != null) {
            this.runningStep.reset();
        }

        this.runningStep = step;
        if (this.runningStep != null) {
            this.runningStep.start();
        }

        fireProgressListener(this.runningStep);
    }

    private void fireProgressListener(ExerciseStep step) {
        if (this.progressListener != null && step != null) {
            this.progressListener.onStep(step, this.steps.indexOf(step));
        }
    }

    private ExerciseStep getRunningStep(String name) {
        for (ExerciseStep step : this.steps) {
            if (Objects.equals(step.name(), name) && step.isRunnable()) {
                return step;
            }
        }
        return null;
    }

    private ExerciseStep nextRunnableStep(ExerciseStep prev) {
        int start = this.steps.indexOf(prev);
        for (int i = start + 1; i < this.steps.size(); i++) {
            ExerciseStep step = this.steps.get(i);
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
        void onStep(ExerciseStep step, int position);
    }
}
