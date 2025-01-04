/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.ui.common;

import java.util.List;

import android.content.Context;
import android.os.Bundle;
import org.crazydan.studio.app.ime.kuaizi.IMEConfig;
import org.crazydan.studio.app.ime.kuaizi.IMEditor;
import org.crazydan.studio.app.ime.kuaizi.IMEditorView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigChangeListener;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTableConfig;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;

/**
 * 集成 筷字输入法 的窗口
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-17
 */
public abstract class ImeIntegratedActivity extends FollowSystemThemeActivity
        implements UserMsgListener, InputMsgListener, ConfigChangeListener {
    private final int layoutResId;

    protected IMEConfig imeConfig;
    protected IMEditor ime;
    protected IMEditorView imeView;

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
        this.imeConfig = IMEConfig.create(context);
        this.ime = IMEditor.create();
        this.imeView = findViewById(R.id.input_pane_view);

        this.ime.setConfig(this.imeConfig.mutable());
        this.imeView.setConfig(this.imeConfig.immutable());

        this.imeConfig.setListener(this);
        this.ime.setListener(this);
        this.imeView.setListener(this);
    }

    @Override
    protected void onDestroy() {
        this.ime.destroy();
        this.imeConfig.destroy();

        super.onDestroy();
    }

    // ===================== End: 生命周期 ==================

    // =============================== Start: 消息处理 ===================================

    @Override
    public void onChanged(ConfigKey key, Object oldValue, Object newValue) {
        this.ime.onChanged(key, oldValue, newValue);
    }

    @Override
    public void onMsg(UserInputMsg msg) {
        this.ime.onMsg(msg);
    }

    @Override
    public void onMsg(UserKeyMsg msg) {
        this.ime.onMsg(msg);
    }

    @Override
    public void onMsg(InputMsg msg) {
        this.imeView.onMsg(msg);
    }

    // =============================== End: 消息处理 ===================================

    protected void prepareInputs(String[]... inputs) {
        this.ime.prepareInputs(List.of(inputs));
    }

    protected void startKeyboard(Keyboard.Type type) {
        startKeyboard(type, false);
    }

    protected void startKeyboard(Keyboard.Type type, boolean resetInputting) {
        Context context = getApplicationContext();
        this.ime.start(context, type, resetInputting);
    }

    protected KeyTableConfig createKeyTableConfig() {
        return KeyTableConfig.from(this.ime.createKeyboardConfig());
    }

    protected PinyinKeyTable createPinyinKeyTable() {
        return PinyinKeyTable.create(createKeyTableConfig());
    }
}
