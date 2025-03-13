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

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Inputboard;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Cancel_Clean_InputList;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Clean_InputList;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Close_Keyboard;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Show_Clipboard;

/**
 * {@link Inputboard} 的视图
 * <p/>
 * 由 {@link InputListView} 以及相关的功能按钮组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-06
 */
public class InputboardView extends LinearLayout implements UserMsgListener, InputMsgListener {
    private final InputListView inputListView;

    private final BtnTools tools = new BtnTools();

    private Config config;
    private UserMsgListener listener;

    private State state = new State(State.Type.Init);

    public InputboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.ime_board_input_view, this);

        this.inputListView = findViewById(R.id.input_list);
        this.inputListView.setListener(this);

        this.tools.toolbar = new BtnGroup(this, R.id.toolbar);
        this.tools.inputbar = new BtnGroup(this, R.id.inputbar);

        this.tools.showToolbar = new Btn(this, R.id.show_toolbar, this::onShowToolbar);
        this.tools.hideToolbar = new Btn(this, R.id.hide_toolbar, this::onHideToolbar);

        this.tools.settings = new Btn(this, R.id.tool_settings, this::onShowPreferences);
        this.tools.clipboard = new Btn(this, R.id.tool_clipboard, this::onShowClipboard);
        this.tools.switchIme = new Btn(this, R.id.tool_switch_ime, this::onSwitchIme);
        this.tools.closeKeyboard = new Btn(this, R.id.tool_close_keyboard, this::onCloseKeyboard);

        this.tools.cleanInputList = new Btn(this, R.id.clean_input_list, this::onCleanInputList);
        this.tools.cancelCleanInputList = new Btn(this, R.id.cancel_clean_input_list, this::onCancelCleanInputList);
    }

    public void setConfig(Config config) {
        this.config = config;

        updateToolsByState();
    }

    // =============================== Start: 消息处理 ===================================

    public void setListener(UserMsgListener listener) {
        this.listener = listener;
    }

    /** 响应内部视图的 {@link UserInputMsg} 消息：从视图向上传递给外部监听者 */
    @Override
    public void onMsg(UserInputMsg msg) {
        this.listener.onMsg(msg);
    }

    // -------------------------------------------

    /** 响应 {@link InputMsg} 消息：向下传递消息给内部视图 */
    @Override
    public void onMsg(InputMsg msg) {
        // Note: 本视图为上层视图的子视图，在主题样式更新时，上层视图会自动重建本视图，
        // 因此，不需要重复处理本视图的布局更新等问题

        handleMsg(msg);

        this.inputListView.onMsg(msg);
    }

    private void handleMsg(InputMsg msg) {
        if (msg.inputList.frozen) {
            this.state = new State(State.Type.Input_Freeze_Doing);
        } else if (!msg.inputList.empty) {
            this.state = new State(State.Type.Input_Doing);
        } else if (msg.inputList.canCancelClean) {
            this.state = new State(State.Type.Input_Cleaned_Cancel_Waiting);
        } else {
            this.state = new State(State.Type.Init);
        }

        updateToolsByState();
    }

    /** 根据输入面板状态更新工具状态 */
    private void updateToolsByState() {
        this.tools.reset();

        this.tools.clipboard.disabled = false;
        this.tools.settings.disabled = this.config.bool(ConfigKey.disable_settings_btn);
        this.tools.switchIme.disabled = this.config.bool(ConfigKey.disable_switch_ime_btn);
        this.tools.closeKeyboard.disabled = this.config.bool(ConfigKey.disable_close_keyboard_btn);

        switch (this.state.type) {
            case Init: {
                this.tools.toolbar.shown = true;

                this.tools.settings.shown = true;
                this.tools.closeKeyboard.shown = true;
                break;
            }
            case Input_Freeze_Doing: {
                this.tools.inputbar.shown = true;

                this.tools.settings.shown = true;
                this.tools.closeKeyboard.shown = true;
                break;
            }
            case Input_Doing: {
                this.tools.inputbar.shown = true;

                this.tools.showToolbar.shown = true;
                this.tools.cleanInputList.shown = true;
                break;
            }
            case Input_Cleaned_Cancel_Waiting: {
                this.tools.inputbar.shown = true;

                this.tools.showToolbar.shown = true;
                this.tools.cancelCleanInputList.shown = true;
                break;
            }
            case Toolbar_Show_Doing: {
                this.tools.toolbar.shown = true;

                this.tools.hideToolbar.shown = true;
                break;
            }
        }

        this.tools.update();

        Keyboard.HandMode handMode = this.config.get(ConfigKey.hand_mode);
        ViewUtils.updateLayoutDirection(this, handMode);
    }

    // =============================== End: 消息处理 ===================================

    // ==================== Start: 按键事件处理 ==================

    private static void toggleDisableBtn(View btn, boolean disabled, View.OnClickListener listener) {
        if (disabled) {
            btn.setAlpha(0.4f);
            btn.setOnClickListener(null);
        } else {
            btn.setAlpha(1.0f);
            btn.setOnClickListener(listener);
        }
    }

    private static void toggleShowBtn(View btn, boolean shown, View.OnClickListener listener) {
        if (shown) {
            ViewUtils.show(btn);
            btn.setOnClickListener(listener);
        } else {
            ViewUtils.hide(btn);
            btn.setOnClickListener(null);
        }
    }

    private void onShowToolbar(View v) {
        this.state = new State(State.Type.Toolbar_Show_Doing, this.state);
        updateToolsByState();
    }

    private void onHideToolbar(View v) {
        this.state = this.state.prev;
        updateToolsByState();
    }

    private void onShowPreferences(View v) {
        SystemUtils.showAppPreferences(getContext());
    }

    private void onSwitchIme(View v) {
        SystemUtils.switchIme(getContext());
    }

    private void onCleanInputList(View v) {
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Clean_InputList));
        onMsg(msg);
    }

    private void onCancelCleanInputList(View v) {
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Cancel_Clean_InputList));
        onMsg(msg);
    }

    private void onCloseKeyboard(View v) {
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Close_Keyboard));
        onMsg(msg);
    }

    private void onShowClipboard(View v) {
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Show_Clipboard));
        onMsg(msg);
    }

    // ==================== End: 按键事件处理 ==================

    private static class State {
        public final Type type;
        public final State prev;

        State(Type type) {
            this(type, null);
        }

        State(Type type, State prev) {
            this.type = type;
            this.prev = prev;
        }

        enum Type {
            /** 初始 */
            Init,
            /** 输入冻结中 */
            Input_Freeze_Doing,
            /** 输入中 */
            Input_Doing,
            /** 等待撤销对输入的清空操作 */
            Input_Cleaned_Cancel_Waiting,
            /** 工具栏显示中 */
            Toolbar_Show_Doing,
        }
    }

    private static class BtnTools {
        public BtnGroup inputbar;
        public Btn settings;
        public Btn showToolbar;

        public Btn hideToolbar;
        public Btn closeKeyboard;

        public Btn cleanInputList;
        public Btn cancelCleanInputList;

        public BtnGroup toolbar;
        public Btn switchIme;
        public Btn clipboard;

        private Btn[] getBtnInToolbar() {
            return new Btn[] { this.switchIme, this.clipboard };
        }

        private Btn[] getBtnInInputbar() {
            return new Btn[] {
                    this.settings,
                    this.showToolbar,
                    this.hideToolbar,
                    this.closeKeyboard,
                    this.cleanInputList,
                    this.cancelCleanInputList
            };
        }

        public void reset() {
            this.inputbar.shown = false;
            this.toolbar.shown = false;

            // 工具栏中的按钮：默认均显示且启用
            for (Btn btn : getBtnInToolbar()) {
                btn.shown = true;
                btn.disabled = false;
            }

            // 其他按钮：默认均不显示且禁用
            for (Btn btn : getBtnInInputbar()) {
                btn.shown = false;
                btn.disabled = false;
            }
        }

        public void update() {
            ViewUtils.visible(this.inputbar.view, this.inputbar.shown);
            ViewUtils.visible(this.toolbar.view, this.toolbar.shown);

            for (Btn btn : getBtnInToolbar()) {
                toggleShowBtn(btn.view, btn.shown, btn.listener);
                toggleDisableBtn(btn.view, btn.disabled, btn.listener);
            }
            for (Btn btn : getBtnInInputbar()) {
                toggleShowBtn(btn.view, btn.shown, btn.listener);
                toggleDisableBtn(btn.view, btn.disabled, btn.listener);
            }
        }
    }

    private static class BtnGroup {
        public final View view;
        public boolean shown;

        BtnGroup(View rootView, int viewId) {
            this.view = rootView.findViewById(viewId);
        }
    }

    private static class Btn {
        public final View view;
        public final View.OnClickListener listener;

        public boolean disabled;
        public boolean shown;

        Btn(View rootView, int viewId, OnClickListener listener) {
            this.view = rootView.findViewById(viewId);
            this.listener = listener;
        }
    }
}
