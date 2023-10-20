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
import android.view.Menu;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.MathOpKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.EditorEditKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.MathKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.EditorEditAction;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCandidateChoosingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardSwitchDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.FollowSystemThemeActivity;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.DynamicLayoutSandboxView;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.ExerciseView;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.RecyclerPageIndicatorView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-11
 */
public class ExerciseMain extends FollowSystemThemeActivity {
    private DrawerLayout drawerLayout;
    private NavigationView exerciseNavView;

    private ImeInputView imeView;
    private ExerciseListView exerciseListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_exercise_main_activity);

        this.imeView = findViewById(R.id.ime_view);
        this.imeView.startInput(Keyboard.Type.Pinyin);

        Keyboard.ThemeType theme = this.imeView.getKeyboardConfig().getTheme();
        int imeThemeResId = Keyboard.Config.getThemeResId(getApplicationContext(), theme);

        DynamicLayoutSandboxView sandboxView = findViewById(R.id.step_image_sandbox_view);
        List<Exercise> exercises = sandboxView.withMutation(imeThemeResId, () -> createExercises(sandboxView));

        initDrawer(exercises);
        initExerciseList(exercises);
    }

    @Override
    protected void onStart() {
        // 确保拼音字典库保持就绪状态
        PinyinDictDB.getInstance().open(getApplicationContext());

        super.onStart();
    }

    @Override
    public void onBackPressed() {
        // 关闭侧边栏或返回到前一个窗口
        if (!closeDrawer()) {
            super.onBackPressed();
        }
    }

    /** 若输入法配置为左手模式，则在左侧打开侧边栏，否则，在右侧打开 */
    private int getDrawerGravity() {
        if (this.imeView.getKeyboardConfig().getHandMode() == Keyboard.HandMode.Left) {
            return GravityCompat.START;
        }
        return GravityCompat.END;
    }

    private void toggleDrawer() {
        if (!closeDrawer()) {
            this.drawerLayout.openDrawer(getDrawerGravity());
        }
    }

    private boolean closeDrawer() {
        int drawerGravity = getDrawerGravity();
        // Note：只能根据 NavigationView 的 layout_gravity 设置决定侧边栏的显示位置
        if (this.drawerLayout.isDrawerOpen(drawerGravity)) {
            this.drawerLayout.closeDrawer(drawerGravity);
            return true;
        }
        return false;
    }

    private void initDrawer(List<Exercise> exercises) {
        this.drawerLayout = findViewById(R.id.drawer_layout);
        this.exerciseNavView = findViewById(R.id.nav_view);

        // 设置侧边栏打开位置
        int drawerGravity = getDrawerGravity();
        DrawerLayout.LayoutParams layoutParams = (DrawerLayout.LayoutParams) this.exerciseNavView.getLayoutParams();
        layoutParams.gravity = drawerGravity;
        this.exerciseNavView.setLayoutParams(layoutParams);

        Toolbar toolbar = getToolbar();
        // 确保侧边栏的唤出按钮的位置与输入法的左右手模式相同
        if (drawerGravity == GravityCompat.END) {
            toolbar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        } else {
            toolbar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, this.drawerLayout, toolbar, 0, 0);
        this.drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // https://stackoverflow.com/questions/18547277/how-to-set-navigation-drawer-to-be-opened-from-right-to-left#answer-50799607
        // 通过编码方式在指定位置弹出侧边栏
        // - 该设定必须放在 ActionBarDrawerToggle 初始化之后，
        //   以覆盖 ActionBarDrawerToggle 对其的默认设置
        toolbar.setNavigationOnClickListener((v) -> toggleDrawer());

        Menu menu = this.exerciseNavView.getMenu();
        for (int i = 0; i < exercises.size(); i++) {
            Exercise exercise = exercises.get(i);
            String title = ExerciseView.createTitle(exercise, i);

            menu.add(Menu.NONE, i + 10, i, title).setCheckable(true);
        }
        this.exerciseNavView.setNavigationItemSelectedListener((item) -> {
            activeExercise(item.getOrder());
            closeDrawer();

            return true;
        });
    }

    private void activeDrawerNavItem(int position) {
        this.exerciseNavView.setCheckedItem(position + 10);
    }

    private void activeExercise(int position) {
        activeDrawerNavItem(position);
        this.exerciseListView.active(position);
    }

    private void initExerciseList(List<Exercise> exercises) {
        this.exerciseListView = findViewById(R.id.exercise_list_view);
        this.exerciseListView.adapter.bind(exercises);

        this.exerciseListView.setExerciseActiveListener((exerciseView) -> {
            int position = exercises.indexOf(exerciseView.getData());
            activeDrawerNavItem(position);

            exerciseView.withIme(this.imeView);
            this.imeView.startInput(Keyboard.Type.Pinyin);
        });

        RecyclerPageIndicatorView indicatorView = findViewById(R.id.exercise_list_indicator_view);
        indicatorView.attachTo(this.exerciseListView);

        // Note：延迟激活指定的练习，以确保始终能被选中
        this.exerciseListView.post(() -> activeExercise(1));
    }

    private List<Exercise> createExercises(DynamicLayoutSandboxView sandboxView) {
        List<Exercise> exerciseList = new ArrayList<>();

        exerciseList.add(Exercise.free("自由练习"));
        exerciseList.add(exercise_Basic_Introduce(sandboxView));

        Exercise[] exercises = new Exercise[] {
                exercise_Pinyin_Slipping_Inputting(sandboxView),
                exercise_Pinyin_Candidate_Filtering(sandboxView),
                exercise_Char_Replacement_Inputting(sandboxView),
                exercise_Math_Inputting(sandboxView),
                exercise_Editor_Editing(sandboxView),
                exercise_Pinyin_Committed_Processing(sandboxView),
                };
        for (int i = 0; i < exercises.length; i++) {
            Exercise exercise = exercises[i];
            boolean isLastOne = i == exercises.length - 1;
            if (!isLastOne) {
                exercise.addStep("本次练习已结束，您可以开始后续练习或者重做当前练习。", (msg, data) -> {});
            } else {
                exercise.addStep("本次练习已结束，您可以重做当前练习。", (msg, data) -> {});
            }

            ExerciseStep.Final finalStep = new ExerciseStep.Final(exercise::restart,
                                                                  !isLastOne
                                                                  ? () -> this.exerciseListView.activeNext()
                                                                  : null);

            exercise.addStep(finalStep);
            exercise.setDisableUserInputData(true);

            exerciseList.add(exercise);
        }

        return exerciseList;
    }

    private Exercise exercise_Basic_Introduce(DynamicLayoutSandboxView sandboxView) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        Key<?> key_ctrl_hand_mode = keyTable.ctrlKey(CtrlKey.Type.Switch_HandMode);
        Key<?> key_ctrl_switch_math = keyTable.keyboardSwitchKey(Keyboard.Type.Math);
        Key<?> key_ctrl_switch_latin = keyTable.keyboardSwitchKey(Keyboard.Type.Latin);
        Key<?> key_ctrl_switch_pinyin = keyTable.keyboardSwitchKey(Keyboard.Type.Pinyin);
        Key<?> key_ctrl_switch_emoji = keyTable.ctrlKey(CtrlKey.Type.Toggle_Emoji_Keyboard);
        Key<?> key_ctrl_switch_symbol = keyTable.ctrlKey(CtrlKey.Type.Toggle_Symbol_Keyboard);
        Key<?> key_ctrl_input_revoke = keyTable.ctrlKey(CtrlKey.Type.RevokeInput);
        Key<?> key_ctrl_input_drop = keyTable.ctrlKey(CtrlKey.Type.DropInput);
        Key<?> key_ctrl_input_confirm = keyTable.ctrlKey(CtrlKey.Type.ConfirmInput);
        Key<?> key_ctrl_cursor_locator = keyTable.ctrlKey(CtrlKey.Type.Editor_Cursor_Locator);
        Key<?> key_ctrl_range_selector = keyTable.ctrlKey(CtrlKey.Type.Editor_Range_Selector);
        Key<?> key_ctrl_backspace = keyTable.ctrlKey(CtrlKey.Type.Backspace);
        Key<?> key_ctrl_enter = keyTable.ctrlKey(CtrlKey.Type.Enter);
        Key<?> key_ctrl_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);
        Key<?> key_ctrl_space = keyTable.ctrlKey(CtrlKey.Type.Space);
        Key<?> key_ctrl_exit = keyTable.ctrlKey(CtrlKey.Type.Exit);

        Exercise exercise = Exercise.introduce("功能按键简介", sandboxView::getImage);

        String[] steps = new String[] {
                // 核心按键
                "<img src=\""
                + sandboxView.withKey(key_ctrl_backspace)
                + "\"/>为前向删除按键，单击可向前删除输入内容，长按则可做连续删除；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_enter)
                + "\"/>为回车/换行按键，单击可向目标编辑器输入换行符，长按则可连续输入换行符；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_space)
                + "\"/>为空格输入按键，单击可在输入内容中添加空格，长按则将连续添加空格；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_cursor_locator)
                + "\"/>为光标定位按键，在其上滑动手指可移动目标编辑器中的光标，"
                + "长按则将进入<b>内容编辑</b>模式；",
                // 其他按键
                "<img src=\""
                + sandboxView.withKey(key_ctrl_hand_mode)
                + "\"/>为左右手输入模式切换按键，用于临时切换<b>左右手使用</b>模式；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_switch_math)
                + "\"/>为算数键盘切换按键，用于切换到<b>算数输入</b>键盘；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_switch_latin)
                + "\"/>为拉丁文键盘切换按键，用于从拼音键盘切换到拉丁文（英文、数字）输入键盘；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_switch_pinyin)
                + "\"/>为拼音键盘切换按键，其在切换到拉丁文键盘后显示，用于从拉丁文键盘切换回拼音键盘；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_switch_emoji)
                + "\"/>为表情符号键盘切换按键，用于切换到表情符号输入键盘；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_switch_symbol)
                + "\"/>为标点符号键盘切换按键，用于切换到标点符号输入键盘；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_input_revoke)
                + "\"/>为已提交输入的<b>撤回</b>按键，用于撤回已提交至目标编辑器的输入，以重新修正。"
                + "在没有可撤回输入时，该按键将被禁用；",
                // 隐藏按键
                "<img src=\""
                + sandboxView.withKey(key_ctrl_commit)
                + "\"/>为输入的提交按键，其在键盘上方的<b>输入列表</b>不为空时显示。"
                + "单击可向目标编辑器提交当前输入；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_input_confirm)
                + "\"/>为拼音候选字的确认按键，其在选中拼音候选字时显示。"
                + "若当前选中的候选字已经是正确的，则可点击该按键以跳过对其的选择；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_exit)
                + "\"/>为当前键盘退出按键，点击后将切换回原键盘。"
                + "比如，从表情符号键盘退回到拼音键盘；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_input_drop)
                + "\"/>为已选中输入的删除按键，在选中表情符号、标点符号、拼音候选字等时显示。"
                + "单击可删除当前选中的输入；",
                "<img src=\""
                + sandboxView.withKey(key_ctrl_range_selector)
                + "\"/>为内容选择按键，在长按<img src=\""
                + sandboxView.withKey(key_ctrl_cursor_locator)
                + "\"/>后显示。在其上滑动手指可移动目标编辑器中的光标，并选中光标移动范围内的内容，"
                + "可进一步执行复制、粘贴、剪切等操作；",
                };

        for (String step : steps) {
            exercise.addStep(step);
        }

        return exercise;
    }

    private Exercise exercise_Pinyin_Slipping_Inputting(DynamicLayoutSandboxView sandboxView) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        String expected_auto_word = "块";
        InputWord case_word = new InputWord("kuai", "筷", "kuài");
        Key<?> key_case_word = keyTable.inputWordKey(case_word, 0);

        Key<?> key_level_0 = keyTable.level0CharKey("k");
        Key<?> key_level_1 = keyTable.level1CharKey("u");
        Key<?> key_level_2 = keyTable.level2CharKey(key_level_0.getText(), "uai");
        Key<?> key_ctrl_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);

        Exercise exercise = Exercise.normal("拼音滑屏输入", sandboxView::getImage);

        exercise.addStep("本次练习输入 <span style=\"color:#ed4c67;\">"
                         + case_word.getValue()
                         + "("
                         + case_word.getNotation()
                         + ")</span>；");

        add_Pinyin_Inputting_Steps(sandboxView,
                                   exercise,
                                   key_level_0,
                                   key_level_1,
                                   key_level_2,
                                   key_case_word,
                                   expected_auto_word,
                                   null);

        add_Common_Input_Committing_Step(sandboxView, exercise, key_ctrl_commit);

        return exercise;
    }

    private Exercise exercise_Pinyin_Candidate_Filtering(DynamicLayoutSandboxView sandboxView) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        String expected_auto_word = "术";
        InputWord case_word = new InputWord("shu", "输", "shū");
        Key<?> key_case_word = keyTable.inputWordKey(case_word, 0);

        Key<?> key_level_0 = keyTable.level0CharKey("sh");
        Key<?> key_level_1 = keyTable.level1CharKey("u");
        Key<?> key_ctrl_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);
        Key<?> key_ctrl_filter_heng = keyTable.strokeFilterKey("一", null);
        Key<?> key_ctrl_filter_shu = keyTable.strokeFilterKey("丨", null);
        Key<?> key_ctrl_filter_pie = keyTable.strokeFilterKey("丿", null);
        Key<?> key_ctrl_filter_na = keyTable.strokeFilterKey("㇏", null);
        // 𠃋
        Key<?> key_ctrl_filter_zhe = keyTable.strokeFilterKey("\uD840\uDCCB", null);

        Exercise exercise = Exercise.normal("拼音候选字过滤", sandboxView::getImage);

        exercise.addStep("本次练习输入 <span style=\"color:#ed4c67;\">"
                         + case_word.getValue()
                         + "("
                         + case_word.getNotation()
                         + ")</span>，并通过笔画过滤筛选其候选字；");
        add_Pinyin_Inputting_Steps(sandboxView,
                                   exercise,
                                   key_level_0,
                                   key_level_1,
                                   null,
                                   key_case_word,
                                   expected_auto_word,
                                   "请在候选字列表区域上方依次点击"
                                   + "<span style=\"color:#ed4c67;\">5</span>次<img src=\""
                                   + sandboxView.withKey(key_ctrl_filter_heng)
                                   + "\"/>（横）"
                                   + "、<span style=\"color:#ed4c67;\">4</span>次<img src=\""
                                   + sandboxView.withKey(key_ctrl_filter_shu)
                                   + "\"/>（竖，其包含竖钩等变形笔画）"
                                   + "、<span style=\"color:#ed4c67;\">1</span>次<img src=\""
                                   + sandboxView.withKey(key_ctrl_filter_pie)
                                   + "\"/>（撇）"
                                   + "、<span style=\"color:#ed4c67;\">1</span>次<img src=\""
                                   + sandboxView.withKey(key_ctrl_filter_na)
                                   + "\"/>（捺，其包含点、提等变形笔画）"
                                   + "、<span style=\"color:#ed4c67;\">2</span>次<img src=\""
                                   + sandboxView.withKey(key_ctrl_filter_zhe)
                                   + "\"/>（折，其包含任意含折的笔画）"
                                   + "，再点击正确的候选字<img src=\""
                                   + sandboxView.withKey(key_case_word)
                                   + "\"/>；");

        exercise.addStep(-1,
                         "<b>提示</b>：笔画过滤是按字所包含的各类笔画的数量进行筛选的。"
                         + "点击笔画按键为增加笔画数，而在其上做快速滑出则是减少其数量；");

        add_Common_Input_Committing_Step(sandboxView, exercise, key_ctrl_commit);

        return exercise;
    }

    private Exercise exercise_Pinyin_Committed_Processing(DynamicLayoutSandboxView sandboxView) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        String expected_auto_word = "自";
        InputWord case_word = new InputWord("zi", "字", "zì");
        Key<?> key_case_word = keyTable.inputWordKey(case_word, 0);

        Key<?> key_level_0 = keyTable.level0CharKey("z");
        Key<?> key_level_1 = keyTable.level1CharKey("i");
        Key<?> key_ctrl_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);
        Key<?> key_ctrl_commit_revoke = keyTable.ctrlKey(CtrlKey.Type.RevokeInput);
        Key<?> key_ctrl_commit_opt_with_pinyin
                = keyTable.commitOptionKey(CtrlKey.InputListCommitOption.Option.with_pinyin);

        Exercise exercise = Exercise.normal("拼音输入提交选项", sandboxView::getImage);

        exercise.addStep("本次练习输入 <span style=\"color:#ed4c67;\">"
                         + case_word.getValue()
                         + "("
                         + case_word.getNotation()
                         + ")</span>，并进行<b>带拼音</b>提交和<b>撤回</b>已提交输入；");

        add_Pinyin_Inputting_Steps(sandboxView,
                                   exercise,
                                   key_level_0,
                                   key_level_1,
                                   null,
                                   key_case_word,
                                   expected_auto_word,
                                   null);

        exercise.addStep("请长按输入提交按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit)
                         + "\"/>以进入<b>输入提交选项</b>模式；", (msg, data) -> {
            if (msg == InputMsg.Keyboard_State_Change_Done) {
                Key<?> key = data.getKey();

                if (key != null && key.equals(key_ctrl_commit)) {
                    exercise.gotoNextStep();
                    return;
                }
            }
            warning("请按当前步骤的指导要求长按 <span style=\"color:#ed4c67;\">输入提交按键</span>");
        });
        exercise.addStep("请点击按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit_opt_with_pinyin)
                         + "\"/>以设置待提交的输入需携带拼音。"
                         + "<b>注</b>：可多次点击做形式切换；", (msg, data) -> {
            if (msg == InputMsg.InputChars_Input_Done) {
                Key<?> key = data.getKey();

                if (key != null && key.equals(key_ctrl_commit_opt_with_pinyin)) {
                    exercise.gotoNextStep();
                }
            }
        });

        exercise.addStep("请点击输入提交按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit)
                         + "\"/>将当前形式的输入提交至目标编辑器。"
                         + "<b>注</b>：长按该按键可退出<b>输入提交选项</b>模式；", (msg, data) -> {
            if (msg == InputMsg.InputList_Commit_Doing) {
                exercise.gotoNextStep();
            }
        });
        exercise.addStep("请点击按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit_revoke)
                         + "\"/>将刚刚提交的输入撤回；", (msg, data) -> {
            if (msg == InputMsg.InputList_Committed_Revoke_Doing) {
                exercise.gotoNextStep();
            } else {
                warning("请按当前步骤的指导要求撤回输入内容");
            }
        });

        return exercise;
    }

    private Exercise exercise_Editor_Editing(DynamicLayoutSandboxView sandboxView) {
        EditorEditKeyTable keyTable = EditorEditKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        Key<?> key_ctrl_cursor_locator = keyTable.ctrlKey(CtrlKey.Type.Editor_Cursor_Locator);
        Key<?> key_ctrl_range_selector = keyTable.ctrlKey(CtrlKey.Type.Editor_Range_Selector);
        Key<?> key_ctrl_exit = keyTable.ctrlKey(CtrlKey.Type.Exit);
        Key<?> key_ctrl_edit_copy = keyTable.editCtrlKey(EditorEditAction.copy);
        Key<?> key_ctrl_edit_paste = keyTable.editCtrlKey(EditorEditAction.paste);

        Exercise exercise = Exercise.normal("内容编辑", sandboxView::getImage);
        exercise.setSampleText(getResources().getString(R.string.app_slogan));

        exercise.addStep("<b>提示</b>：光标移动和内容选择的范围与手指在按键上滑行的距离相关，"
                         + "手指在按键上的滑行距离越长，光标移动和内容选择的范围将越大；");
        exercise.addStep("请使用手指在光标定位按键<img src=\""
                         + sandboxView.withKey(key_ctrl_cursor_locator)
                         + "\"/>上向不同方向快速滑动，并观察目标编辑器中光标位置的变化；", (msg, data) -> {
            if (msg == InputMsg.Editor_Cursor_Move_Doing) {
                Key<?> key = data.getKey();

                if (key.equals(key_ctrl_cursor_locator)) {
                    exercise.gotoNextStep();
                    return;
                }
            }
            warning("请按当前步骤的指导要求移动目标编辑器中的光标");
        });
        exercise.addStep("请长按光标定位按键<img src=\""
                         + sandboxView.withKey(key_ctrl_cursor_locator)
                         + "\"/>以进入<b>内容编辑</b>模式；", (msg, data) -> {
            if (msg == InputMsg.Keyboard_State_Change_Done) {
                Key<?> key = data.getKey();

                if (key != null && key.equals(key_ctrl_cursor_locator)) {
                    exercise.gotoNextStep();
                } else {
                    warning("请按当前步骤的指导要求长按 <span style=\"color:#ed4c67;\">光标定位按键</span>");
                }
            }
        });
        exercise.addStep("请在内容选择按键<img src=\""
                         + sandboxView.withKey(key_ctrl_range_selector)
                         + "\"/>上快速滑动，并观察目标编辑器中内容的选择状态；", (msg, data) -> {
            if (msg == InputMsg.Editor_Range_Select_Doing) {
                Key<?> key = data.getKey();

                if (key.equals(key_ctrl_range_selector)) {
                    exercise.gotoNextStep();
                }
            }
        });
        exercise.addStep("请尝试点击<img src=\""
                         + sandboxView.withKey(key_ctrl_edit_copy)
                         + "\"/>、<img src=\""
                         + sandboxView.withKey(key_ctrl_edit_paste)
                         + "\"/>等按键，"
                         + "并观察复制、粘贴、剪切、撤销和重做等操作的结果；", (msg, data) -> {
            if (msg == InputMsg.Editor_Edit_Doing) {
                exercise.gotoNextStep();
            }
        });

        exercise.addStep("请点击退出按键<img src=\"" + sandboxView.withKey(key_ctrl_exit) + "\"/>以切换回原键盘；",
                         (msg, data) -> {
                             if (msg == InputMsg.Keyboard_State_Change_Done) {
                                 exercise.gotoNextStep();
                             }
                         });

        return exercise;
    }

    private Exercise exercise_Char_Replacement_Inputting(DynamicLayoutSandboxView sandboxView) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        Key<?> key_symbol_tanhao = keyTable.symbolKey("！");
        Key<?> key_ctrl_space = keyTable.ctrlKey(CtrlKey.Type.Space);
        Key<?> key_ctrl_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);

        Exercise exercise = Exercise.normal("字符输入变换", sandboxView::getImage);

        exercise.addStep("本次练习输入 <span style=\"color:#ed4c67;\">Be Happy!</span>；");
        exercise.addStep("<b>提示</b>：在拼音键盘和拉丁文键盘中，"
                         + "均可以通过单击按键输入字符，通过双击按键输入字符的变换形式，"
                         + "若连续多次快速点击，则会按顺序依次做不同形式的变换。"
                         + "形式变换包括字母的大小写切换、ü/v/V 转换和标点的中英文切换；");

        char[] chars = new char[] { 'B', 'e', ' ', 'H', 'a', 'p', 'p', 'y' };
        for (char ch : chars) {
            if (ch == ' ') {
                exercise.addStep("请点击按键<img src=\"" + sandboxView.withKey(key_ctrl_space) + "\"/>以输入空格；",
                                 (msg, data) -> {
                                     if (msg == InputMsg.InputChars_Input_Done) {
                                         Key<?> key = data.getKey();

                                         if (key.getText().equals(key_ctrl_space.getText())) {
                                             exercise.gotoNextStep();
                                             return;
                                         }
                                     }
                                     warning("请按当前步骤的指导要求输入空格");
                                 });
                continue;
            }

            Key<?> key_char = keyTable.level0CharKey(ch + "");

            if (Character.isUpperCase(ch)) {
                exercise.addStep("请双击按键<img src=\""
                                 + sandboxView.withKey(key_char)
                                 + "\"/>以输入大写字母 <span style=\"color:#ed4c67;\">"
                                 + ch
                                 + "</span>；", (msg, data) -> {
                    if (msg == InputMsg.InputChars_Input_Doing) {
                        Key<?> key = data.getKey();

                        if (((InputCharsInputtingMsgData) data).keyInputType
                            != InputCharsInputtingMsgData.KeyInputType.tap //
                            || !key.getText().equalsIgnoreCase(key_char.getText())) {
                            warning("请按当前步骤的指导要求双击按键 <span style=\"color:#ed4c67;\">%s</span>",
                                    key_char.getText());
                        } else if (key.getText().equals(key_char.getText())) {
                            exercise.gotoNextStep();
                        }
                    } else {
                        warning("请按当前步骤的指导要求输入字母 <span style=\"color:#ed4c67;\">%s</span>", ch);
                    }
                });
            } else {
                exercise.addStep("请点击按键<img src=\""
                                 + sandboxView.withKey(key_char)
                                 + "\"/>以输入小写字母 <span style=\"color:#ed4c67;\">"
                                 + ch
                                 + "</span>；", (msg, data) -> {
                    if (msg == InputMsg.InputChars_Input_Doing) {
                        Key<?> key = data.getKey();

                        if (((InputCharsInputtingMsgData) data).keyInputType
                            != InputCharsInputtingMsgData.KeyInputType.tap //
                            || !key.getText().equals(key_char.getText())) {
                            warning("请按当前步骤的指导要求点击按键 <span style=\"color:#ed4c67;\">%s</span>",
                                    key_char.getText());
                        } else {
                            exercise.gotoNextStep();
                        }
                    } else {
                        warning("请按当前步骤的指导要求输入字母 <span style=\"color:#ed4c67;\">%s</span>", ch);
                    }
                });
            }
        }

        exercise.addStep("请双击按键<img src=\""
                         + sandboxView.withKey(key_symbol_tanhao)
                         + "\"/>以输入英文标点 <span style=\"color:#ed4c67;\">!</span>；", (msg, data) -> {
            if (msg == InputMsg.InputChars_Input_Doing) {
                Key<?> key = data.getKey();

                if (((InputCharsInputtingMsgData) data).keyInputType != InputCharsInputtingMsgData.KeyInputType.tap //
                    || (!key.getText().equals("!") && !key.getText().equals(key_symbol_tanhao.getText()))) {
                    warning("请按当前步骤的指导要求双击按键 <span style=\"color:#ed4c67;\">%s</span>",
                            key_symbol_tanhao.getText());
                } else if (key.getText().equals("!")) {
                    exercise.gotoNextStep();
                }
            } else if (msg != InputMsg.InputChars_Input_Done) {
                warning("请按当前步骤的指导要求输入字符 <span style=\"color:#ed4c67;\">!</span>");
            }
        });

        add_Common_Input_Committing_Step(sandboxView, exercise, key_ctrl_commit);

        return exercise;
    }

    private Exercise exercise_Math_Inputting(DynamicLayoutSandboxView sandboxView) {
        MathKeyTable keyTable = MathKeyTable.create(new KeyTable.Config(this.imeView.getKeyboardConfig()));

        Key<?> key_ctrl_switch_math = keyTable.keyboardSwitchKey(Keyboard.Type.Math);
        Key<?> key_ctrl_commit = keyTable.ctrlKey(CtrlKey.Type.Commit_InputList);

        Exercise exercise = Exercise.normal("算数输入", sandboxView::getImage);

        exercise.addStep("本次练习输入 <span style=\"color:#ed4c67;\">3 × (2 + 1) =</span>；");
        exercise.addStep("<b>提示</b>：若计算式以等号开头，则提交内容将仅包含计算结果。"
                         + "若计算式以等号结尾，则提交内容除了计算结果以外还将包括计算式本身。"
                         + "对于无效的计算式，则将保持原样输出；");
        exercise.addStep("请点击按键<img src=\"" + sandboxView.withKey(key_ctrl_switch_math) + "\"/>以切换到算数键盘；",
                         (msg, data) -> {
                             if (msg == InputMsg.Keyboard_Switch_Doing) {
                                 Keyboard.Type type = ((KeyboardSwitchDoingMsgData) data).target;

                                 if (type == Keyboard.Type.Math) {
                                     exercise.gotoNextStep();
                                     return;
                                 }
                             }
                             warning("请按当前步骤的指导要求切换到算数键盘");
                         });
        String[] chars = new String[] {
                "3",
                MathOpKey.Type.multiply.name(),
                MathOpKey.Type.brackets.name(),
                "2",
                MathOpKey.Type.plus.name(),
                "1",
                MathOpKey.Type.equal.name(),
                };
        for (String ch : chars) {
            if (Character.isDigit(ch.charAt(0))) {
                Key<?> key_number = keyTable.numberKey(ch);

                exercise.addStep("请点击按键<img src=\""
                                 + sandboxView.withKey(key_number)
                                 + "\"/>以输入数字 <span style=\"color:#ed4c67;\">"
                                 + key_number.getText()
                                 + "</span>；", (msg, data) -> {
                    if (msg == InputMsg.InputChars_Input_Doing) {
                        Key<?> key = data.getKey();

                        if (key.getText().equals(key_number.getText())) {
                            exercise.gotoNextStep();
                            return;
                        }
                    }
                    warning("请按当前步骤的指导要求输入数字 <span style=\"color:#ed4c67;\">%s</span>",
                            key_number.getText());
                });
            } else {
                Key<?> key_op = keyTable.mathOpKey(MathOpKey.Type.valueOf(ch));

                exercise.addStep("请点击按键<img src=\""
                                 + sandboxView.withKey(key_op)
                                 + "\"/>以输入运算符 <span style=\"color:#ed4c67;\">"
                                 + key_op.getText()
                                 + "</span>；", (msg, data) -> {
                    if (msg == InputMsg.InputChars_Input_Doing) {
                        Key<?> key = data.getKey();

                        if (key.getText().equals(key_op.getText())) {
                            exercise.gotoNextStep();
                            return;
                        }
                    }
                    warning("请按当前步骤的指导要求输入运算符 <span style=\"color:#ed4c67;\">%s</span>",
                            key_op.getText());
                });
            }
        }

        exercise.addStep("请点击输入提交按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit)
                         + "\"/>将当前输入提交至目标编辑器，"
                         + "并观察输入的计算式中是否包含最终的运算结果；", (msg, data) -> {
            if (msg == InputMsg.InputList_Commit_Doing) {
                exercise.gotoNextStep();
            } else {
                warning("请按当前步骤的指导要求提交输入内容");
            }
        });

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

    private void add_Pinyin_Inputting_Steps(
            DynamicLayoutSandboxView sandboxView, Exercise exercise, //
            Key<?> key_level_0, Key<?> key_level_1, Key<?> key_level_2, //
            Key<?> key_case_word, String expected_auto_word, String word_choose_step_content
    ) {
        InputWord case_word = ((InputWordKey) key_case_word).getWord();

        exercise.addStep("<b>提示</b>：在拼音输入过程中，手指可随意滑过其他按键，仅需确保手指释放前输入了完整的拼音即可；");

        exercise.addStep("input_level_0",
                         "请将手指放在下方键盘的按键<img src=\""
                         + sandboxView.withKey(key_level_0)
                         + "\"/>上，并让手指贴着屏幕从该按键上滑出；",
                         (msg, data) -> {
                             if (msg == InputMsg.InputChars_Input_Doing) {
                                 Key<?> key = data.getKey();

                                 if (((InputCharsInputtingMsgData) data).keyInputType
                                     != InputCharsInputtingMsgData.KeyInputType.slip) {
                                     warning("请按当前步骤的指导要求从按键"
                                             + " <span style=\"color:#ed4c67;\">%s</span>"
                                             + " 上滑出，不要做点击或翻页等动作", key_level_0.getLabel());
                                 } else if (key.getLabel().equals(key_level_0.getLabel())) {
                                     exercise.gotoNextStep();
                                 } else {
                                     warning("请按当前步骤的指导要求从按键"
                                             + " <span style=\"color:#ed4c67;\">%s</span>"
                                             + " 上滑出，不要从其他按键上滑出", key_level_0.getLabel());
                                 }
                             } else if (msg != InputMsg.Keyboard_State_Change_Done) {
                                 warning("请按当前步骤的指导要求输入拼音");
                             }
                         });

        if (key_level_2 == null) {
            exercise.addStep("input_level_1",
                             "请不要让手指离开屏幕，继续将手指滑到按键<img src=\""
                             + sandboxView.withKey(key_level_1)
                             + "\"/>上，并就地释放手指；",
                             (msg, data) -> {
                                 switch (msg) {
                                     case InputChars_Input_Doing: {
                                         Key<?> key = data.getKey();

                                         if (!key.getLabel().equals(key_level_1.getLabel())) {
                                             warning("请重新滑回到按键"
                                                     + " <span style=\"color:#ed4c67;\">%s</span>"
                                                     + " 上，再就地释放手指", key_level_1.getLabel());
                                         }
                                         break;
                                     }
                                     case InputChars_Input_Done: {
                                         Key<?> key = data.getKey();

                                         if (key != null && key.getLabel().equals(key_level_1.getLabel())) {
                                             exercise.gotoNextStep();
                                         } else {
                                             warning("当前输入的拼音与练习内容不符，请重新开始本练习");
                                             exercise.restart();
                                         }
                                         break;
                                     }
                                 }
                             });
        } else {
            exercise.addStep("input_level_1",
                             "请不要让手指离开屏幕，继续将手指滑到按键<img src=\""
                             + sandboxView.withKey(key_level_1)
                             + "\"/>上，再从该按键上滑出；",
                             (msg, data) -> {
                                 if (msg == InputMsg.InputChars_Input_Doing) {
                                     Key<?> key = data.getKey();

                                     if (key.getLabel().equals(key_level_1.getLabel())) {
                                         exercise.gotoNextStep();
                                     } else {
                                         warning("请按当前步骤的指导要求从按键"
                                                 + " <span style=\"color:#ed4c67;\">%s</span>"
                                                 + " 上滑出，不要从其他按键上滑出", key_level_1.getLabel());
                                     }
                                 } else {
                                     warning("当前操作不符合输入要求，请重新开始本练习");
                                     exercise.restart();
                                 }
                             });
            exercise.addStep("input_level_2",
                             "请继续将手指滑到按键<img src=\""
                             + sandboxView.withKey(key_level_2)
                             + "\"/>上，并就地释放手指；",
                             (msg, data) -> {
                                 switch (msg) {
                                     case InputChars_Input_Doing: {
                                         Key<?> key = data.getKey();

                                         if (!key.getLabel()
                                                 .startsWith(key_level_0.getLabel() + key_level_1.getLabel())) {
                                             warning("请重新滑回到按键"
                                                     + " <span style=\"color:#ed4c67;\">%s</span>"
                                                     + " 上，再从其上滑出", key_level_1.getLabel());
                                             exercise.gotoStep("input_level_1");
                                         }
                                         break;
                                     }
                                     case InputChars_Input_Done: {
                                         Key<?> key = data.getKey();

                                         if (key != null && key.getLabel().equals(key_level_2.getLabel())) {
                                             exercise.gotoNextStep();
                                         } else {
                                             warning("当前输入的拼音与练习内容不符，请重新开始本练习");
                                             exercise.restart();
                                         }
                                         break;
                                     }
                                 }
                             });
        }

        exercise.addStep("select_auto_word",
                         "请选中键盘上方 <span style=\"color:#ed4c67;\">输入列表</span> 中的拼音候选字"
                         + " <span style=\"color:#ed4c67;\">"
                         + expected_auto_word
                         + "</span>；",
                         (msg, data) -> {
                             if (msg == InputMsg.InputCandidate_Choose_Doing) {
                                 CharInput input = ((InputCandidateChoosingMsgData) data).target;
                                 InputWord word = input.getWord();

                                 if (word != null && expected_auto_word.equals(word.getValue())) {
                                     exercise.gotoNextStep();
                                     return;
                                 }
                             }
                             warning("请按当前步骤的指导要求选中指定的拼音候选字"
                                     + " <span style=\"color:#ed4c67;\">%s</span>", expected_auto_word);
                         });

        exercise.addStep("choose_correct_word",
                         word_choose_step_content != null
                         ? word_choose_step_content
                         : "请在候选字列表区域中点击正确的候选字<img src=\""
                           + sandboxView.withKey(key_case_word)
                           + "\"/>。<b>注</b>：可在该区域中上下翻页；",
                         (msg, data) -> {
                             if (msg == InputMsg.InputCandidate_Choose_Done) {
                                 CharInput input = ((InputCandidateChoosingMsgData) data).target;
                                 InputWord word = input.getWord();

                                 if (word != null && case_word.getValue().equals(word.getValue())) {
                                     exercise.gotoNextStep();
                                 } else {
                                     warning("当前选择的候选字与练习内容不符，请按照指导步骤重新选择"
                                             + " <span style=\"color:#ed4c67;\">%s</span>", expected_auto_word);
                                     exercise.gotoStep("select_auto_word");
                                 }
                             } else if (msg != InputMsg.InputCandidate_Choose_Doing) {
                                 warning("当前操作不符合练习步骤指导要求，请按照指导步骤重新选择"
                                         + " <span style=\"color:#ed4c67;\">%s</span>", expected_auto_word);
                                 exercise.gotoStep("select_auto_word");
                             }
                         });
    }

    private void add_Common_Input_Committing_Step(
            DynamicLayoutSandboxView sandboxView, Exercise exercise, Key<?> key_ctrl_commit
    ) {
        exercise.addStep("请点击输入提交按键<img src=\""
                         + sandboxView.withKey(key_ctrl_commit)
                         + "\"/>将当前输入提交至目标编辑器；", (msg, data) -> {
            if (msg == InputMsg.InputList_Commit_Doing) {
                exercise.gotoNextStep();
            } else {
                warning("请按当前步骤的指导要求提交输入内容");
            }
        });
    }
}
