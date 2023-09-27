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
import org.crazydan.studio.app.ime.kuaizi.internal.key.MathOpKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.LocatorKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.MathKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputEditAction;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCandidateChoosingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListCommittingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputTargetCursorLocatingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardSwitchingMsgData;
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

    private List<Exercise> createExercises(DynamicLayoutSandboxView sandboxView) {
        List<Exercise> exercises = new ArrayList<>();

        Exercise exercise = Exercise.free("自由练习");
        exercises.add(exercise);

        exercise = exercise_Basic_Introduce(sandboxView);
        exercises.add(exercise);

        exercise = exercise_Basic_Pinyin_Inputting(sandboxView);
        exercises.add(exercise);

        exercise = exercise_Filter_Pinyin_Inputting(sandboxView);
        exercises.add(exercise);

        exercise = exercise_Advance_Pinyin_Inputting(sandboxView);
        exercises.add(exercise);

        exercise = exercise_Input_Editting(sandboxView);
        exercises.add(exercise);

        exercise = exercise_Char_Inputting(sandboxView);
        exercises.add(exercise);

        exercise = exercise_Math_Inputting(sandboxView);
        exercises.add(exercise);

        return exercises;
    }

    private Exercise exercise_Basic_Introduce(DynamicLayoutSandboxView sandboxView) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        Key<?> key_ctrl_hand_mode = keyTable.ctrlKey(CtrlKey.Type.SwitchHandMode);
        Key<?> key_ctrl_switch_math = keyTable.ctrlKey(CtrlKey.Type.SwitchToMathKeyboard);
        Key<?> key_ctrl_switch_latin = keyTable.ctrlKey(CtrlKey.Type.SwitchToLatinKeyboard);
        Key<?> key_ctrl_switch_emoji = keyTable.ctrlKey(CtrlKey.Type.SwitchToEmojiKeyboard);
        Key<?> key_ctrl_switch_symbol = keyTable.ctrlKey(CtrlKey.Type.SwitchToSymbolKeyboard);
        Key<?> key_ctrl_input_revoke = keyTable.ctrlKey(CtrlKey.Type.RevokeInput);
        Key<?> key_ctrl_cursor_locate = keyTable.ctrlKey(CtrlKey.Type.LocateInputCursor);
        Key<?> key_ctrl_backspace = keyTable.ctrlKey(CtrlKey.Type.Backspace);
        Key<?> key_ctrl_enter = keyTable.ctrlKey(CtrlKey.Type.Enter);
        Key<?> key_ctrl_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);
        Key<?> key_ctrl_space = keyTable.ctrlKey(CtrlKey.Type.Space);

        Exercise exercise = Exercise.normal("拼音键盘布局", sandboxView::getImage);
        exercise.setDisableUserInputData(true);

        exercise.addStep("<img src=\""
                         + sandboxView.withKey(key_ctrl_hand_mode)
                         + "\"/>为左右手输入模式切换按键，用于临时切换左右手模式；");
        exercise.addStep("<img src=\""
                         + sandboxView.withKey(key_ctrl_switch_math)
                         + "\"/>为计算器键盘切换按键，用于切换到计算器键盘以进行数学计算；");
        exercise.addStep("<img src=\""
                         + sandboxView.withKey(key_ctrl_switch_latin)
                         + "\"/>为拉丁文键盘切换按键，用于切换到拉丁文键盘以输入英文字母和数字；");
        exercise.addStep("<img src=\""
                         + sandboxView.withKey(key_ctrl_switch_emoji)
                         + "\"/>为表情输入键盘切换按键，用于切换到表情键盘以输入各类表情字符；");
        exercise.addStep("<img src=\""
                         + sandboxView.withKey(key_ctrl_switch_symbol)
                         + "\"/>为标点符号输入键盘切换按键，用于切换到标点符号键盘以输入各种标点符号；");
        exercise.addStep("<img src=\""
                         + sandboxView.withKey(key_ctrl_input_revoke)
                         + "\"/>为已提交输入的撤回按键，用于将已提交输入撤回以重新修改输入；");
        exercise.addStep("<img src=\""
                         + sandboxView.withKey(key_ctrl_cursor_locate)
                         + "\"/>为输入光标定位按键，在该按键上左右上下滑动可移动输入目标中的光标位置，"
                         + "长按该按键将进入文本编辑模式；");
        exercise.addStep("<img src=\""
                         + sandboxView.withKey(key_ctrl_backspace)
                         + "\"/>为向前删除输入的按键，单击可直接删除正在输入或目标输入框中的内容，"
                         + "长按则可做连续删除；");
        exercise.addStep("<img src=\""
                         + sandboxView.withKey(key_ctrl_enter)
                         + "\"/>、<img src=\""
                         + sandboxView.withKey(key_ctrl_commit)
                         + "\"/>为回车和输入提交按键，其将根据输入情况而变换图标和功能。"
                         + "长按将连续输入换行或提供额外的输入提交选项；");
        exercise.addStep("<img src=\""
                         + sandboxView.withKey(key_ctrl_space)
                         + "\"/>为空格输入按键，可向输入内容中添加空格。"
                         + "长按则将输入连续空格；");

        return exercise;
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
                         + "\"/>（其包含点、提等变形笔画）"
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
                         + ")</b>，并带拼音提交和撤回已提交输入；");
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

    private Exercise exercise_Input_Editting(DynamicLayoutSandboxView sandboxView) {
        LocatorKeyTable keyTable = LocatorKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        Key<?> key_ctrl_cursor_locate = keyTable.ctrlKey(CtrlKey.Type.LocateInputCursor);
        Key<?> key_ctrl_text_selector = keyTable.ctrlKey(CtrlKey.Type.LocateInputCursor_Selector);
        Key<?> key_ctrl_exit = keyTable.ctrlKey(CtrlKey.Type.Exit);
        Key<?> key_ctrl_edit_copy = keyTable.editCtrlKey(InputEditAction.copy);
        Key<?> key_ctrl_edit_paste = keyTable.editCtrlKey(InputEditAction.paste);

        Exercise exercise = Exercise.normal("输入内容编辑", sandboxView::getImage);
        exercise.setDisableUserInputData(true);
        exercise.setSampleText(getResources().getString(R.string.app_slogan));

        exercise.addStep("<b>提示</b>：光标移动和文本选择的范围与在按键上滑动的距离相关，"
                         + "滑动距离越长，光标移动和文本选择范围将越大；");
        exercise.addStep("请使用手指在输入光标定位按键<img src=\""
                         + sandboxView.withKey(key_ctrl_cursor_locate)
                         + "\"/>上进行左/右/上/下快速滑动，并观察目标输入框中光标位置的变化；", (msg, data) -> {
            switch (msg) {
                case InputTarget_Cursor_Locating: {
                    Key<?> key = ((InputTargetCursorLocatingMsgData) data).key;
                    if (key.equals(key_ctrl_cursor_locate)) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("使用手指长按输入光标定位按键<img src=\""
                         + sandboxView.withKey(key_ctrl_cursor_locate)
                         + "\"/>，在键盘发生变换后释放手指；", (msg, data) -> {
            switch (msg) {
                case InputTarget_Cursor_Locating: {
                    Key<?> key = ((InputTargetCursorLocatingMsgData) data).key;
                    if (key.equals(key_ctrl_cursor_locate)) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("使用手指在文本选择按键<img src=\""
                         + sandboxView.withKey(key_ctrl_text_selector)
                         + "\"/>上进行左/右/上/下快速滑动，并观察文本的选择状态；", (msg, data) -> {
            switch (msg) {
                case InputTarget_Selecting: {
                    Key<?> key = ((InputTargetCursorLocatingMsgData) data).key;
                    if (key.equals(key_ctrl_text_selector)) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("尝试点击<img src=\""
                         + sandboxView.withKey(key_ctrl_edit_copy)
                         + "\"/>、<img src=\""
                         + sandboxView.withKey(key_ctrl_edit_paste)
                         + "\"/>等按键，"
                         + "并观察文本的复制、粘贴、剪切、撤销和重做等操作的结果；", (msg, data) -> {
            switch (msg) {
                case InputTarget_Editing: {
                    exercise.gotoNextStep();
                    break;
                }
            }
        });
        exercise.addStep("点击退出按键<img src=\"" + sandboxView.withKey(key_ctrl_exit) + "\"/>以切换到原键盘；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_InputtingEnd: {
                                     exercise.gotoNextStep();
                                     confirm(exercise);
                                     break;
                                 }
                             }
                         });
        exercise.addStep("本次练习已结束，您可以开始后续练习或者继续当前练习。", (msg, data) -> {});

        return exercise;
    }

    private Exercise exercise_Char_Inputting(DynamicLayoutSandboxView sandboxView) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        Key<?> key_char_b = keyTable.level0CharKey("b");
        Key<?> key_char_e = keyTable.level0CharKey("e");
        Key<?> key_char_h = keyTable.level0CharKey("h");
        Key<?> key_char_a = keyTable.level0CharKey("a");
        Key<?> key_char_p = keyTable.level0CharKey("p");
        Key<?> key_char_y = keyTable.level0CharKey("y");
        Key<?> key_symbol_tanhao = PinyinKeyTable.symbolKey("！");
        Key<?> key_ctrl_switch_latin = keyTable.ctrlKey(CtrlKey.Type.SwitchToLatinKeyboard);
        Key<?> key_ctrl_space = keyTable.ctrlKey(CtrlKey.Type.Space);
        Key<?> key_ctrl_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);

        Exercise exercise = Exercise.normal("字母大小写输入", sandboxView::getImage);
        exercise.setDisableUserInputData(true);

        exercise.addStep("本次练习输入 <b>Be Happy!</b>；");
        exercise.addStep("<b>提示</b>：单击字母或标点符号按键将输入其字符本身，"
                         + "双击字母按键将做其大小写转换，而双击标点符号按键则将做中英文符号转换。"
                         + "您也可以点击按键<img src=\""
                         + sandboxView.withKey(key_ctrl_switch_latin)
                         + "\"/>切换到拉丁文键盘，通过同样的方式输入练习内容。"
                         + "不同的是，拉丁文键盘的输入是直接提交至输入目标中的；");
        exercise.addStep("请双击按键<img src=\""
                         + sandboxView.withKey(key_char_b)
                         + "\"/>以输入大写字母 <b>"
                         + key_char_b.getText().toUpperCase()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_char_b.getText().toUpperCase())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请单击按键<img src=\""
                         + sandboxView.withKey(key_char_e)
                         + "\"/>以输入小写字母 <b>"
                         + key_char_e.getText()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_char_e.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请单击按键<img src=\"" + sandboxView.withKey(key_ctrl_space) + "\"/>以输入空格；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_Inputting: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (key.getText().equals(key_ctrl_space.getText())) {
                                         exercise.gotoNextStep();
                                     }
                                     break;
                                 }
                             }
                         });
        exercise.addStep("请双击按键<img src=\""
                         + sandboxView.withKey(key_char_h)
                         + "\"/>以输入大写字母 <b>"
                         + key_char_h.getText().toUpperCase()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_char_h.getText().toUpperCase())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请单击按键<img src=\""
                         + sandboxView.withKey(key_char_a)
                         + "\"/>以输入小写字母 <b>"
                         + key_char_a.getText()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_char_a.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请单击按键<img src=\""
                         + sandboxView.withKey(key_char_p)
                         + "\"/>以输入小写字母 <b>"
                         + key_char_p.getText()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_char_p.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请单击按键<img src=\""
                         + sandboxView.withKey(key_char_p)
                         + "\"/>以输入小写字母 <b>"
                         + key_char_p.getText()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_char_p.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请单击按键<img src=\""
                         + sandboxView.withKey(key_char_y)
                         + "\"/>以输入小写字母 <b>"
                         + key_char_y.getText()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_char_y.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请双击按键<img src=\""
                         + sandboxView.withKey(key_symbol_tanhao)
                         + "\"/>以输入英文标点 <b>!</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals("!")) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("点击输入提交按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit)
                         + "\"/>以将输入内容提交至目标输入框中；", (msg, data) -> {
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

    private Exercise exercise_Math_Inputting(DynamicLayoutSandboxView sandboxView) {
        MathKeyTable keyTable = MathKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        Key<?> key_number_3 = keyTable.numberKey("3");
        Key<?> key_number_2 = keyTable.numberKey("2");
        Key<?> key_number_1 = keyTable.numberKey("1");
        Key<?> key_op_multiply = keyTable.mathOpKey(MathOpKey.Type.multiply);
        Key<?> key_op_plus = keyTable.mathOpKey(MathOpKey.Type.plus);
        Key<?> key_op_equal = keyTable.mathOpKey(MathOpKey.Type.equal);
        Key<?> key_op_brackets = keyTable.mathOpKey(MathOpKey.Type.brackets);
        Key<?> key_ctrl_switch_math = keyTable.ctrlKey(CtrlKey.Type.SwitchToMathKeyboard);
        Key<?> key_ctrl_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);
        Key<?> key_ctrl_enter = keyTable.ctrlKey(CtrlKey.Type.Enter);

        Exercise exercise = Exercise.normal("数学计算输入", sandboxView::getImage);
        exercise.setDisableUserInputData(true);

        exercise.addStep("本次练习输入 <b>3 × (2 + 1) =</b>；");
        exercise.addStep("请点击按键<img src=\""
                         + sandboxView.withKey(key_ctrl_switch_math)
                         + "\"/>以切换到计算器键盘；", (msg, data) -> {
            switch (msg) {
                case Keyboard_Switching: {
                    Keyboard.Type type = ((KeyboardSwitchingMsgData) data).target;
                    if (type == Keyboard.Type.Math) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请单击按键<img src=\""
                         + sandboxView.withKey(key_number_3)
                         + "\"/>以输入数字 <b>"
                         + key_number_3.getText()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_number_3.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请单击按键<img src=\""
                         + sandboxView.withKey(key_op_multiply)
                         + "\"/>以输入运算符 <b>"
                         + key_op_multiply.getText()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_op_multiply.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请单击按键<img src=\"" + sandboxView.withKey(key_op_brackets) + "\"/>以输入括号；",
                         (msg, data) -> {
                             switch (msg) {
                                 case InputChars_Inputting: {
                                     Key<?> key = ((InputCharsInputtingMsgData) data).current;
                                     if (key.getText().equals(key_op_brackets.getText())) {
                                         exercise.gotoNextStep();
                                     }
                                     break;
                                 }
                             }
                         });
        exercise.addStep("请单击按键<img src=\""
                         + sandboxView.withKey(key_number_2)
                         + "\"/>以输入数字 <b>"
                         + key_number_2.getText()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_number_2.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请单击按键<img src=\""
                         + sandboxView.withKey(key_op_plus)
                         + "\"/>以输入运算符 <b>"
                         + key_op_plus.getText()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_op_plus.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请单击按键<img src=\""
                         + sandboxView.withKey(key_number_1)
                         + "\"/>以输入数字 <b>"
                         + key_number_1.getText()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_number_1.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请单击按键<img src=\""
                         + sandboxView.withKey(key_op_equal)
                         + "\"/>以输入运算符 <b>"
                         + key_op_equal.getText()
                         + "</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_op_equal.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请点击输入提交按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit)
                         + "\"/>以将输入内容提交至目标输入框中，"
                         + "并观察输入的计算式中是否包含最终的运算结果；", (msg, data) -> {
            switch (msg) {
                case InputList_Committing: {
                    exercise.gotoNextStep();
                    break;
                }
            }
        });
        exercise.addStep("请点击按键<img src=\""
                         + sandboxView.withKey(key_ctrl_switch_math)
                         + "\"/>以切换到计算器键盘；", (msg, data) -> {
            switch (msg) {
                case Keyboard_Switching: {
                    Keyboard.Type type = ((KeyboardSwitchingMsgData) data).target;
                    if (type == Keyboard.Type.Math) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请点击按键<img src=\""
                         + sandboxView.withKey(key_ctrl_enter)
                         + "\"/>以向目标输入框中输入换行符；", (msg, data) -> {
            switch (msg) {
                case InputList_Committing: {
                    CharSequence text = ((InputListCommittingMsgData) data).text;
                    if (text.equals(key_ctrl_enter.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请按照以上步骤输入 <b>= 3 × (2 + 1)</b>；", (msg, data) -> {
            switch (msg) {
                case InputChars_Inputting: {
                    Key<?> key = ((InputCharsInputtingMsgData) data).current;
                    if (key.getText().equals(key_number_1.getText())) {
                        exercise.gotoNextStep();
                    }
                    break;
                }
            }
        });
        exercise.addStep("请点击输入提交按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit)
                         + "\"/>以将输入内容提交至目标输入框中，"
                         + "并观察输入的计算式中是否包含最终的运算结果；", (msg, data) -> {
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
