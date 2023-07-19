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
import android.text.Editable;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputCommittingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputCursorLocatingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputTextSelectingMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView;

/**
 * 功能演示页面
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class Demo extends AppCompatActivity implements InputMsgListener {
    private EditText editText;
    private ImeInputView imeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 启用暗黑主题: https://juejin.cn/post/7130482856878407694
        // https://developer.android.com/develop/ui/views/theming/darktheme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        setContentView(R.layout.demo_activity);

        this.editText = findViewById(R.id.text_input);
        this.editText.setClickable(false);

        this.imeView = findViewById(R.id.ime_view);

        this.imeView.keyboard.addInputMsgListener(this);
        this.imeView.keyboard.changeKeyboardType(Keyboard.Type.Pinyin);

        this.editText.setText("这是一段测试文本\nThis is a text for testing\n欢迎使用筷字输入法\nThanks for using Kuaizi Input Method");
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
                selectInputText((InputTextSelectingMsgData) data);
                break;
            }
            case IMESwitching: {
                Toast.makeText(getApplicationContext(), "仅在输入法状态下才可切换系统输入法", Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    private void commitInputting(StringBuilder text) {
        int start = Math.min(this.editText.getSelectionStart(), this.editText.getSelectionEnd());
        int end = Math.max(this.editText.getSelectionStart(), this.editText.getSelectionEnd());

        this.editText.getText().replace(start, end, text);
        // 移动到替换后的文本内容之后
        this.editText.setSelection(start + text.length());
    }

    private void backwardDeleteInput() {
        Editable text = this.editText.getText();
        int length = text.toString().length();
        if (length == 0) {
            return;
        }

        int start = Math.min(this.editText.getSelectionStart(), this.editText.getSelectionEnd());
        int end = Math.max(this.editText.getSelectionStart(), this.editText.getSelectionEnd());
        // 删除光标前的
        if (start == end) {
            if (start > 0) {
                text.delete(start - 1, end);
            }
        }
        // 删除已选中的
        else {
            text.delete(start, end);
        }
    }

    private void locateInputCursor(InputCursorLocatingMsgData data) {
        Motion anchor = data.anchor;
        if (anchor == null || anchor.distance <= 0) {
            return;
        }

        Editable text = this.editText.getText();
        int length = text.toString().length();
        if (length == 0) {
            return;
        }

        int start = moveSelectionCursor(anchor, length, this.editText.getSelectionStart());
        this.editText.setSelection(start);
    }

    private void selectInputText(InputTextSelectingMsgData data) {
        Motion anchor1 = data.anchor1;
        Motion anchor2 = data.anchor2;

        Editable text = this.editText.getText();
        int length = text.toString().length();
        if (length == 0) {
            return;
        }

        int start = moveSelectionCursor(anchor1, length, this.editText.getSelectionStart());
        int end = moveSelectionCursor(anchor2, length, this.editText.getSelectionEnd());

        this.editText.setSelection(start, end);
    }

    private int moveSelectionCursor(Motion anchor, int length, int current) {
        switch (anchor.direction) {
            case left:
            case up:
                current -= anchor.distance;
                break;
            case right:
            case down:
                current += anchor.distance;
                break;
        }

        return Math.min(length, Math.max(0, current));
    }
}
