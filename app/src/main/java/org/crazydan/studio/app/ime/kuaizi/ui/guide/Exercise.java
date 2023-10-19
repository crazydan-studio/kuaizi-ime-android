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
import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.ViewData;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class Exercise implements ViewData, InputMsgListener {
    public final Mode mode;
    public final String title;
    public final List<ExerciseStep> steps = new ArrayList<>();

    private final ExerciseStep.ImageGetter imageGetter;

    private boolean disableUserInputData;
    private String sampleText;
    private ProgressListener progressListener;
    private ExerciseStep runningStep;

    private Exercise(Mode mode, String title, ExerciseStep.ImageGetter imageGetter) {
        this.mode = mode;
        this.title = title;
        this.imageGetter = imageGetter;
    }

    public static Exercise free(String title) {
        return new Exercise(Mode.free, title, null);
    }

    public static Exercise normal(String title, ExerciseStep.ImageGetter imageGetter) {
        return new Exercise(Mode.normal, title, imageGetter);
    }

    public static Exercise introduce(String title, ExerciseStep.ImageGetter imageGetter) {
        return new Exercise(Mode.introduce, title, imageGetter);
    }

    public void start() {
        gotoNextStep();
    }

    public void restart() {
        doReset();
        start();
    }

    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        ExerciseStep current = this.runningStep;
        if (current == null) {
            return;
        }

        switch (msg) {
            case InputChars_Input_Doing:
                if (data.getKey() == null || data.getKey().getText() == null) {
                    return;
                } else {
                    break;
                }
            case Keyboard_State_Change_Done:
                if (data.getKey() == null) {
                    return;
                } else {
                    break;
                }
            case InputAudio_Play_Doing:
            case InputList_Clean_Done:
            case InputList_Cleaned_Cancel_Done:
            case InputList_Input_Choose_Done:
            case InputList_Input_Completion_Update_Done:
            case InputList_Input_Completion_Apply_Done:
            case Keyboard_Config_Update_Done:
            case Keyboard_Theme_Update_Done:
                return;
        }

        current.onInputMsg(msg, data);
    }

    public void gotoNextStep() {
        gotoStep(nextRunnableStep(this.runningStep));
    }

    public void gotoStep(String name) {
        gotoStep(getRunningStep(name));
    }

    public ExerciseStep addStep(String content) {
        return addStep(content, null);
    }

    public ExerciseStep addStep(String content, ExerciseStep.Action action) {
        return addStep(null, content, action);
    }

    public ExerciseStep addStep(String name, String content, ExerciseStep.Action action) {
        ExerciseStep step = ExerciseStep.create(name, content, action, this.imageGetter);

        return addStep(step);
    }

    public ExerciseStep addStep(ExerciseStep step) {
        this.steps.add(step);
        return step;
    }

    public boolean isDisableUserInputData() {
        return this.disableUserInputData;
    }

    public void setDisableUserInputData(boolean disableUserInputData) {
        this.disableUserInputData = disableUserInputData;
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

    private void doReset() {
        this.runningStep = null;
        for (ExerciseStep step : this.steps) {
            step.reset();
        }
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
            if (Objects.equals(step.name, name) && step.isRunnable()) {
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
        free,
        normal,
        introduce,
    }

    public interface ProgressListener {
        void onStep(ExerciseStep step, int position);
    }
}
