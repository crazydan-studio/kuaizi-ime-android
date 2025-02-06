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

/**
 * {@link Inputboard} 的视图
 * <p/>
 * 由 {@link InputListView} 以及相关的功能按钮组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-06
 */
public class InputboardView extends FrameLayout implements UserMsgListener, InputMsgListener {
    private InputListView inputListView;

    private View settingsBtnView;
    private View switchImeBtnView;
    private View inputListCleanBtnView;
    private View inputListCleanCancelBtnView;

    private boolean needToDisableInputListCleanBtn = true;
    private boolean needToDisableInputListCleanCancelBtn = true;

    private Config config;
    private UserMsgListener listener;

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

        this.inputListView.onMsg(msg);

        if (msg.type == InputMsgType.Keyboard_Start_Done) {
            toggleEnableSettingsBtn();
        }
        toggleEnableInputListCleanBtnByMsg(msg);
    }

    private void toggleEnableInputListCleanBtnByMsg(InputMsg msg) {
        boolean disableCleanBtn = msg.inputList.frozen || msg.inputList.empty;
        boolean disableCleanCancelBtn = msg.inputList.frozen || !msg.inputList.canCancelClean;

        if (this.needToDisableInputListCleanBtn != disableCleanBtn
            || this.needToDisableInputListCleanCancelBtn != disableCleanCancelBtn //
        ) {
            this.needToDisableInputListCleanBtn = disableCleanBtn;
            this.needToDisableInputListCleanCancelBtn = disableCleanCancelBtn;

            toggleEnableInputListCleanBtn();
        }
    }

    // =============================== End: 消息处理 ===================================

    // =============================== Start: 视图更新 ===================================

    /** 布局视图 */
    private void doLayout() {
        Keyboard.Theme theme = this.config.get(ConfigKey.theme);
        int themeResId = theme.getResId(getContext());

        View rootView = inflateWithTheme(R.layout.inputboard_root_view, themeResId);

        this.settingsBtnView = rootView.findViewById(R.id.settings);
        toggleEnableSettingsBtn();

        this.switchImeBtnView = rootView.findViewById(R.id.switch_ime);
        toggleEnableSwitchImeBtn();

        this.inputListCleanBtnView = rootView.findViewById(R.id.clean_input_list);
        this.inputListCleanCancelBtnView = rootView.findViewById(R.id.cancel_clean_input_list);
        toggleEnableInputListCleanBtn();

        this.inputListView = rootView.findViewById(R.id.input_list);
        this.inputListView.setListener(this);
    }

    private void toggleEnableSettingsBtn() {
        if (this.config.bool(ConfigKey.disable_settings_btn)) {
            this.settingsBtnView.setAlpha(0.4f);
            this.settingsBtnView.setOnClickListener(null);
        } else {
            this.settingsBtnView.setAlpha(1.0f);
            this.settingsBtnView.setOnClickListener(this::onShowPreferences);
        }
    }

    private void toggleEnableSwitchImeBtn() {
        if (this.config.bool(ConfigKey.disable_switch_ime_btn)) {
            this.switchImeBtnView.setAlpha(0.4f);
            this.switchImeBtnView.setOnClickListener(null);
        } else {
            this.switchImeBtnView.setAlpha(1.0f);
            this.switchImeBtnView.setOnClickListener(this::onSwitchIme);
        }
    }

    private void toggleEnableInputListCleanBtn() {
        if (this.needToDisableInputListCleanBtn) {
            ViewUtils.hide(this.inputListCleanBtnView);
            this.inputListCleanBtnView.setOnClickListener(null);

            if (this.needToDisableInputListCleanCancelBtn) {
                ViewUtils.hide(this.inputListCleanCancelBtnView);
                this.inputListCleanCancelBtnView.setOnClickListener(null);
            } else {
                ViewUtils.show(this.inputListCleanCancelBtnView);
                this.inputListCleanCancelBtnView.setOnClickListener(this::onCancelCleanInputList);
            }
        } else {
            ViewUtils.show(this.inputListCleanBtnView);
            this.inputListCleanBtnView.setOnClickListener(this::onCleanInputList);

            ViewUtils.hide(this.inputListCleanCancelBtnView);
            this.inputListCleanCancelBtnView.setOnClickListener(null);
        }
    }

    private <T extends View> T inflateWithTheme(int resId, int themeResId) {
        // 通过 Context Theme 仅对面板自身的视图设置主题样式，
        // 以避免通过 AppCompatDelegate.setDefaultNightMode 对配置等视图造成影响
        return ThemeUtils.inflate(this, resId, themeResId, true);
    }

    // =============================== End: 视图更新 ===================================

    // ==================== Start: 按键事件处理 ==================

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

    // ==================== End: 按键事件处理 ==================
}
