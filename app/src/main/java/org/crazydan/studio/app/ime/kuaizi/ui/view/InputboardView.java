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
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Inputboard;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Cancel_Clean_InputList;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Clean_InputList;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Hide_Keyboard;

/**
 * {@link Inputboard} 的视图
 * <p/>
 * 由 {@link InputListView} 以及相关的功能按钮组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-06
 */
public class InputboardView extends FrameLayout implements UserMsgListener, InputMsgListener {
    private View inputbarView;
    private InputListView inputListView;

    private View toolbarView;
    private View settingsBtnView;
    private View clipboardBtnView;

    private View showToolbarBtnView;
    private View hideToolbarBtnView;
    private View switchImeBtnView;
    private View hideKeyboardBtnView;

    private View inputListCleanBtnView;
    private View inputListCleanCancelBtnView;

    private Config config;
    private UserMsgListener listener;
    private State state = new State(State.Type.Init);

    public InputboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setConfig(Config config) {
        this.config = config;
        doLayout();
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
        } else if (msg.type == InputMsgType.Keyboard_Start_Done) {
            this.state = new State(State.Type.Init);
        } else {
            if (!msg.inputList.empty) {
                this.state = new State(State.Type.Input_Doing);
            } else if (msg.inputList.canCancelClean) {
                this.state = new State(State.Type.Input_Cleaned_Cancel_Waiting);
            } else {
                this.state = new State(State.Type.Init);
            }
        }

        updateToolsByState();
    }

    // =============================== End: 消息处理 ===================================

    // =============================== Start: 视图更新 ===================================

    /** 布局视图 */
    private void doLayout() {
        Keyboard.Theme theme = this.config.get(ConfigKey.theme);
        int themeResId = theme.getResId(getContext());

        View rootView = inflateWithTheme(R.layout.inputboard_root_view, themeResId);

        this.toolbarView = rootView.findViewById(R.id.toolbar);
        this.settingsBtnView = rootView.findViewById(R.id.tool_settings);
        this.clipboardBtnView = rootView.findViewById(R.id.tool_clipboard);

        this.showToolbarBtnView = rootView.findViewById(R.id.show_toolbar);
        this.hideToolbarBtnView = rootView.findViewById(R.id.hide_toolbar);
        this.switchImeBtnView = rootView.findViewById(R.id.tool_switch_ime);
        this.hideKeyboardBtnView = rootView.findViewById(R.id.tool_hide_keyboard);

        this.inputListCleanBtnView = rootView.findViewById(R.id.clean_input_list);
        this.inputListCleanCancelBtnView = rootView.findViewById(R.id.cancel_clean_input_list);

        this.inputbarView = rootView.findViewById(R.id.inputbar);
        this.inputListView = rootView.findViewById(R.id.input_list);
        this.inputListView.setListener(this);

        updateToolsByState();
    }

    private <T extends View> T inflateWithTheme(int resId, int themeResId) {
        // 通过 Context Theme 仅对面板自身的视图设置主题样式，
        // 以避免通过 AppCompatDelegate.setDefaultNightMode 对配置等视图造成影响
        return ThemeUtils.inflate(this, resId, themeResId, true);
    }

    /** 根据输入面板状态更新工具状态 */
    private void updateToolsByState() {
        switch (this.state.type) {
            case Init: {
                ViewUtils.visible(this.inputbarView, false);
                ViewUtils.visible(this.toolbarView, true);

                toggleShowBtn(this.switchImeBtnView, true, this::onSwitchIme);
                toggleShowBtn(this.showToolbarBtnView, false, this::onShowToolbar);

                toggleShowBtn(this.hideToolbarBtnView, false, this::onHideToolbar);
                toggleShowBtn(this.hideKeyboardBtnView, true, this::onHideKeyboard);
                toggleShowBtn(this.inputListCleanBtnView, false, this::onCleanInputList);
                toggleShowBtn(this.inputListCleanCancelBtnView, false, this::onCancelCleanInputList);
                break;
            }
            case Input_Freeze_Doing: {
                ViewUtils.visible(this.inputbarView, true);
                ViewUtils.visible(this.toolbarView, false);

                toggleShowBtn(this.switchImeBtnView, true, this::onSwitchIme);
                toggleShowBtn(this.showToolbarBtnView, false, this::onShowToolbar);

                toggleShowBtn(this.hideToolbarBtnView, false, this::onHideToolbar);
                toggleShowBtn(this.hideKeyboardBtnView, true, this::onHideKeyboard);
                toggleShowBtn(this.inputListCleanBtnView, false, this::onCleanInputList);
                toggleShowBtn(this.inputListCleanCancelBtnView, false, this::onCancelCleanInputList);
                break;
            }
            case Input_Doing: {
                ViewUtils.visible(this.inputbarView, true);
                ViewUtils.visible(this.toolbarView, false);

                toggleShowBtn(this.switchImeBtnView, false, this::onSwitchIme);
                toggleShowBtn(this.showToolbarBtnView, true, this::onShowToolbar);

                toggleShowBtn(this.hideToolbarBtnView, false, this::onHideToolbar);
                toggleShowBtn(this.hideKeyboardBtnView, false, this::onHideKeyboard);
                toggleShowBtn(this.inputListCleanBtnView, true, this::onCleanInputList);
                toggleShowBtn(this.inputListCleanCancelBtnView, false, this::onCancelCleanInputList);
                break;
            }
            case Input_Cleaned_Cancel_Waiting: {
                ViewUtils.visible(this.inputbarView, true);
                ViewUtils.visible(this.toolbarView, false);

                toggleShowBtn(this.switchImeBtnView, false, this::onSwitchIme);
                toggleShowBtn(this.showToolbarBtnView, true, this::onShowToolbar);

                toggleShowBtn(this.hideToolbarBtnView, false, this::onHideToolbar);
                toggleShowBtn(this.hideKeyboardBtnView, false, this::onHideKeyboard);
                toggleShowBtn(this.inputListCleanBtnView, false, this::onCleanInputList);
                toggleShowBtn(this.inputListCleanCancelBtnView, true, this::onCancelCleanInputList);
                break;
            }
            case Toolbar_Show_Doing: {
                ViewUtils.visible(this.inputbarView, false);
                ViewUtils.visible(this.toolbarView, true);

                toggleShowBtn(this.switchImeBtnView, false, this::onSwitchIme);
                toggleShowBtn(this.showToolbarBtnView, false, this::onShowToolbar);

                toggleShowBtn(this.hideToolbarBtnView, true, this::onHideToolbar);
                toggleShowBtn(this.hideKeyboardBtnView, false, this::onHideKeyboard);
                toggleShowBtn(this.inputListCleanBtnView, false, this::onCleanInputList);
                toggleShowBtn(this.inputListCleanCancelBtnView, false, this::onCancelCleanInputList);
                break;
            }
        }

        toggleDisableBtn(this.switchImeBtnView, this.config.bool(ConfigKey.disable_switch_ime_btn), this::onSwitchIme);
        toggleDisableBtn(this.hideKeyboardBtnView,
                         this.config.bool(ConfigKey.disable_hide_keyboard_btn),
                         this::onHideKeyboard);

        toggleDisableBtn(this.clipboardBtnView, false, this::onShowClipboard);
        toggleDisableBtn(this.settingsBtnView,
                         this.config.bool(ConfigKey.disable_settings_btn),
                         this::onShowPreferences);

        Keyboard.HandMode handMode = this.config.get(ConfigKey.hand_mode);
        switch (handMode) {
            case left: {
                this.toolbarView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                break;
            }
            case right: {
                this.toolbarView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                break;
            }
        }
    }

    private void toggleDisableBtn(View btn, boolean disabled, View.OnClickListener listener) {
        if (disabled) {
            btn.setAlpha(0.4f);
            btn.setOnClickListener(null);
        } else {
            btn.setAlpha(1.0f);
            btn.setOnClickListener(listener);
        }
    }

    private void toggleShowBtn(View btn, boolean shown, View.OnClickListener listener) {
        if (shown) {
            ViewUtils.show(btn);
            btn.setOnClickListener(listener);
        } else {
            ViewUtils.hide(btn);
            btn.setOnClickListener(null);
        }
    }

    // =============================== End: 视图更新 ===================================

    // ==================== Start: 按键事件处理 ==================

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

    private void onHideKeyboard(View v) {
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Hide_Keyboard));
        onMsg(msg);
    }

    private void onShowClipboard(View v) {
        // TODO 显示剪贴板
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
}
