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

        exercise = exercise_Basic_Pinyin_Inputting(sandboxView);
        exercises.add(exercise);

        exercise = exercise_Filter_Pinyin_Inputting(sandboxView);
        exercises.add(exercise);

        exercise = exercise_Advance_Pinyin_Inputting(sandboxView);
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

    private Exercise exercise_Basic_Pinyin_Inputting(DynamicLayoutSandboxView sandboxView) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        InputWord case_word = new InputWord("kuai", "筷", "kuài");
        Key<?> key_case_word = keyTable.inputWordKey(case_word, 0);

        Key<?> key_level_0 = keyTable.level0CharKey("k");
        Key<?> key_level_1 = keyTable.level1CharKey("u");
        Key<?> key_level_2 = keyTable.level2CharKey(key_level_0.getText(), "uai");
        Key<?> key_ctrl_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);

        String expectedAutoWord = "块";
        String notSelectExpectedAutoWordMsgText = "请按照指导步骤选中键盘上方的候选字 <big><b>%s</b></big>";

        Exercise exercise = Exercise.normal("拼音输入（基础）", sandboxView::getImage);
        exercise.setDisableUserInputData(true);

        exercise.addStep("本次练习输入 <b>" + case_word.getValue() + "(" + case_word.getNotation() + ")</b>；");
        exercise.addStep("<b>提示</b>：在拼音输入过程中，手指可随意滑过其他按键，只需要确保手指释放前输入了完整的拼音即可；");
        exercise.addStep("input_level_0",
                         "请将手指放在下方键盘的按键<img src=\""
                         + sandboxView.withKey(key_level_0)
                         + "\"/>上，并让手指贴着屏幕从该按键上滑出；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_Inputting: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (key.getLabel().equals(key_level_0.getLabel())) {
                                         exercise.gotoNextStep();
                                     } else {
                                         warning("请按照指导步骤从按键 <big><b>%s</b></big> 开始滑出",
                                                 key_level_0.getLabel());
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
        exercise.addStep("input_level_1",
                         "手指不要离开屏幕，继续将手指滑到按键<img src=\""
                         + sandboxView.withKey(key_level_1)
                         + "\"/>上，再从该按键上滑出；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_Inputting: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (key.getLabel().equals(key_level_1.getLabel())) {
                                         exercise.gotoNextStep();
                                     } else {
                                         warning("请按照指导步骤滑到按键 <big><b>%s</b></big> 上",
                                                 key_level_1.getLabel());
                                     }
                                     break;
                                 }
                             }
                         });
        exercise.addStep("input_level_2",
                         "将手指滑到按键<img src=\"" + sandboxView.withKey(key_level_2) + "\"/>上，并就地释放手指；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_Inputting: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (!key.getLabel().startsWith(key_level_2.getLabel().substring(0, 2))) {
                                         exercise.gotoStep("input_level_1");

                                         warning("请按照指导步骤滑到按键 <big><b>%s</b></big> 上，再从其上滑出",
                                                 key_level_1.getLabel());
                                     }
                                     break;
                                 }
                                 case InputChars_InputtingEnd: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (key != null && key.getLabel().equals(key_level_2.getLabel())) {
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
                         + sandboxView.withKey(key_case_word)
                         + "\"/>（在该区域内可上下翻页）；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputCandidate_Chosen: {
                                     CharInput input = ((InputCandidateChoosingMsgData) data).input;
                                     InputWord word = input.getWord();

                                     if (word != null && case_word.getValue().equals(word.getValue())) {
                                         exercise.gotoNextStep();
                                     } else {
                                         exercise.gotoStep("select_auto_word");
                                         warning("当前选择的候选字与练习内容不符，请按照指导步骤选择 <big><b>%s</b></big>",
                                                 case_word.getValue());
                                     }
                                     break;
                                 }
                             }
                         });
        exercise.addStep("点击键盘中的输入提交按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit)
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

        return exercise;
    }

    private Exercise exercise_Filter_Pinyin_Inputting(DynamicLayoutSandboxView sandboxView) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        InputWord case_word = new InputWord("shu", "输", "shū");
        Key<?> key_case_word = keyTable.inputWordKey(case_word, 0);

        Key<?> key_level_0 = keyTable.level0CharKey("sh");
        Key<?> key_level_1 = keyTable.level1CharKey("u");
        Key<?> key_ctrl_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);
        Key<?> key_ctrl_filter_heng = keyTable.strokeFilterKey("一", null);
        Key<?> key_ctrl_filter_shu = keyTable.strokeFilterKey("丨", null);
        Key<?> key_ctrl_filter_pie = keyTable.strokeFilterKey("丿", null);
        Key<?> key_ctrl_filter_na = keyTable.strokeFilterKey("㇏", null);
        Key<?> key_ctrl_filter_zhe = keyTable.strokeFilterKey("\uD840\uDCCB", null);

        String expectedAutoWord = "术";
        String notSelectExpectedAutoWordMsgText = "请按照指导步骤选中键盘上方的候选字 <big><b>%s</b></big>";

        Exercise exercise = Exercise.normal("拼音输入（候选字过滤）", sandboxView::getImage);
        exercise.setDisableUserInputData(true);

        exercise.addStep("本次练习输入 <b>"
                         + case_word.getValue()
                         + "("
                         + case_word.getNotation()
                         + ")</b>，并通过笔画过滤筛选其候选字；");
        exercise.addStep("<b>提示</b>：在拼音输入过程中，手指可随意滑过其他按键，只需要确保手指释放前输入了完整的拼音即可；");
        exercise.addStep("input_level_0",
                         "请将手指放在下方键盘的按键<img src=\""
                         + sandboxView.withKey(key_level_0)
                         + "\"/>上，并让手指贴着屏幕从该按键上滑出；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_Inputting: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (key.getLabel().equals(key_level_0.getLabel())) {
                                         exercise.gotoNextStep();
                                     } else {
                                         warning("请按照指导步骤从按键 <big><b>%s</b></big> 开始滑出",
                                                 key_level_0.getLabel());
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
        exercise.addStep("input_level_1",
                         "手指不要离开屏幕，继续将手指滑到按键<img src=\""
                         + sandboxView.withKey(key_level_1)
                         + "\"/>上，并就地释放手指；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_Inputting: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (!key.getLabel().equals(key_level_1.getLabel())) {
                                         warning("请按照指导步骤滑到按键 <big><b>%s</b></big> 上，再从其上滑出",
                                                 key_level_1.getLabel());
                                     }
                                     break;
                                 }
                                 case InputChars_InputtingEnd: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (key.getLabel().equals(key_level_1.getLabel())) {
                                         exercise.gotoNextStep();
                                     } else {
                                         warning("请按照指导步骤滑到按键 <big><b>%s</b></big> 上",
                                                 key_level_1.getLabel());
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
        exercise.addStep("<b>提示</b>：笔画过滤是按字所包含的笔画数进行筛选的"
                         + "，点击笔画按键为增加笔画数，在其上做快速滑出则是减少其数目；");
        exercise.addStep("choose_correct_word",
                         "在候选字列表区域上方依次点击"
                         + "<b>5</b>次<img src=\""
                         + sandboxView.withKey(key_ctrl_filter_heng)
                         + "\"/>"
                         + "、<b>4</b>次<img src=\""
                         + sandboxView.withKey(key_ctrl_filter_shu)
                         + "\"/>（其包含竖钩等变形笔画）"
                         + "、<b>1</b>次<img src=\""
                         + sandboxView.withKey(key_ctrl_filter_pie)
                         + "\"/>"
                         + "、<b>1</b>次<img src=\""
                         + sandboxView.withKey(key_ctrl_filter_na)
                         + "\"/>（其包含点等变形笔画）"
                         + "、<b>2</b>次<img src=\""
                         + sandboxView.withKey(key_ctrl_filter_zhe)
                         + "\"/>（其包含任意含折的笔画）"
                         + "，再点击正确的候选字<img src=\""
                         + sandboxView.withKey(key_case_word)
                         + "\"/>；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputCandidate_Chosen: {
                                     CharInput input = ((InputCandidateChoosingMsgData) data).input;
                                     InputWord word = input.getWord();

                                     if (word != null && case_word.getValue().equals(word.getValue())) {
                                         exercise.gotoNextStep();
                                     } else {
                                         exercise.gotoStep("select_auto_word");
                                         warning("当前选择的候选字与练习内容不符，请按照指导步骤选择 <big><b>%s</b></big>",
                                                 case_word.getValue());
                                     }
                                     break;
                                 }
                             }
                         });
        exercise.addStep("点击输入提交按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit)
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

        return exercise;
    }

    private Exercise exercise_Advance_Pinyin_Inputting(DynamicLayoutSandboxView sandboxView) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        InputWord case_word = new InputWord("zi", "字", "zì");
        Key<?> key_case_word = keyTable.inputWordKey(case_word, 0);

        Key<?> key_level_0 = keyTable.level0CharKey("z");
        Key<?> key_level_1 = keyTable.level1CharKey("i");
        Key<?> key_ctrl_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);
        Key<?> key_ctrl_commit_revoke = keyTable.ctrlKey(CtrlKey.Type.RevokeInput);
        Key<?> key_ctrl_commit_opt_with_pinyin
                = keyTable.commitOptionKey(CtrlKey.CommitInputListOption.Option.with_pinyin);

        String expectedAutoWord = "自";
        String notSelectExpectedAutoWordMsgText = "请按照指导步骤选中键盘上方的候选字 <big><b>%s</b></big>";

        Exercise exercise = Exercise.normal("拼音输入（高级）", sandboxView::getImage);
        exercise.setDisableUserInputData(true);

        exercise.addStep("本次练习输入 <b>"
                         + case_word.getValue()
                         + "("
                         + case_word.getNotation()
                         + ")</b>，并带拼音提交和撤销输入提交；");
        exercise.addStep("<b>提示</b>：在拼音输入过程中，手指可随意滑过其他按键，只需要确保手指释放前输入了完整的拼音即可；");
        exercise.addStep("input_level_0",
                         "请将手指放在下方键盘的按键<img src=\""
                         + sandboxView.withKey(key_level_0)
                         + "\"/>上，并让手指贴着屏幕从该按键上滑出；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_Inputting: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (key.getLabel().equals(key_level_0.getLabel())) {
                                         exercise.gotoNextStep();
                                     } else {
                                         warning("请按照指导步骤从按键 <big><b>%s</b></big> 开始滑出",
                                                 key_level_0.getLabel());
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
        exercise.addStep("input_level_1",
                         "手指不要离开屏幕，继续将手指滑到按键<img src=\""
                         + sandboxView.withKey(key_level_1)
                         + "\"/>上，并就地释放手指；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_Inputting: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (!key.getLabel().equals(key_level_1.getLabel())) {
                                         warning("请按照指导步骤滑到按键 <big><b>%s</b></big> 上，再从其上滑出",
                                                 key_level_1.getLabel());
                                     }
                                     break;
                                 }
                                 case InputChars_InputtingEnd: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (key.getLabel().equals(key_level_1.getLabel())) {
                                         exercise.gotoNextStep();
                                     } else {
                                         warning("请按照指导步骤滑到按键 <big><b>%s</b></big> 上",
                                                 key_level_1.getLabel());
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
                         "在候选字列表区域点击正确的候选字<img src=\"" + sandboxView.withKey(key_case_word) + "\"/>；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputCandidate_Chosen: {
                                     CharInput input = ((InputCandidateChoosingMsgData) data).input;
                                     InputWord word = input.getWord();

                                     if (word != null && case_word.getValue().equals(word.getValue())) {
                                         exercise.gotoNextStep();
                                     } else {
                                         exercise.gotoStep("select_auto_word");
                                         warning("当前选择的候选字与练习内容不符，请按照指导步骤选择 <big><b>%s</b></big>",
                                                 case_word.getValue());
                                     }
                                     break;
                                 }
                             }
                         });
        exercise.addStep("长按输入提交按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit)
                         + "\"/>，进入提交选项界面；", (msg, data) -> {
            switch (msg) {
                case InputChars_InputtingEnd: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key != null && key.equals(key_ctrl_commit)) {
                        exercise.gotoNextStep();
                    } else {
                        warning("请按照指导步骤长按 <big><b>输入提交</b></big> 按键");
                    }
                    break;
                }
            }
        });
        exercise.addStep("点击按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit_opt_with_pinyin)
                         + "\"/>以改变输入是否携带拼音。"
                         + "<b>注</b>：可多次点击做形式切换；", (msg, data) -> {
            switch (msg) {
                case InputChars_InputtingEnd: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key != null && key.equals(key_ctrl_commit_opt_with_pinyin)) {
                        exercise.gotoNextStep();
                    } else {
                        warning("请按照指导步骤点击 <big><b>%s</b></big> 按键",
                                key_ctrl_commit_opt_with_pinyin.getLabel());
                    }
                    break;
                }
            }
        });
        exercise.addStep("点击输入提交按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit)
                         + "\"/>，将当前形式的输入提交至目标输入框。"
                         + "<b>注</b>：长按该按键可退出输入选项界面；", (msg, data) -> {
            switch (msg) {
                case InputList_Committing: {
                    exercise.gotoNextStep();
                    break;
                }
            }
        });
        exercise.addStep("点击按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit_revoke)
                         + "\"/>，将已提交的输入撤回。"
                         + "<b>注</b>：若在输入提交后进行了其他修改操作则不能撤回；", (msg, data) -> {
            switch (msg) {
                case InputList_Committed_Revoking: {
                    exercise.gotoNextStep();
                    confirm(exercise);
                    break;
                }
            }
        });
        exercise.addStep("本次练习已结束，您可以开始后续练习或者继续当前练习。", (msg, data) -> {});

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
