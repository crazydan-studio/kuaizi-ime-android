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

package org.crazydan.studio.app.ime.kuaizi;

import java.util.List;

import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodSubtype;
import org.crazydan.studio.app.ime.kuaizi.common.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.pane.EditorSelection;
import org.crazydan.studio.app.ime.kuaizi.pane.InputConfig;
import org.crazydan.studio.app.ime.kuaizi.pane.InputPane;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.EditorEditAction;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.EditorCursorMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputListCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputListPairSymbolCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputPaneView;

/**
 * 输入法生命周期: https://stackoverflow.com/questions/19961618/inputmethodservice-lifecycle-bug#answer-66238856
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-29
 */
public class ImeService extends InputMethodService implements InputMsgListener {
    private InputPane inputPane;
    private InputPaneView inputPaneView;

    private int prevFieldId;
    private EditorSelection editorSelection;

    /**
     * 启动输入，先于 {@link #onCreateInputView()} 和
     * {@link #onStartInputView(EditorInfo, boolean)} 调用
     * <p/>
     * Note: 不可进行视图渲染相关的调用；该接口在显示和隐藏键盘时均会被调用；
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
    }

    /** 切换到其他系统输入法时调用 */
    @Override
    public void onDestroy() {
        this.inputPane.destroy();

        this.inputPane = null;
        this.inputPaneView = null;
        this.editorSelection = null;

        super.onDestroy();
    }

    /** 输入法视图只创建一次 */
    @Override
    public View onCreateInputView() {
        this.inputPane = InputPane.create(getApplicationContext());
        this.inputPaneView = (InputPaneView) getLayoutInflater().inflate(R.layout.input_pane_view, null);

        // 从视图向键盘转发按键消息
        this.inputPaneView.setListener(this.inputPane);
        // 从键盘向视图转发键盘的消息
        this.inputPane.addListener(this.inputPaneView);

        // 响应键盘消息以实现文本编辑
        this.inputPane.addListener(this);

        return this.inputPaneView;
    }

    /** 每次弹出键盘时调用 */
    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        boolean singleLineInputting = false;
        boolean passwordInputting = false;

        // Note: 默认保持键盘类型不变
        Keyboard.Type keyboardType = Keyboard.Type.Keep_Current;

        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
            case InputType.TYPE_CLASS_PHONE:
                keyboardType = Keyboard.Type.Number;
                break;
            case InputType.TYPE_CLASS_TEXT:
                // Note: 切换前为数字键盘时，需强制切换到拼音键盘，否则无法输入文本
                if (this.inputPane.getKeyboardType() == Keyboard.Type.Number) {
                    keyboardType = Keyboard.Type.Pinyin;
                }

                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                    keyboardType = Keyboard.Type.Latin;
                    singleLineInputting = true;
                    passwordInputting = true;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    || variation == InputType.TYPE_TEXT_VARIATION_URI
                    || variation == InputType.TYPE_TEXT_VARIATION_FILTER
                    || (attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    singleLineInputting = true;
                }
        }

        int prevFieldId = this.prevFieldId;
        // Note: 熄屏前后同一编辑组件的 id 会发生变化，会导致亮屏后输入丢失
        this.prevFieldId = attribute.fieldId;

        doStartInput(keyboardType, singleLineInputting, passwordInputting, prevFieldId != attribute.fieldId);
    }

    /** 响应系统对键盘类型的修改 */
    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        // Note: 切换系统键盘时，视图可能还未创建
        if (this.inputPaneView == null) {
            return;
        }

        doStartInput(Keyboard.Type.By_ImeSubtype, null, null, false);
    }

    /** 隐藏输入：暂时退出编辑，但会恢复编辑 */
    @Override
    public void onFinishInputView(boolean finishingInput) {
        this.inputPane.hide();
        super.onFinishInputView(finishingInput);
    }

    /** 输入结束：彻底退出编辑。注意，熄屏也会调用该接口 */
    @Override
    public void onFinishInput() {
        this.inputPane.exit();
        this.editorSelection = null;

        super.onFinishInput();
    }

    private void doStartInput(
            Keyboard.Type keyboardType, //
            Boolean useSingleLineInputting, Boolean usePasswordInputting, //
            boolean resetInputting
    ) {
        this.editorSelection = null;

        ImeSubtype imeSubtype = SystemUtils.getImeSubtype(getApplicationContext());
        this.inputPane.updateConfig((conf) -> {
            conf.set(InputConfig.Key.ime_subtype, imeSubtype);
            conf.set(InputConfig.Key.single_line_input, useSingleLineInputting, true);
            conf.set(InputConfig.Key.disable_input_key_popup_tips, usePasswordInputting, true);
        });

        this.inputPane.start(keyboardType, resetInputting);
    }

    // =============================== Start: 消息处理 ===================================

    @Override
    public void onMsg(Keyboard keyboard, InputMsg msg) {
        switch (msg.type) {
            case InputList_Commit_Doing: {
                InputListCommitMsgData d = (InputListCommitMsgData) msg.data;
                commitText(d.text, d.replacements);
                break;
            }
            case InputList_Committed_Revoke_Doing: {
                revokeTextCommitting();
                break;
            }
            case InputList_PairSymbol_Commit_Doing: {
                InputListPairSymbolCommitMsgData d = (InputListPairSymbolCommitMsgData) msg.data;
                commitText(d.left, d.right);
                break;
            }
            case Editor_Cursor_Move_Doing: {
                moveCursor((EditorCursorMsgData) msg.data);
                break;
            }
            case Editor_Range_Select_Doing: {
                selectText((EditorCursorMsgData) msg.data);
                break;
            }
            case Editor_Edit_Doing: {
                EditorEditMsgData d = (EditorEditMsgData) msg.data;
                editText(d.action);
                break;
            }
            case IME_Switch_Doing: {
                switchIme();
                break;
            }
        }
    }

    // =============================== End: 消息处理 ===================================

    private void backspace() {
        // Note: 发送按键事件的兼容性更好，可由组件处理删除操作
        sendKey(KeyEvent.KEYCODE_DEL);
    }

    private void switchIme() {
        SystemUtils.switchIme(getApplicationContext());
    }

    private void moveCursor(EditorCursorMsgData data) {
        Motion anchor = data.anchor;
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

    private void selectText(EditorCursorMsgData data) {
        Motion anchor = data.anchor;
        if (anchor == null || anchor.distance <= 0) {
            return;
        }

        // Note: 通过 shift + 方向键 的方式进行文本选择
        sendKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT);
        moveCursor(data);
        sendKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT);
    }

    private void editText(EditorEditAction action) {
        switch (action) {
            case backspace:
                backspace();
                break;
            case select_all:
                super.onExtractTextContextMenuItem(android.R.id.selectAll);
                break;
            case copy:
                super.onExtractTextContextMenuItem(android.R.id.copy);
                break;
            case paste:
                super.onExtractTextContextMenuItem(android.R.id.paste);
                break;
            case cut:
                super.onExtractTextContextMenuItem(android.R.id.cut);
                break;
            case redo:
                super.onExtractTextContextMenuItem(android.R.id.redo);
                break;
            case undo:
                super.onExtractTextContextMenuItem(android.R.id.undo);
                break;
        }
    }

    private void revokeTextCommitting() {
        // Note: 撤销由编辑器控制，其可能会撤销间隔时间较短的多个输入，
        // 故而，只能采用记录输入前的范围，再还原的方式实现输入的撤回
        //editEditor(EditorEditAction.undo);
        EditorSelection selection = this.editorSelection;
        if (selection == null) {
            return;
        }

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }

        replaceText(ic, selection.content, selection.origStart, selection.end);
        // 重新选中
        ic.setSelection(selection.origStart, selection.origEnd);
    }

    private void commitText(CharSequence text, List<String> replacements) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }
        this.editorSelection = null;

        // Note: 假设替换字符的长度均相同
        CharSequence raw = ic.getTextBeforeCursor(text.length(), 0);
        // 替换字符
        if (replacements.contains(raw.toString())) {
            replaceTextBeforeCursor(ic, text, raw.length());
        }
        // 输入字符
        else if (text.length() == 1) {
            char ch = text.charAt(0);
            // 单个字符需以事件形式发送，才能被所有组件识别
            sendKeyChar(ch);
        } else {
            EditorSelection before = EditorSelection.from(ic);

            addText(ic, text);

            EditorSelection after = EditorSelection.from(ic);

            this.editorSelection = new EditorSelection(after.start,
                                                       after.end,
                                                       before.start,
                                                       before.end,
                                                       before.content);
        }
    }

    private void commitText(CharSequence left, CharSequence right) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }

        EditorSelection selection = EditorSelection.from(ic);
        int start = selection.start;
        int end = selection.end;

        // Note: 仅包含多个编辑动作时，才启用编辑批处理
        ic.beginBatchEdit();

        // Note: 先向选区尾部添加符号，以避免选区发生移动
        addText(ic, right, end);
        addText(ic, left, start);

        // 重新选中初始文本：确保选区的选择移动方向不变
        int offset = left.length();
        ic.setSelection(selection.origStart + offset, selection.origEnd + offset);

        ic.endBatchEdit();
    }

    private void sendKey(int code) {
        sendKeyDown(code);
        sendKeyUp(code);
    }

    private void sendKeyDown(int code) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        }
    }

    private void sendKeyUp(int code) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
        }
    }

    /** 在光标位置添加文本 */
    private void addText(InputConnection ic, CharSequence text) {
        if (text == null) {
            text = "";
        }

        // Note: 第二个参数必须为 1，
        // 若设置为0，则浏览器页面的输入框的光标位置不会移动到插入文本之后，
        // 而若设置为文本长度，则某些 app 会将光标移动两倍文本长度
        ic.commitText(text, 1);
    }

    /** 向指定位置添加文本 */
    private void addText(InputConnection ic, CharSequence text, int pos) {
        replaceText(ic, text, pos, pos);
    }

    /** 替换光标之前指定长度的文本 */
    private void replaceTextBeforeCursor(InputConnection ic, CharSequence text, int length) {
        EditorSelection selection = EditorSelection.from(ic);

        int start = selection.origStart;
        replaceText(ic, text, start - length, start);
    }

    /** 替换指定范围内的文本 */
    private void replaceText(InputConnection ic, CharSequence text, int posStart, int posEnd) {
        // 移动光标到指定位置
        ic.setSelection(posStart, posEnd);

        addText(ic, text);
    }
}
