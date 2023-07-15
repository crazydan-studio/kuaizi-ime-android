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
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputCommittingMsgData;
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

        this.imeView = findViewById(R.id.ime_view);

        this.imeView.keyboard.addInputMsgListener(this);
        this.imeView.keyboard.changeKeyboardType(Keyboard.Type.Pinyin);
    }

    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        switch (msg) {
            case InputCommitting:
                commitInputting(((InputCommittingMsgData) data).text);
                break;
        }
    }

    private void commitInputting(StringBuilder text) {
        this.editText.getText().append(text);

        this.imeView.keyboard.finishInput();
    }
}
