/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

import java.util.List;

import android.content.Context;
import android.os.Bundle;
import org.crazydan.studio.app.ime.kuaizi.ImeConfig;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigChangeListener;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.pane.InputPane;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.KeyTableConfig;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputPaneView;

/**
 * 集成当前输入法的窗口
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-17
 */
public abstract class ImeIntegratedActivity extends FollowSystemThemeActivity
        implements UserMsgListener, InputMsgListener, ConfigChangeListener {
    private final int layoutResId;

    protected ImeConfig config;

    protected InputPane inputPane;
    protected InputPaneView inputPaneView;

    public ImeIntegratedActivity(int layoutResId) {
        super();
        this.layoutResId = layoutResId;
    }

    // ===================== Start: 生命周期 ==================
    // Activity 生命周期: https://media.geeksforgeeks.org/wp-content/uploads/20210303165235/ActivityLifecycleinAndroid-601x660.jpg

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(this.layoutResId);

        Context context = getApplicationContext();
        this.config = ImeConfig.create(context);
        this.inputPane = InputPane.create(context);
        this.inputPaneView = findViewById(R.id.input_pane_view);

        this.inputPane.setConfig(this.config.mutable());
        this.inputPaneView.setConfig(this.config.immutable());

        this.config.setListener(this);
        this.inputPane.setListener(this);
        this.inputPaneView.setListener(this);
    }

    @Override
    protected void onDestroy() {
        this.inputPane.destroy();
        this.config.destroy();

        super.onDestroy();
    }

    // ===================== End: 生命周期 ==================

    // =============================== Start: 消息处理 ===================================

    @Override
    public void onChanged(ConfigKey key, Object oldValue, Object newValue) {
        this.inputPane.onChanged(key, oldValue, newValue);
    }

    @Override
    public void onMsg(UserInputMsg msg) {
        this.inputPane.onMsg(msg);
    }

    @Override
    public void onMsg(UserKeyMsg msg) {
        this.inputPane.onMsg(msg);
    }

    @Override
    public void onMsg(InputMsg msg) {
        this.inputPaneView.onMsg(msg);
    }

    // =============================== End: 消息处理 ===================================

    protected void prepareInputs(String[]... inputs) {
        this.inputPane.prepareInputs(List.of(inputs));
    }

    protected void startKeyboard(Keyboard.Type type) {
        this.inputPane.start(type, false);
    }

    protected KeyTableConfig createKeyTableConfig() {
        return KeyTableConfig.from(this.inputPane.createKeyboardConfig());
    }

    protected PinyinKeyTable createPinyinKeyTable() {
        return PinyinKeyTable.create(createKeyTableConfig());
    }
}
