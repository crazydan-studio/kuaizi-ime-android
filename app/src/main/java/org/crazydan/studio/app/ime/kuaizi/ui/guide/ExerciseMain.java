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

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputEditAction;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListCommittingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListPairSymbolCommittingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputTargetCursorLocatingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputTargetEditingMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.FollowSystemThemeActivity;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.DynamicLayoutSandboxView;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.RecyclerPageIndicatorView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-11
 */
public class ExerciseMain extends FollowSystemThemeActivity implements InputMsgListener {
    private EditText editText;
    private ImeInputView imeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_exercise_main_activity);

        this.imeView = findViewById(R.id.ime_view);
        this.imeView.updateKeyboard(new Keyboard.Config(Keyboard.Type.Pinyin));

        int imeThemeResId = this.imeView.getKeyboardConfig().getThemeResId();
        DynamicLayoutSandboxView sandboxView = findViewById(R.id.step_image_sandbox_view);
        List<Exercise> exercises = sandboxView.withMutation(imeThemeResId, () -> createExercises(sandboxView));

        ExerciseListView listView = findViewById(R.id.exercise_list_view);
        listView.adapter.bind(exercises);

        RecyclerPageIndicatorView indicatorView = findViewById(R.id.exercise_list_indicator_view);
        indicatorView.attachTo(listView);

        listView.active(1);

//        this.editText = findViewById(R.id.text_input);
//        this.editText.setClickable(false);
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

        Key<?> key_k = keyTable.level0CharKey("k");
        Key<?> key_u = keyTable.level1CharKey("u");
        Key<?> key_kuai = keyTable.level2CharKey(key_k.getText(), "uai");
        Key<?> key_sh = keyTable.level0CharKey("sh");

        Exercise exercise = Exercise.normal("拼音输入", sandboxView::getImage);

        exercise.addStep("本次练习输入：<big><b>筷字输入法</b></big>");
        exercise.addStep("输入 <b>筷(kuai)</b>：")
                .subStep("请将手指放在下方键盘的按键<img src=\""
                         + sandboxView.withKey(key_k)
                         + "\"/>上，并让手指贴着屏幕从该按键上滑出", null)
                .subStep("不释放手指，让手指滑到按键<img src=\""
                         + sandboxView.withKey(key_u)
                         + "\"/>上，再从该按键上滑出。"
                         + "<b>注</b>：若滑出后碰到了其他按键，可以重新滑到<img src=\""
                         + sandboxView.withKey(key_u)
                         + "\"/>上再滑出", null)
                .subStep("最后，将手指滑到按键<img src=\""
                         + sandboxView.withKey(key_kuai)
                         + "\"/>上以后，原地释放手指，完成该字的输入。"
                         + "<b>注</b>：滑动过程中，手指可随意滑过其他按键，只要确保手指释放前的按键为最终待输入字的拼音即可",
                         null);
        exercise.addStep("暂时不管已经输入拼音的候选字是否正确，先继续按照上述过程输入其他几个字。"
                         + "<b>注</b>：在输入 <b>输(shu)</b> 时，手指需从按键<img src=\""
                         + sandboxView.withKey(key_sh)
                         + "\"/>上滑出", null);
        exercise.addStep("输入完成后，开始选择已输入拼音的候选字：")
                .subStep("手指点击以选中按键上方输入框中的<img src=\"筷(kuai)\"/>字", null)
                .subStep("在出现的候选字列表区域，可快速向上或向下滑动手指进行翻页，并手指点击正确的候选字，以完成选字",
                         null)
                .subStep("<b>提示</b>：若默认候选字已经是正确的，则可点击<img src=\"ok\"/>按键以确认该字；"
                         + "<img src=\"submit\"/>为输入提交按键，点击后会将当前输入提交至目标输入中；"
                         + "<img src=\"delete\"/>为删除当前拼音，点击后将从输入中删除拼音并回到拼音键盘以接受新的输入；")
                .subStep("接着选择拼音 <b>zi</b> 的候选字", null)
                .subStep("在该字的候选字列表中点击<img src=\"z->zh\"/>按键以切换该拼音的平/翘舌。"
                         + "<b>注</b>：确保最终拼音依然为 <b>zi</b>", null)
                .subStep("点击候选字列表上方的<img src=\"横\"/>等笔画过滤按键，以按候选字所包含的笔画快速过滤出正确的候选字。"
                         + "<b>注</b>：单击笔画过滤按键为增加笔画数，在按键上快速滑出为减少笔画数", null)
                .subStep("继续完成后续拼音的候选字选择和确认操作");
        exercise.addStep("点击按键上方输入框的空白区域，使键盘输入光标移动到当前输入尾部", null);
        exercise.addStep("点击键盘中的<img src=\"submit\"/>按键以提交当前输入至目标输入框中", null);
        exercise.addStep("点击键盘中的<img src=\"revoke\"/>按键将已提交输入撤回。"
                         + "<b>注</b>：这里仅用于演示对已提交输入的修订支持，本次练习不需要对其做修改", null);
        exercise.addStep("长按键盘中的<img src=\"submit\"/>按键，再分别点击<img src=\"带拼音\"/>"
                         + " 等按键以切换提交输入的形式。<b>注</b>：长按<img src=\"submit\"/>按键可退出切换选择，"
                         + "若直接点击该按键则将会以最终切换后的形式提交输入至目标输入框中", null);
        exercise.addStep("本次练习已结束，您可以开始后续练习或者继续当前练习", null);

        return exercise;
    }

    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        switch (msg) {
            case InputList_Committing: {
                InputListCommittingMsgData d = (InputListCommittingMsgData) data;
                commitText(d.text, d.replacements);
                break;
            }
            case InputList_Committed_Revoking: {
                revokeCommitting();
                break;
            }
            case InputList_PairSymbol_Committing: {
                InputListPairSymbolCommittingMsgData d = (InputListPairSymbolCommittingMsgData) data;
                commitText(d.left, d.right);
                break;
            }
            case InputTarget_Cursor_Locating: {
                locateInputCursor(((InputTargetCursorLocatingMsgData) data).anchor);
                break;
            }
            case InputTarget_Selecting: {
                selectInputText(((InputTargetCursorLocatingMsgData) data).anchor);
                break;
            }
            case InputTarget_Editing: {
                InputTargetEditingMsgData d = (InputTargetEditingMsgData) data;
                editInput(d.action);
                break;
            }
            case IME_Switching: {
                Toast.makeText(getApplicationContext(), "仅在输入法状态下才可切换系统输入法", Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    private void commitText(CharSequence text, List<String> replacements) {
        int start = Math.min(this.editText.getSelectionStart(), this.editText.getSelectionEnd());
        int end = Math.max(this.editText.getSelectionStart(), this.editText.getSelectionEnd());

        // Note：假设替换字符的长度均相同
        int replacementStartIndex = Math.max(0, start - text.length());
        CharSequence raw = this.editText.getText().subSequence(replacementStartIndex, start);
        if (replacements.contains(raw.toString())) {
            this.editText.getText().replace(replacementStartIndex, start, text);
            return;
        }

        this.editText.getText().replace(start, end, text);

        // 移动到替换后的文本内容之后
        int offset = text.length();
        this.editText.setSelection(start + offset);
    }

    private void commitText(CharSequence left, CharSequence right) {
        int start = Math.min(this.editText.getSelectionStart(), this.editText.getSelectionEnd());
        int end = Math.max(this.editText.getSelectionStart(), this.editText.getSelectionEnd());

        // Note：先向选区尾部添加符号，以避免选区发生移动
        this.editText.getText().replace(end, end, right);
        this.editText.getText().replace(start, start, left);

        if (start == end) {
            this.editText.setSelection(start + left.length());
        } else {
            // 重新选中初始文本：以上添加文本过程中，EditText 会自动更新选区，且选区结束位置在配对符号的右符号的最右侧
            this.editText.setSelection(this.editText.getSelectionStart(),
                                       this.editText.getSelectionEnd() - right.length());
        }
    }

    private void backwardDeleteInput() {
        sendKey(KeyEvent.KEYCODE_DEL);
    }

    private void locateInputCursor(Motion anchor) {
        if (anchor == null || anchor.distance <= 0) {
            return;
        }

        // Note: 发送按键事件方式可支持上下移动光标，以便于快速定位到目标位置
        for (int i = 0; i < anchor.distance; i++) {
            switch (anchor.direction) {
                case up:
                    sendKey(KeyEvent.KEYCODE_DPAD_UP);
                    break;
                case down:
                    sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
                    break;
                case left:
                    sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
                    break;
                case right:
                    sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
                    break;
            }
        }
    }

    private void selectInputText(Motion anchor) {
        if (anchor == null || anchor.distance <= 0) {
            return;
        }

        // Note: 通过 shift + 方向键 的方式进行文本选择
        sendKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT);
        locateInputCursor(anchor);
        sendKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT);
    }

    private void editInput(InputEditAction action) {
        switch (action) {
            case backspace:
                backwardDeleteInput();
                break;
            case copy:
                this.editText.onTextContextMenuItem(android.R.id.copy);
                break;
            case paste:
                this.editText.onTextContextMenuItem(android.R.id.paste);
                break;
            case cut:
                this.editText.onTextContextMenuItem(android.R.id.cut);
                break;
            case redo:
                this.editText.onTextContextMenuItem(android.R.id.redo);
                break;
            case undo:
                this.editText.onTextContextMenuItem(android.R.id.undo);
                break;
        }
    }

    private void revokeCommitting() {
        editInput(InputEditAction.undo);
    }

    private void sendKey(int code) {
        sendKeyDown(code);
        sendKeyUp(code);
    }

    private void sendKeyDown(int code) {
        this.editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
    }

    private void sendKeyUp(int code) {
        this.editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }
}
