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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.view;

import java.util.List;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import org.crazydan.studio.app.ime.kuaizi.internal.EditorSelection;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.EditorEditAction;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.MsgBus;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.EditorCursorMovingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.EditorEditDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListCommitDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListPairSymbolCommitDoingMsgData;

/**
 * 用于筷字输入法使用练习的编辑框
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-16
 */
public class ExerciseEditText extends androidx.appcompat.widget.AppCompatEditText implements InputMsgListener {
    private EditorSelection editorSelection;

    public ExerciseEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 禁止获取焦点时弹出系统输入法
        // Note：需同时在 activity 的配置上添加 android:windowSoftInputMode="stateAlwaysHidden"
        setShowSoftInputOnFocus(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        MsgBus.register(InputMsg.class, this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        MsgBus.unregister(getClass());
    }

    @Override
    public void onMsg(Keyboard keyboard, InputMsg msg, InputMsgData data) {
        switch (msg) {
            case InputList_Commit_Doing: {
                InputListCommitDoingMsgData d = (InputListCommitDoingMsgData) data;
                commitText(d.text, d.replacements);
                break;
            }
            case InputList_Committed_Revoke_Doing: {
                revokeTextCommitting();
                break;
            }
            case InputList_PairSymbol_Commit_Doing: {
                InputListPairSymbolCommitDoingMsgData d = (InputListPairSymbolCommitDoingMsgData) data;
                commitText(d.left, d.right);
                break;
            }
            case Editor_Cursor_Move_Doing: {
                moveCursor(((EditorCursorMovingMsgData) data).anchor);
                break;
            }
            case Editor_Range_Select_Doing: {
                selectText(((EditorCursorMovingMsgData) data).anchor);
                break;
            }
            case Editor_Edit_Doing: {
                EditorEditDoingMsgData d = (EditorEditDoingMsgData) data;
                editEditor(d.action);
                break;
            }
        }
    }

    private void commitText(CharSequence text, List<String> replacements) {
        this.editorSelection = null;

        EditorSelection before = EditorSelection.from(this);
        if (before == null) {
            return;
        }

        int start = before.start;
        int end = before.end;

        Editable editable = getText();
        // Note：假设替换字符的长度均相同
        int replacementStartIndex = Math.max(0, start - text.length());
        CharSequence raw = editable.subSequence(replacementStartIndex, start);
        if (replacements.contains(raw.toString())) {
            replaceText(text, replacementStartIndex, start);
            return;
        }

        replaceText(text, start, end);

        // 移动到替换后的文本内容之后
        int offset = text.length();
        setSelection(start + offset);

        EditorSelection after = EditorSelection.from(this);
        this.editorSelection = new EditorSelection(after.start, after.end, before.start, before.end, before.content);
    }

    private void commitText(CharSequence left, CharSequence right) {
        EditorSelection selection = EditorSelection.from(this);
        if (selection == null) {
            return;
        }

        int start = selection.start;
        int end = selection.end;

        // Note：先向选区尾部添加符号，以避免选区发生移动
        replaceText(right, end, end);
        replaceText(left, start, start);

        if (start == end) {
            setSelection(start + left.length());
        } else {
            // 重新选中初始文本：以上添加文本过程中，EditText 会自动更新选区，且选区结束位置在配对符号的右符号的最右侧
            setSelection(selection.origStart, selection.origEnd - right.length());
        }
    }

    private void backspace() {
        sendKey(KeyEvent.KEYCODE_DEL);
    }

    private void moveCursor(Motion anchor) {
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

    private void selectText(Motion anchor) {
        if (anchor == null || anchor.distance <= 0) {
            return;
        }

        // Note: 通过 shift + 方向键 的方式进行文本选择
        sendKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT);
        moveCursor(anchor);
        sendKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT);
    }

    private void editEditor(EditorEditAction action) {
        switch (action) {
            case backspace:
                backspace();
                break;
            case copy:
                onTextContextMenuItem(android.R.id.copy);
                break;
            case paste:
                onTextContextMenuItem(android.R.id.paste);
                break;
            case cut:
                onTextContextMenuItem(android.R.id.cut);
                break;
            case redo:
                onTextContextMenuItem(android.R.id.redo);
                break;
            case undo:
                onTextContextMenuItem(android.R.id.undo);
                break;
        }
    }

    private void revokeTextCommitting() {
        //editEditor(EditorEditAction.undo);
        EditorSelection selection = this.editorSelection;
        if (selection == null) {
            return;
        }

        replaceText(selection.content, selection.origStart, selection.end);
        setSelection(selection.origStart, selection.origEnd);
    }

    private void sendKey(int code) {
        sendKeyDown(code);
        sendKeyUp(code);
    }

    private void sendKeyDown(int code) {
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
    }

    private void sendKeyUp(int code) {
        dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    /** 替换指定范围内的文本 */
    private void replaceText(CharSequence text, int posStart, int posEnd) {
        Editable editable = getText();
        if (editable == null) {
            return;
        }

        if (text == null) {
            text = "";
        }

        editable.replace(posStart, posEnd, text);
    }
}
