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
import java.util.Locale;

import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.widget.Toast;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCandidateChoosingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.FollowSystemThemeActivity;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.Alert;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.DynamicLayoutSandboxView;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.RecyclerPageIndicatorView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-11
 */
public class ExerciseMain extends FollowSystemThemeActivity {
    private ImeInputView imeView;
    private ExerciseListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_exercise_main_activity);

        this.imeView = findViewById(R.id.ime_view);
        this.imeView.startInput(Keyboard.Type.Pinyin);

        int imeThemeResId = this.imeView.getKeyboardConfig().getThemeResId();
        DynamicLayoutSandboxView sandboxView = findViewById(R.id.step_image_sandbox_view);
        List<Exercise> exercises = sandboxView.withMutation(imeThemeResId, () -> createExercises(sandboxView));

        this.listView = findViewById(R.id.exercise_list_view);
        this.listView.adapter.bind(exercises);

        this.listView.setExerciseActiveListener((exerciseView) -> {
            exerciseView.withIme(this.imeView);

            this.imeView.startInput(Keyboard.Type.Pinyin);
        });

        RecyclerPageIndicatorView indicatorView = findViewById(R.id.exercise_list_indicator_view);
        indicatorView.attachTo(this.listView);

        this.listView.active(1);
    }

    @Override
    protected void onStart() {
        // 确保拼音字典库保持就绪状态
        PinyinDictDB.getInstance().open(getApplicationContext());

        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 确保拼音字典库能够被及时关闭
        PinyinDictDB.getInstance().close();
    }

    private List<Exercise> createExercises(DynamicLayoutSandboxView sandboxView) {
        List<Exercise> exercises = new ArrayList<>();

        Exercise exercise = Exercise.free("自由练习");
        exercises.add(exercise);

        exercise = exercise_Pinyin_Inputting(sandboxView);
        exercises.add(exercise);

        exercise = Exercise.normal("文本编辑", null);
        exercises.add(exercise);

        exercise = Exercise.normal("字母切换", null);
        exercises.add(exercise);

        exercise = Exercise.normal("符号输入", null);
        exercises.add(exercise);

        exercise = Exercise.normal("计算输入", null);
        exercises.add(exercise);

        return exercises;
    }

    private Exercise exercise_Pinyin_Inputting(DynamicLayoutSandboxView sandboxView) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        InputWord candidate = new InputWord("kuai", "筷", "kuài");
        Key<?> key_candidate = keyTable.inputWordKey(candidate, 0);

        Key<?> key_k = keyTable.level0CharKey("k");
        Key<?> key_u = keyTable.level1CharKey("u");
        Key<?> key_kuai = keyTable.level2CharKey(key_k.getText(), "uai");
        Key<?> key_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);

        String expectedAutoWord = "块";
        String notSelectExpectedAutoWordMsgText = "请按照指导步骤选中键盘上方的候选字 <big><b>%s</b></big>";

        Exercise exercise = Exercise.normal("拼音输入（基础）", sandboxView::getImage);
        exercise.setDisableUserInputData(true);

        exercise.addStep("本次练习输入：<b>筷(kuai)</b>；");
        exercise.addStep("<b>提示</b>：在拼音输入过程中，手指可随意滑过其他按键，只需要确保手指释放前输入了完整的拼音即可；");
        exercise.addStep("input_k",
                         "请将手指放在下方键盘的按键<img src=\""
                         + sandboxView.withKey(key_k)
                         + "\"/>上，并让手指贴着屏幕从该按键上滑出；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_Inputting: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (key.getLabel().equals("k")) {
                                         exercise.gotoNextStep();
                                     } else {
                                         warning("请按照指导步骤从按键 <big><b>%s</b></big> 开始滑出", "k");
                                     }
                                     break;
                                 }
                                 case InputCandidate_Choosing: {
                                     CharInput input = ((InputCandidateChoosingMsgData) data).input;
                                     InputWord word = input.getWord();

                                     if (word != null && expectedAutoWord.equals(word.getValue())) {
                                         exercise.gotoStep("choose_correct_word");
                                     } else {
                                         warning(notSelectExpectedAutoWordMsgText, expectedAutoWord);
                                     }
                                     break;
                                 }
                             }
                         });
        exercise.addStep("input_u",
                         "手指不要离开屏幕，继续将手指滑到按键<img src=\""
                         + sandboxView.withKey(key_u)
                         + "\"/>上，再从该按键上滑出；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_Inputting: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (key.getLabel().equals("u")) {
                                         exercise.gotoNextStep();
                                     } else {
                                         warning("请按照指导步骤滑到按键 <big><b>%s</b></big> 上", "u");
                                     }
                                     break;
                                 }
                             }
                         });
        exercise.addStep("input_kuai",
                         "将手指滑到按键<img src=\"" + sandboxView.withKey(key_kuai) + "\"/>上，并就地释放手指；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_Inputting: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (!key.getLabel().startsWith("ku")) {
                                         exercise.gotoStep("input_u");

                                         warning("请按照指导步骤滑到按键 <big><b>%s</b></big> 上，再从其上滑出", "u");
                                     }
                                     break;
                                 }
                                 case InputChars_InputtingEnd: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (key != null && key.getLabel().equals("kuai")) {
                                         exercise.gotoNextStep();
                                     } else {
                                         exercise.restart();
                                         warning("当前输入的拼音与练习内容不符，请按照指导步骤重新输入");
                                     }
                                     break;
                                 }
                             }
                         });
        exercise.addStep("select_auto_word", "请点击键盘上方的候选字 <b>" + expectedAutoWord + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputCandidate_Choosing: {
                    CharInput input = ((InputCandidateChoosingMsgData) data).input;
                    InputWord word = input.getWord();

                    if (word != null && expectedAutoWord.equals(word.getValue())) {
                        exercise.gotoNextStep();
                    } else {
                        warning(notSelectExpectedAutoWordMsgText, expectedAutoWord);
                    }
                    break;
                }
            }
        });
        exercise.addStep("choose_correct_word",
                         "在候选字列表区域点击正确的候选字<img src=\""
                         + sandboxView.withKey(key_candidate)
                         + "\"/>（在该区域内可上下翻页）；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputCandidate_Chosen: {
                                     CharInput input = ((InputCandidateChoosingMsgData) data).input;
                                     InputWord word = input.getWord();

                                     if (word != null && candidate.getValue().equals(word.getValue())) {
                                         exercise.gotoNextStep();
                                     } else {
                                         exercise.gotoStep("select_auto_word");
                                         warning("当前选择的候选字与练习内容不符，请按照指导步骤选择 <big><b>%s</b></big>",
                                                 candidate.getValue());
                                     }
                                     break;
                                 }
                             }
                         });
        exercise.addStep("点击键盘中的提交按键<img src=\""
                         + sandboxView.withKey(key_commit)
                         + "\"/>，将当前输入提交至目标输入框；", (msg, data) -> {
            switch (msg) {
                case InputList_Committing: {
                    exercise.gotoNextStep();
                    confirm(exercise);
                    break;
                }
            }
        });
        exercise.addStep("本次练习已结束，您可以开始后续练习或者继续当前练习。", (msg, data) -> {});

//        exercise.addStep("暂时不管已经输入拼音的候选字是否正确，先继续按照上述过程输入其他几个字。"
//                         + "<b>注</b>：在输入 <b>输(shu)</b> 时，手指需从按键<img src=\""
//                         + sandboxView.withKey(key_sh)
//                         + "\"/>上滑出", null);
//        exercise.addStep("输入完成后，开始选择已输入拼音的候选字：")
//                .subStep("手指点击以选中按键上方输入框中的<img src=\"筷(kuai)\"/>字", null)
//                .subStep("在出现的候选字列表区域，可快速向上或向下滑动手指进行翻页，并手指点击正确的候选字，以完成选字",
//                         null)
//                .subStep("<b>提示</b>：若默认候选字已经是正确的，则可点击<img src=\"ok\"/>按键以确认该字；"
//                         + "<img src=\"submit\"/>为输入提交按键，点击后会将当前输入提交至目标输入中；"
//                         + "<img src=\"delete\"/>为删除当前拼音，点击后将从输入中删除拼音并回到拼音键盘以接受新的输入；")
//                .subStep("接着选择拼音 <b>zi</b> 的候选字", null)
//                .subStep("在该字的候选字列表中点击<img src=\"z->zh\"/>按键以切换该拼音的平/翘舌。"
//                         + "<b>注</b>：确保最终拼音依然为 <b>zi</b>", null)
//                .subStep("点击候选字列表上方的<img src=\"横\"/>等笔画过滤按键，以按候选字所包含的笔画快速过滤出正确的候选字。"
//                         + "<b>注</b>：单击笔画过滤按键为增加笔画数，在按键上快速滑出为减少笔画数", null)
//                .subStep("继续完成后续拼音的候选字选择和确认操作");
//        exercise.addStep("点击按键上方输入框的空白区域，使键盘输入光标移动到当前输入尾部", null);
//        exercise.addStep("点击键盘中的<img src=\"submit\"/>按键以提交当前输入至目标输入框中", null);
//        exercise.addStep("点击键盘中的<img src=\"revoke\"/>按键将已提交输入撤回。"
//                         + "<b>注</b>：这里仅用于演示对已提交输入的修订支持，本次练习不需要对其做修改", null);
//        exercise.addStep("长按键盘中的<img src=\"submit\"/>按键，再分别点击<img src=\"带拼音\"/>"
//                         + " 等按键以切换提交输入的形式。<b>注</b>：长按<img src=\"submit\"/>按键可退出切换选择，"
//                         + "若直接点击该按键则将会以最终切换后的形式提交输入至目标输入框中", null);
//        exercise.addStep("本次练习已结束，您可以开始后续练习或者继续当前练习", null);

        return exercise;
    }

    private void warning(String msg, Object... args) {
        String text = String.format(Locale.getDefault(), msg, args);

        Toast toast = Toast.makeText(getApplicationContext(),
                                     Html.fromHtml(text, FROM_HTML_MODE_COMPACT),
                                     Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);

        toast.show();
    }

    private void confirm(Exercise exercise) {
        Alert.with(this)
             .setView(R.layout.guide_alert_view)
             .setTitle(R.string.title_tips)
             .setMessage(R.string.msg_guid_exercise_finished_confirm)
             .setNegativeButton(R.string.btn_guide_exercise_try_again, (dialog, which) -> {
                 exercise.restart();
             })
             .setPositiveButton(R.string.btn_guide_exercise_try_new_one, (dialog, which) -> {
                 this.listView.activeNext();
             })
             .show();
    }
}
