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

import java.util.List;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
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
import org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-11
 */
public class CaseMain extends FollowSystemThemeActivity implements InputMsgListener {
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_case_activity);

        this.editText = findViewById(R.id.text_input);
        this.editText.setClickable(false);

        ImeInputView imeView = findViewById(R.id.ime_view);
        imeView.addInputMsgListener(this);
        imeView.updateKeyboard(new Keyboard.Config(Keyboard.Type.Pinyin));
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

    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        switch (msg) {
            case InputList_Committing: {
                InputListCommittingMsgData d = (InputListCommittingMsgData) data;
                commitText(d.text, d.replacements);
                break;
            }
            case InputList_Revoking: {
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