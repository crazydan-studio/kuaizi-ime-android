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

package org.crazydan.studio.app.ime.kuaizi.ui;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputCommittingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputCursorLocatingMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView;

/**
 * 功能演示页面
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class Demo extends AppCompatActivity implements InputMsgListener {
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 启用暗黑主题: https://juejin.cn/post/7130482856878407694
        // https://developer.android.com/develop/ui/views/theming/darktheme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        setContentView(R.layout.demo_activity);

        this.editText = findViewById(R.id.text_input);
        this.editText.setClickable(false);
        this.editText.setText("这是一段测试文本\nThis is a text for testing\n欢迎使用筷字输入法\nThanks for using Kuaizi Input Method");

        ImeInputView imeView = findViewById(R.id.ime_view);
        imeView.keyboard.addInputMsgListener(this);
        imeView.keyboard.changeKeyboardType(Keyboard.Type.Pinyin);
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
            case InputCommitting: {
                commitInputting(((InputCommittingMsgData) data).text);
                break;
            }
            case InputBackwardDeleting: {
                backwardDeleteInput();
                break;
            }
            case LocatingInputCursor: {
                locateInputCursor(((InputCursorLocatingMsgData) data).anchor);
                break;
            }
            case SelectingInputText: {
                selectInputText(((InputCursorLocatingMsgData) data).anchor);
                break;
            }
            case CopyingInputText: {
                copyInputText();
                break;
            }
            case PastingInputText: {
                pasteInputText();
                break;
            }
            case CuttingInputText: {
                cutInputText();
                break;
            }
            case UndoingInputChange: {
                undoInputChange();
                break;
            }
            case RedoingInputChange: {
                redoInputChange();
                break;
            }
            case IMESwitching: {
                Toast.makeText(getApplicationContext(), "仅在输入法状态下才可切换系统输入法", Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    private void commitInputting(CharSequence text) {
        int start = Math.min(this.editText.getSelectionStart(), this.editText.getSelectionEnd());
        int end = Math.max(this.editText.getSelectionStart(), this.editText.getSelectionEnd());

        this.editText.getText().replace(start, end, text);
        // 移动到替换后的文本内容之后
        this.editText.setSelection(start + text.length());
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

    private void copyInputText() {
        this.editText.onTextContextMenuItem(android.R.id.copy);
    }

    private void pasteInputText() {
        this.editText.onTextContextMenuItem(android.R.id.paste);
    }

    private void cutInputText() {
        this.editText.onTextContextMenuItem(android.R.id.cut);
    }

    private void undoInputChange() {
        this.editText.onTextContextMenuItem(android.R.id.undo);
    }

    private void redoInputChange() {
        this.editText.onTextContextMenuItem(android.R.id.redo);
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
