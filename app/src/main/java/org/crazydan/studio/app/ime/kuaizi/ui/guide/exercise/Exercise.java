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
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewData;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.KeyImageRender;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsg;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgType;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.data.ExerciseStepStartDoneMsgData;

/**
 * 练习题
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class Exercise implements InputMsgListener {
    protected final Logger log = Logger.getLogger(getClass());

    public enum Mode {
        /** 自由模式 */
        free,
        /** 互动模式 */
        normal,
        /** 介绍模式，无互动 */
        introduce,
    }

    private final Mode mode;
    private final String title;
    private final KeyImageRender keyImageRender;
    private final List<ExerciseStep> steps = new ArrayList<>();

    /** 输入框内的演示内容 */
    private String sampleText;

    private boolean xInputPadEnabled;
    private ExerciseStep currentStep;
    private ExerciseMsgListener listener;

    public static Exercise free(String title) {
        return new Exercise(Mode.free, title, null);
    }

    public static Exercise normal(String title, KeyImageRender keyImageRender) {
        return new Exercise(Mode.normal, title, keyImageRender);
    }

    public static Exercise introduce(String title, KeyImageRender keyImageRender) {
        return new Exercise(Mode.introduce, title, keyImageRender);
    }

    public static String createViewTitle(String title, int index) {
        return (index + 1) + ". " + title;
    }

    Exercise(Mode mode, String title, KeyImageRender keyImageRender) {
        this.mode = mode;
        this.title = title;
        this.keyImageRender = keyImageRender;
    }

    public Mode getMode() {
        return this.mode;
    }

    public String getTitle() {
        return this.title;
    }

    public boolean isXInputPadEnabled() {
        return this.xInputPadEnabled;
    }

    public void enableXInputPad() {
        this.xInputPadEnabled = true;
    }

    public void setSampleText(String sampleText) {
        this.sampleText = sampleText;
    }

    public Exercise.ViewData createViewData() {
        return new ViewData(this);
    }

    /** 在模板参数为 {@link Key} 时，自动转换为 &lt;img/&gt; 图片标签 */
    public ExerciseStep newStep(String content, Object... args) {
        ExerciseStep step = new ExerciseStep();
        addStep(0, step);

        if (args.length > 0) {
            content = String.format(content, Arrays.stream(args).map((arg) -> {
                if (arg instanceof Key) {
                    return "<img src=\"" + this.keyImageRender.withKey((Key<?>) arg) + "\"/>";
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

    /** 更新步骤样式 */
    public void updateStepTheme(Object theme) {
        this.steps.forEach((step) -> step.theme(theme));
    }

    // ================ Start: 驱动练习 ================

    public void restart() {
        this.currentStep = null;
        for (ExerciseStep step : this.steps) {
            step.reset();
        }

        gotoNextStep();
    }

    public void gotoStep(String name) {
        ExerciseStep step = getRunnableStep(name);
        gotoStep(step);
    }

    public void gotoNextStep() {
        ExerciseStep step = getNextRunnableStep(this.currentStep);
        gotoStep(step);
    }

    private void gotoStep(ExerciseStep step) {
        if (this.currentStep != null) {
            this.currentStep.reset();
        }

        this.currentStep = step;
        if (this.currentStep != null) {
            this.currentStep.activate();
        }

        on_Step_Start_Done_Msg(this.currentStep);
    }

    private ExerciseStep getRunnableStep(String name) {
        for (ExerciseStep step : this.steps) {
            if (step.hasSameName(name) && step.isRunnable()) {
                return step;
            }
        }
        return null;
    }

    private ExerciseStep getNextRunnableStep(ExerciseStep prev) {
        int start = this.steps.indexOf(prev);
        for (int i = start + 1; i < this.steps.size(); i++) {
            ExerciseStep step = this.steps.get(i);
            if (step.isRunnable()) {
                return step;
            }
        }
        return null;
    }

    // ================ End: 驱动练习 ================

    // ================ Start: 消息处理 =================

    public void setListener(ExerciseMsgListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMsg(InputMsg msg) {
        ExerciseStep current = this.currentStep;
        if (current == null) {
            return;
        }

        Key<?> key = msg.data.key;
        this.log.debug("On Input message: %s - %s", msg.type, key);

        switch (msg.type) {
            case InputChars_Input_Doing:
                if (key == null || key.getText() == null) {
                    return;
                } else {
                    break;
                }
//            case Keyboard_State_Change_Done:
//                if (key == null) {
//                    return;
//                } else {
//                    break;
//                }
            case Editor_Range_Select_Doing:
            case Editor_Cursor_Move_Doing:
            case InputList_Committed_Revoke_Doing:
            case Editor_Edit_Doing:
            case Keyboard_Switch_Done:
            case Keyboard_XPad_Simulation_Terminated:
            case InputList_Commit_Doing:
            case InputCandidate_Choose_Done:
            case InputCandidate_Choose_Doing:
            case InputChars_Input_Done: {
                break;
            }
            default: {
                return;
            }
        }

        this.log.debug("Pass Input message %s to active step", msg.type);
        current.onMsg(msg);
    }

    private void on_Step_Start_Done_Msg(ExerciseStep step) {
        if (this.listener == null || step == null) {
            return;
        }

        ExerciseMsgData msgData = new ExerciseStepStartDoneMsgData(this, this.steps.indexOf(step));
        ExerciseMsg msg = new ExerciseMsg(ExerciseMsgType.Step_Start_Done, msgData);
        this.listener.onMsg(msg);
    }

    // ================ End: 消息处理 =================

    public static class ViewData implements RecyclerViewData {
        public final KeyImageRender keyImageRender;

        public final Mode mode;
        public final String title;
        /** 输入框内的演示内容 */
        public final String sampleText;

        public final List<ExerciseStep.ViewData> steps;

        ViewData(Exercise exercise) {
            this.keyImageRender = exercise.keyImageRender;
            this.mode = exercise.mode;
            this.title = exercise.title;
            this.sampleText = exercise.sampleText;
            this.steps = exercise.steps.stream().map(ExerciseStep::createViewData).collect(Collectors.toList());
        }

        @Override
        public boolean isSameWith(Object o) {
            return false;
        }
    }
}
