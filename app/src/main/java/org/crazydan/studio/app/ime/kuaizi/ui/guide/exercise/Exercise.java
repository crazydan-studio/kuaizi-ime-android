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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
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
        return (index < 9 ? "0" : "") + (index + 1) + ". " + title;
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
                    return "<img src=\"" + this.keyImageRender.withKey((Key) arg) + "\"/>";
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
        boolean restarted = this.currentStep == null;
        if (this.currentStep != null) {
            this.currentStep.reset();
        }

        this.currentStep = step;
        if (this.currentStep != null) {
            this.currentStep.activate();
        }

        on_Step_Start_Done_Msg(this.currentStep, restarted);
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

        Key key = msg.data().key;

        switch (msg.type) {
            case InputChars_Input_Doing:
                if (key == null || key.value == null) {
                    return;
                }
                break;
            case Keyboard_State_Change_Done:
                if (key == null) {
                    return;
                }
                break;
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

        this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                msg.getClass().getSimpleName(), current.getClass().getSimpleName()
        });

        current.onMsg(msg);

        this.log.endTreeLog();
    }

    private void on_Step_Start_Done_Msg(ExerciseStep step, boolean restarted) {
        if (this.listener == null) {
            return;
        }

        int stepIndex = this.steps.indexOf(step);
        ExerciseMsgData msgData = new ExerciseStepStartDoneMsgData(this, stepIndex, restarted);
        ExerciseMsg msg = new ExerciseMsg(ExerciseMsgType.Step_Start_Done, msgData);
        this.listener.onMsg(msg);
    }

    // ================ End: 消息处理 =================

    public static class ViewData {
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
    }
}
