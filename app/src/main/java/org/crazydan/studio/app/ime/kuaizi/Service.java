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
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodSubtype;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.EditorEditAction;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.EditorCursorMovingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.EditorEditDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListCommitDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListPairSymbolCommitDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView;
import org.crazydan.studio.app.ime.kuaizi.utils.SystemUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-29
 */
public class Service extends InputMethodService implements InputMsgListener {
    private ImeInputView imeView;
    private Keyboard.Config imeKeyboardConfig;

    private int prevFieldId;

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
        super.onDestroy();

        // 确保拼音字典库能够被及时关闭
        PinyinDictDB.getInstance().close();
    }

    /** 输入法视图只创建一次 */
    @Override
    public View onCreateInputView() {
        this.imeView = (ImeInputView) getLayoutInflater().inflate(R.layout.ime_input_view, null);

        this.imeView.addInputMsgListener(this);

        return this.imeView;
    }

    /** 每次弹出键盘时调用 */
    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        // 确保拼音字典库保持就绪状态
        PinyinDictDB.getInstance().open(getApplicationContext());

        boolean singleLineInput = false;
        Keyboard.Config config = this.imeKeyboardConfig;
        Keyboard.Type keyboardType = config != null ? config.getType() : Keyboard.Type.Pinyin;

        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
            case InputType.TYPE_CLASS_PHONE:
                keyboardType = Keyboard.Type.Number;
                break;
            case InputType.TYPE_CLASS_TEXT:
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                    keyboardType = Keyboard.Type.Latin;
                    singleLineInput = true;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    || variation == InputType.TYPE_TEXT_VARIATION_URI
                    || variation == InputType.TYPE_TEXT_VARIATION_FILTER
                    || (attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    singleLineInput = true;
                }
        }

        // Note: 此接口内只是根据输入目标的类型对键盘类型做临时切换，
        // 故不直接修改 this.imeKeyboardConfig
        config = new Keyboard.Config(keyboardType, config);
        config.setSingleLineInput(singleLineInput);

        int prevFieldId = this.prevFieldId;
        this.prevFieldId = attribute.fieldId;

        startImeInput(config, prevFieldId != attribute.fieldId);
    }

    /** 响应系统对子键盘类型的修改 */
    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        Keyboard.Config config = this.imeKeyboardConfig;
        Keyboard.Type keyboardType;

        if (subtype != null //
            && ("en_US".equals(subtype.getLocale()) //
                || "en_US".equals(subtype.getLanguageTag()))) {
            keyboardType = Keyboard.Type.Latin;
        } else {
            keyboardType = Keyboard.Type.Pinyin;
        }

        this.imeKeyboardConfig = new Keyboard.Config(keyboardType, config);
        startImeInput(this.imeKeyboardConfig, false);
    }

    private void startImeInput(Keyboard.Config config, boolean resetInputList) {
        this.imeView.startInput(config, resetInputList);
    }

    /** 输入结束隐藏键盘 */
    @Override
    public void onFinishInput() {
        super.onFinishInput();

        if (this.imeView != null) {
            this.imeView.finishInput();
        }
    }

    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
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
                moveCursor((EditorCursorMovingMsgData) data);
                break;
            }
            case Editor_Range_Select_Doing: {
                selectText((EditorCursorMovingMsgData) data);
                break;
            }
            case Editor_Edit_Doing: {
                EditorEditDoingMsgData d = (EditorEditDoingMsgData) data;
                editEditor(d.action);
                break;
            }
            case IME_Switch_Doing: {
                switchIme();
                break;
            }
        }
    }

    private void backspace() {
        // Note: 发送按键事件的兼容性更好，可由组件处理删除操作
        sendKey(KeyEvent.KEYCODE_DEL);
    }

    private void switchIme() {
        SystemUtils.switchIme(getApplicationContext());
    }

    private void moveCursor(EditorCursorMovingMsgData data) {
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

    private void selectText(EditorCursorMovingMsgData data) {
        Motion anchor = data.anchor;
        if (anchor == null || anchor.distance <= 0) {
            return;
        }

        // Note: 通过 shift + 方向键 的方式进行文本选择
        sendKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT);
        moveCursor(data);
        sendKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT);
    }

    private void editEditor(EditorEditAction action) {
        switch (action) {
            case backspace:
                backspace();
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
        editEditor(EditorEditAction.undo);
    }

    private void commitText(CharSequence text, List<String> replacements) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }

        // Note：假设替换字符的长度均相同
        CharSequence raw = ic.getTextBeforeCursor(text.length(), 0);
        // 替换字符
        if (replacements.contains(raw.toString())) {
            replaceTextBeforeCursor(text, raw.length());
        }
        // 输入字符
        else if (text.length() == 1) {
            char ch = text.charAt(0);
            // 单个字符需以事件形式发送，才能被所有组件识别
            sendKeyChar(ch);
        } else {
            ic.beginBatchEdit();

            addText(text);

            ic.endBatchEdit();
        }
    }

    private void commitText(CharSequence left, CharSequence right) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }

        ExtractedText extractedText = getExtractedText();
        // Note：仅异常情况才获取不到 ExtractedText
        if (extractedText == null) {
            return;
        }

        int start = Math.min(extractedText.selectionStart, extractedText.selectionEnd);
        int end = Math.max(extractedText.selectionStart, extractedText.selectionEnd);

        ic.beginBatchEdit();

        // Note：先向选区尾部添加符号，以避免选区发生移动
        addText(right, end);
        addText(left, start);

        // 重新选中初始文本：确保选区的选择移动方向不变
        int offset = left.length();
        ic.setSelection(extractedText.selectionStart + offset, extractedText.selectionEnd + offset);

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

    private ExtractedText getExtractedText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return null;
        }

        // https://stackoverflow.com/questions/40521324/selection-using-android-ime#answer-58778722
        return ic.getExtractedText(new ExtractedTextRequest(), 0);
    }

    /** 在光标位置添加文本 */
    private void addText(CharSequence text) {
        if (text.length() == 0) {
            return;
        }

        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            // Note: 第二个参数必须为 1，
            // 若设置为0，则浏览器页面的输入框的光标位置不会移动到插入文本之后，
            // 而若设置为文本长度，则某些 app 会将光标移动两倍文本长度
            ic.commitText(text, 1);
        }
    }

    /** 向指定位置添加文本 */
    private void addText(CharSequence text, int pos) {
        replaceText(text, pos, pos);
    }

    /** 替换光标之前指定长度的文本 */
    private void replaceTextBeforeCursor(CharSequence text, int length) {
        ExtractedText extractedText = getExtractedText();
        if (extractedText == null) {
            return;
        }

        int start = extractedText.selectionStart;
        replaceText(text, start - length, start);
    }

    /** 替换指定范围内的文本 */
    private void replaceText(CharSequence text, int posStart, int posEnd) {
        if (text.length() == 0) {
            return;
        }

        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            // 移动光标到指定位置
            ic.setSelection(posStart, posEnd);

            addText(text);
        }
    }
}
