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

package org.crazydan.studio.app.ime.kuaizi;

import java.util.List;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodSubtype;
import org.crazydan.studio.app.ime.kuaizi.common.Motion;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorSelection;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigChangeListener;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorCursorMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputListCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputListPairSymbolCommitMsgData;

/**
 * 输入法生命周期: https://stackoverflow.com/questions/19961618/inputmethodservice-lifecycle-bug#answer-66238856
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-29
 */
public class IMEService extends InputMethodService implements UserMsgListener, InputMsgListener, ConfigChangeListener {
    protected final Logger log = Logger.getLogger(getClass());

    private IMEConfig imeConfig;
    private IMEditor ime;
    private IMEditorView imeView;

    private int prevFieldId;
    /** 记录可撤回输入的选区信息 */
    private EditorSelection.ChangeRevertion editorChangeRevertion;

    /** 系统输入法切换到本输入法时调用 */
    @Override
    public void onCreate() {
        super.onCreate();

        this.imeConfig = IMEConfig.create(getApplicationContext());
        this.imeConfig.setListener(this);
    }

    /** 切换到其他系统输入法时调用 */
    @Override
    public void onDestroy() {
        if (this.ime != null) {
            this.ime.destroy();
        }
        if (this.imeConfig != null) {
            this.imeConfig.destroy();
        }

        this.ime = null;
        this.imeView = null;
        this.imeConfig = null;
        this.editorChangeRevertion = null;

        super.onDestroy();
    }

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

    /** 输入法视图只创建一次 */
    @Override
    public View onCreateInputView() {
        this.ime = IMEditor.create(this.imeConfig.mutable());
        this.imeView = (IMEditorView) getLayoutInflater().inflate(R.layout.ime_view, null);

        // 通过当前层向逻辑层和视图层分别转发用户消息和输入消息
        this.ime.setListener(this);
        this.imeView.setListener(this);
        this.imeView.setConfig(this.imeConfig.immutable());

        return this.imeView;
    }

    /** 每次弹出键盘时调用 */
    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        int oldPrevFieldId = this.prevFieldId;
        // Note: 熄屏前后同一编辑组件的 id 会发生变化，会导致亮屏后输入丢失
        this.prevFieldId = attribute.fieldId;

        boolean resetInputting = oldPrevFieldId != this.prevFieldId;
        boolean singleLineInputting = false;
        boolean passwordInputting = false;

        // Note: 默认保持键盘类型不变
        Keyboard.Type keyboardType = Keyboard.Type.Keep_Current;

        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
            case InputType.TYPE_CLASS_PHONE:
                keyboardType = Keyboard.Type.Number;
                // 输入数字的情况下，应该强制清空输入列表
                resetInputting = true;
                break;
            case InputType.TYPE_CLASS_TEXT:
                // Note: 文本输入默认仅由 latin 和 pinyin 键盘处理
                if (this.ime.getKeyboardType() != Keyboard.Type.Latin) {
                    keyboardType = Keyboard.Type.Pinyin;
                }

                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                    keyboardType = Keyboard.Type.Latin;
                    singleLineInputting = true;
                    passwordInputting = true;
                    // 清空输入列表，以确保采用直输模式
                    resetInputting = true;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    || variation == InputType.TYPE_TEXT_VARIATION_URI
                    || variation == InputType.TYPE_TEXT_VARIATION_FILTER
                    || (attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    singleLineInputting = true;
                }
        }

        doStartInput(keyboardType, singleLineInputting, passwordInputting, resetInputting);
    }

    /** 响应系统对键盘类型的修改 */
    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        // Note: 切换系统键盘时，视图可能还未创建
        if (this.ime == null) {
            return;
        }

        doStartInput(Keyboard.Type.By_ImeSubtype, null, null, false);
    }

    /** 隐藏输入：暂时退出编辑，但会恢复编辑 */
    @Override
    public void onFinishInputView(boolean finishingInput) {
        if (this.ime != null) {
            this.ime.hide();
        }

        super.onFinishInputView(finishingInput);
    }

    /** 输入结束：彻底退出编辑。注意，熄屏/亮屏也会调用该接口 */
    @Override
    public void onFinishInput() {
        // Note: 在 #onCreateInputView 之前，该接口也会被调用
        if (this.ime != null) {
            this.ime.exit();
        }
        this.editorChangeRevertion = null;

        super.onFinishInput();
    }

    private void doStartInput(
            Keyboard.Type keyboardType, //
            Boolean useSingleLineInputting, Boolean usePasswordInputting, //
            boolean resetInputting
    ) {
        this.editorChangeRevertion = null;

        Context context = getApplicationContext();
        IMESubtype imeSubtype = IMESubtype.from(context);
        Keyboard.Orientation orientation = Keyboard.Orientation.from(context);
        this.imeConfig.set(ConfigKey.ime_subtype, imeSubtype);
        this.imeConfig.set(ConfigKey.orientation, orientation);
        this.imeConfig.set(ConfigKey.single_line_input, useSingleLineInputting, true);
        this.imeConfig.set(ConfigKey.disable_input_key_popup_tips, usePasswordInputting, true);

        // Note: 不重置输入列表，以避免误操作导致输入被清空
        this.ime.start(context, keyboardType, false /*resetInputting*/);
    }

    // =============================== Start: 消息处理 ===================================

    @Override
    public void onChanged(ConfigKey key, Object oldValue, Object newValue) {
        // Note: 配置变更也可能发生在输入法未初始化时
        if (this.ime != null) {
            this.ime.onChanged(key, oldValue, newValue);
        }
    }

    @Override
    public void onMsg(UserInputMsg msg) {
        this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                msg.getClass(), this.ime.getClass()
        });

        this.ime.onMsg(msg);

        this.log.endTreeLog();
    }

    @Override
    public void onMsg(UserKeyMsg msg) {
        this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                msg.getClass(), this.ime.getClass()
        });

        this.ime.onMsg(msg);

        this.log.endTreeLog();
    }

    @Override
    public void onMsg(InputMsg msg) {
        this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                msg.getClass(), this.imeView.getClass()
        });

        this.imeView.onMsg(msg);

        this.log.endTreeLog();
        /////////////////////////////////////////////////////////////////
        this.log.beginTreeLog("Handle %s", () -> new Object[] { msg.getClass() }) //
                .debug("Message Type: %s", () -> new Object[] { msg.type }) //
                .debug("Message Data: %s", () -> new Object[] { msg.data() });

        handleMsg(msg);

        this.log.endTreeLog();
    }

    private void handleMsg(InputMsg msg) {
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
                moveCursor(msg.data());
                break;
            }
            case Editor_Range_Select_Doing: {
                selectText(msg.data());
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
            case IME_Switch_Doing: {
                switchIme();
                break;
            }
            default: {
                this.log.warn("Ignore message %s", () -> new Object[] { msg.type });
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

    private void editText(EditorAction action) {
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
        EditorSelection.ChangeRevertion revertion = this.editorChangeRevertion;
        if (revertion == null) {
            return;
        }

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }

        // 将 从编辑前的开始位置 到 编辑后的终点位置 之间的内容恢复为编辑前的内容
        replaceText(ic, revertion.before.content, revertion.before.start, revertion.after.end);
        // 还原编辑前的选区
        ic.setSelection(revertion.before.start, revertion.before.end);
    }

    private void commitText(CharSequence text, List<String> replacements) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }

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

            this.editorChangeRevertion = new EditorSelection.ChangeRevertion(before, after);
        }
    }

    private void commitPairSymbolText(CharSequence left, CharSequence right) {
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

        // 重新选中初始文本
        int offset = left.length();
        ic.setSelection(start + offset, end + offset);

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

        int start = selection.start;
        replaceText(ic, text, start - length, start);
    }

    /** 替换指定范围内的文本 */
    private void replaceText(InputConnection ic, CharSequence text, int posStart, int posEnd) {
        // 移动光标到指定位置
        ic.setSelection(posStart, posEnd);

        addText(ic, text);
    }
}
