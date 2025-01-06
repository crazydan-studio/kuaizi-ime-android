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
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.ConfigUpdateMsgData;

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
        switch (msg.type) {
            case Keyboard_Start_Done: {
                toggleEnableSettingsBtn();
                break;
            }
            case Config_Update_Done: {
                ConfigUpdateMsgData data = msg.data();
                if (data.key == ConfigKey.theme) {
                    doLayout();
                }
                break;
            }
        }

        toggleEnableInputListCleanBtnByMsg(msg);

        // Note: 涉及重建视图的情况，因此，需在最后转发消息到子视图
        this.inputListView.onMsg(msg);
    }

    private void toggleEnableInputListCleanBtnByMsg(InputMsg msg) {
        boolean disableCleanBtn = msg.inputList.empty;
        boolean disableCleanCancelBtn = !msg.inputList.canCancelClean;

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
        // 必须先清除已有的子视图，否则，重复 inflate 会无法即时生效
        removeAllViews();

        Keyboard.Theme theme = this.config.get(ConfigKey.theme);
        int themeResId = theme.getResId(getContext());

        View rootView = inflateWithTheme(R.layout.inputboard_view_layout, themeResId);

        this.settingsBtnView = rootView.findViewById(R.id.settings);
        toggleEnableSettingsBtn();

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

    private void toggleEnableInputListCleanBtn() {
        if (this.needToDisableInputListCleanBtn) {
            if (!this.needToDisableInputListCleanCancelBtn) {
                ViewUtils.hide(this.inputListCleanBtnView);
                ViewUtils.show(this.inputListCleanCancelBtnView);
            } else {
                ViewUtils.show(this.inputListCleanBtnView).setAlpha(0);
                ViewUtils.hide(this.inputListCleanCancelBtnView);
            }

            this.inputListCleanBtnView.setOnClickListener(null);
            this.inputListCleanCancelBtnView.setOnClickListener(this::onCancelCleanInputList);
        } else {
            ViewUtils.show(this.inputListCleanBtnView).setAlpha(1);
            ViewUtils.hide(this.inputListCleanCancelBtnView);

            this.inputListCleanBtnView.setOnClickListener(this::onCleanInputList);
            this.inputListCleanCancelBtnView.setOnClickListener(null);
        }
    }

    private <T extends View> T inflateWithTheme(int resId, int themeResId) {
        // 通过 Context Theme 仅对键盘自身的视图设置主题样式，
        // 以避免通过 AppCompatDelegate.setDefaultNightMode 对配置等视图造成影响
        return ThemeUtils.inflate(this, resId, themeResId, true);
    }

    // =============================== End: 视图更新 ===================================

    // ==================== Start: 按键事件处理 ==================

    private void onShowPreferences(View v) {
        SystemUtils.showAppPreferences(getContext());
    }

    private void onCleanInputList(View v) {
        UserInputMsg msg = new UserInputMsg(UserInputMsgType.SingleTap_Btn_Clean_InputList);
        onMsg(msg);
    }

    private void onCancelCleanInputList(View v) {
        UserInputMsg msg = new UserInputMsg(UserInputMsgType.SingleTap_Btn_Cancel_Clean_InputList);
        onMsg(msg);
    }

    // ==================== End: 按键事件处理 ==================
}
