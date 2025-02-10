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
import android.os.Bundle;
import org.crazydan.studio.app.ime.kuaizi.IMEConfig;
import org.crazydan.studio.app.ime.kuaizi.IMEditor;
import org.crazydan.studio.app.ime.kuaizi.IMEditorView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigChangeListener;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTableConfig;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.KeyboardSwitchMsgData;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_Switch_Doing;

/**
 * 集成 筷字输入法 的窗口
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-17
 */
public abstract class ImeIntegratedActivity extends FollowSystemThemeActivity
        implements UserMsgListener, InputMsgListener, ConfigChangeListener {
    protected final Logger log = Logger.getLogger(getClass());

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
        this.imeConfig.setListener(this);

        this.imeConfig.set(ConfigKey.disable_settings_btn, true);
        this.imeConfig.set(ConfigKey.disable_switch_ime_btn, true);
        this.imeConfig.set(ConfigKey.disable_hide_keyboard_btn, true);

        this.ime = IMEditor.create(this.imeConfig.mutable());
        this.imeView = findViewById(R.id.input_pane_view);

        this.ime.setListener(this);
        this.imeView.setListener(this);
        this.imeView.setConfig(this.imeConfig.mutable());
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
    }

    // =============================== End: 消息处理 ===================================

    protected void prepareInputs(String[]... inputs) {
        this.ime.prepareInputs(List.of(inputs));
    }

    protected Keyboard.Type getKeyboardType() {
        return this.ime.getKeyboardType();
    }

    protected void startKeyboard(Keyboard.Type type) {
        startKeyboard(type, false);
    }

    protected void startKeyboard(Keyboard.Type type, boolean resetInputting) {
        Context context = getApplicationContext();
        this.ime.start(context, type, resetInputting);
    }

    /** 通过构造 {@link InputMsgType#Keyboard_Switch_Done} 消息以手动切换键盘类型 */
    protected void switchKeyboard(Keyboard.Type type) {
        KeyboardSwitchMsgData data = new KeyboardSwitchMsgData(null, type);
        this.ime.onMsg(InputMsg.build((b) -> b.type(Keyboard_Switch_Doing).data(data)));
    }

    protected KeyTableConfig createKeyTableConfig() {
        return this.ime.withKeyboardContext(KeyTableConfig::new);
    }

    protected PinyinKeyTable createPinyinKeyTable() {
        return PinyinKeyTable.create(createKeyTableConfig());
    }
}
