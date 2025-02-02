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

package org.crazydan.studio.app.ime.kuaizi.ui.common;

import java.util.List;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import androidx.appcompat.widget.AppCompatEditText;
import org.crazydan.studio.app.ime.kuaizi.common.Motion;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorSelection;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorCursorMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputListCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputListPairSymbolCommitMsgData;

/**
 * 支持 筷字输入法 输入的编辑框
 * <p/>
 * 注：由外部集成方统一分发 {@link InputMsg} 消息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-16
 */
public class ImeSupportEditText extends AppCompatEditText implements InputMsgListener {
    /** 记录可撤回输入的选区信息 */
    private EditorSelection.ChangeRevertion editorChangeRevertion;

    public ImeSupportEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 禁止获取焦点时弹出系统输入法
        // Note：需同时在 activity 的配置上添加 android:windowSoftInputMode="stateAlwaysHidden"
        setShowSoftInputOnFocus(false);
    }

    @Override
    public void onMsg(InputMsg msg) {
        switch (msg.type) {
            case InputList_Commit_Doing: {
                this.editorChangeRevertion = null;

                InputListCommitMsgData d = msg.data();
                commitText(d.text, d.replacements);
                break;
            }
            case InputList_Committed_Revoke_Doing: {
                revokeTextCommitting();
                this.editorChangeRevertion = null;
                break;
            }
            case InputList_PairSymbol_Commit_Doing: {
                this.editorChangeRevertion = null;

                InputListPairSymbolCommitMsgData d = msg.data();
                commitPairSymbolText(d.left, d.right);
                break;
            }
            case Editor_Cursor_Move_Doing: {
                EditorCursorMsgData d = msg.data();
                moveCursor(d.anchor);
                break;
            }
            case Editor_Range_Select_Doing: {
                EditorCursorMsgData d = msg.data();
                selectText(d.anchor);
                break;
            }
            case Editor_Edit_Doing: {
                EditorEditMsgData d = msg.data();

                if (EditorAction.hasEffect(d.action)) {
                    this.editorChangeRevertion = null;
                }
                editText(d.action);
                break;
            }
        }
    }

    private void commitText(CharSequence text, List<String> replacements) {
        EditorSelection before = EditorSelection.from(this);

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
        this.editorChangeRevertion = new EditorSelection.ChangeRevertion(before, after);
    }

    private void commitPairSymbolText(CharSequence left, CharSequence right) {
        EditorSelection selection = EditorSelection.from(this);

        int start = selection.start;
        int end = selection.end;

        // Note：先向选区尾部添加符号，以避免选区发生移动
        replaceText(right, end, end);
        replaceText(left, start, start);

        // 重新选中初始文本
        int offset = left.length();
        setSelection(start + offset, end + offset);
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

    private void editText(EditorAction action) {
        switch (action) {
            case backspace:
                backspace();
                break;
            case select_all:
                onTextContextMenuItem(android.R.id.selectAll);
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
        // Note: 撤销由编辑器控制，其可能会撤销间隔时间较短的多个输入，
        // 故而，只能采用记录输入前的范围，再还原的方式实现输入的撤回
        //editEditor(EditorEditAction.undo);
        EditorSelection.ChangeRevertion revertion = this.editorChangeRevertion;
        if (revertion == null) {
            return;
        }

        // 将 从编辑前的开始位置 到 编辑后的终点位置 之间的内容恢复为编辑前的内容
        replaceText(revertion.before.content, revertion.before.start, revertion.after.end);
        // 还原编辑前的选区
        setSelection(revertion.before.start, revertion.before.end);
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

    /** 替换指定范围内（posStart ~ posEnd）的文本 */
    private void replaceText(CharSequence text, int posStart, int posEnd) {
        Editable editable = getText();
        assert editable != null;

        if (text == null) {
            text = "";
        }

        editable.replace(posStart, posEnd, text);
    }
}
