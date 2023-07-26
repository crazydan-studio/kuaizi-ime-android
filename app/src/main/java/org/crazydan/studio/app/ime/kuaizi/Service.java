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

import android.content.Context;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
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
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-29
 */
public class Service extends InputMethodService implements InputMsgListener {
    private InputMethodManager inputMethodManager;

    private ContextThemeWrapper imeViewTheme;
    private ImeInputView imeView;
    private Keyboard.Type imeKeyboardType;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        this.imeViewTheme = new ContextThemeWrapper(getApplicationContext(), R.style.Theme_DayNight_KuaiziIME);
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
        // 只能通过 Context Theme 设置输入视图的暗黑模式，
        // 而 AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) 方式无效
        // https://stackoverflow.com/questions/65433795/unable-to-update-the-day-and-night-modes-in-android-with-window-manager-screens#answer-67340930
        this.imeView = inflateView(R.layout.ime_input_view);

        this.imeView.keyboard.addInputMsgListener(this);

        return this.imeView;
    }

    /**
     * 开始启动输入，先于 {@link #onStartInputView(EditorInfo, boolean)} 调用，
     * 可用于记录弹出键盘的需设置的模式等信息
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        // 确保拼音字典库保持就绪状态
        PinyinDictDB.getInstance().open(getApplicationContext());

        super.onStartInput(attribute, restarting);

        Keyboard.Type keyboardType = Keyboard.Type.Pinyin;
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                keyboardType = Keyboard.Type.Number;
                break;
            case InputType.TYPE_CLASS_PHONE:
                keyboardType = Keyboard.Type.Phone;
                break;
        }

        this.imeKeyboardType = keyboardType;
    }

    /** 每次弹出键盘时调用 */
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        InputMethodSubtype subtype = this.inputMethodManager.getCurrentInputMethodSubtype();
        onCurrentInputMethodSubtypeChanged(subtype);
    }

    /** 响应对子键盘类型的修改 */
    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        Keyboard.Type keyboardType = this.imeKeyboardType;
        if (keyboardType == Keyboard.Type.Pinyin //
            && subtype != null //
            && ("en_US".equals(subtype.getLocale()) //
                || "en_US".equals(subtype.getLanguageTag()))) {
            keyboardType = Keyboard.Type.English;
        }

        this.imeView.keyboard.startInput(keyboardType);
    }

    /** 输入结束隐藏键盘 */
    @Override
    public void onFinishInput() {
        super.onFinishInput();

        this.imeView.keyboard.finishInput();
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
                locateInputCursor((InputCursorLocatingMsgData) data);
                break;
            }
            case SelectingInputText: {
                selectInputText((InputCursorLocatingMsgData) data);
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
                switchIME();
                break;
            }
        }
    }

    private void commitInputting(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }

        if (text.length() == 1) {
            // 回车、单个的数字等字符，需以事件形式发送，才能被所有组件识别
            switch (text.charAt(0)) {
                case ' ':
                    sendKey(KeyEvent.KEYCODE_SPACE);
                    break;
                case '\n':
                    sendKey(KeyEvent.KEYCODE_ENTER);
                    break;
                default:
                    commitText(text);
            }
        } else {
            commitText(text);
        }
    }

    private void backwardDeleteInput() {
        // Note: 发送按键事件的兼容性更好，可由组件处理删除操作
        sendKey(KeyEvent.KEYCODE_DEL);
    }

    private void switchIME() {
        // https://stackoverflow.com/questions/16684482/android-switch-to-a-different-ime-programmatically#answer-16684491
        InputMethodManager manager
                = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.showInputMethodPicker();
        }
    }

    private void locateInputCursor(InputCursorLocatingMsgData data) {
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

    private void selectInputText(InputCursorLocatingMsgData data) {
        Motion anchor = data.anchor;
        if (anchor == null || anchor.distance <= 0) {
            return;
        }

        // Note: 通过 shift + 方向键 的方式进行文本选择
        sendKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT);
        locateInputCursor(data);
        sendKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT);
    }

    private void copyInputText() {
        super.onExtractTextContextMenuItem(android.R.id.copy);
    }

    private void pasteInputText() {
        super.onExtractTextContextMenuItem(android.R.id.paste);
    }

    private void cutInputText() {
        super.onExtractTextContextMenuItem(android.R.id.cut);
    }

    private void undoInputChange() {
        super.onExtractTextContextMenuItem(android.R.id.undo);
    }

    private void redoInputChange() {
        super.onExtractTextContextMenuItem(android.R.id.redo);
    }

    private void commitText(CharSequence text) {
        if (text.length() == 0) {
            return;
        }

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }

        ic.beginBatchEdit();
        // Note: 第二个参数必须为 1，
        // 若设置为0，则浏览器页面的输入框的光标位置不会移动到插入文本之后，
        // 而若设置为文本长度，则某些 app 会将光标移动两倍文本长度
        ic.commitText(text, 1);
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
        ExtractedText extractedText = ic.getExtractedText(new ExtractedTextRequest(), 0);
        if (extractedText == null || extractedText.text == null || extractedText.text.length() == 0) {
            return null;
        }
        return extractedText;
    }

    private <T extends View> T inflateView(int resId) {
        return (T) LayoutInflater.from(this.imeViewTheme).inflate(resId, null);
    }
}
