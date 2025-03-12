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

package org.crazydan.studio.app.ime.kuaizi;

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ObjectUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.AudioPlayer;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.ConfigUpdateMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputAudioPlayMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputCharsInputPopupShowMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.KeyboardHandModeSwitchMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputQuickView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputboardView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.KeyboardView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.key.XPadKeyViewHolder;
import org.crazydan.studio.app.ime.kuaizi.ui.view.xpad.XPadView;

/**
 * {@link IMEditor} 的视图
 * <p/>
 * 由 {@link KeyboardView} 和 {@link InputboardView} 组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class IMEditorView extends FrameLayout implements UserMsgListener, InputMsgListener {
    protected final Logger log = Logger.getLogger(getClass());

    private final AudioPlayer audioPlayer;

    private KeyboardView keyboardView;
    private InputboardView inputboardView;
    private TextView keyboardWarningView;

    private View popupAnchor;
    private PopupWindow inputKeyPopupWindow;
    private PopupWindow inputQuickPopupWindow;

    private boolean needToAddBottomSpacing;

    private Config.Mutable config;
    private UserMsgListener listener;

    public IMEditorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.audioPlayer = new AudioPlayer();
        this.audioPlayer.load(getContext(),
                              R.raw.tick_single,
                              R.raw.tick_double,
                              R.raw.page_flip,
                              R.raw.tick_clock,
                              R.raw.tick_ping);
    }

    public void setConfig(Config.Mutable config) {
        this.config = config;
        updateLayout();
    }

    public XPadView getXPadView() {
        XPadKeyViewHolder holder = this.keyboardView.getXPadKeyViewHolder();
        return holder != null ? holder.getXPad() : null;
    }

    public void close() {
        showInputQuickPopupWindow(null);
        showInputKeyPopupWindow(null, false);
    }

    // =============================== Start: 消息处理 ===================================

    public void setListener(UserMsgListener listener) {
        this.listener = listener;
    }

    /** 响应内部视图的 {@link UserKeyMsg} 消息：从视图向上传递给外部监听者 */
    @Override
    public void onMsg(UserKeyMsg msg) {
        this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                msg.getClass(), this.listener.getClass()
        });

        this.listener.onMsg(msg);

        this.log.endTreeLog();
    }

    /** 响应内部视图的 {@link UserInputMsg} 消息：从视图向上传递给外部监听者 */
    @Override
    public void onMsg(UserInputMsg msg) {
        this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                msg.getClass(), this.listener.getClass()
        });

        this.listener.onMsg(msg);

        this.log.endTreeLog();
    }

    // -------------------------------------------

    /** 响应 {@link InputMsg} 消息：向下传递消息给内部视图 */
    @Override
    public void onMsg(InputMsg msg) {
        this.log.beginTreeLog("Handle %s", () -> new Object[] { msg.getClass() }) //
                .debug("Message Type: %s", () -> new Object[] { msg.type }) //
                .debug("Message Data: %s", () -> new Object[] { msg.data() });

        handleMsg(msg);

        this.log.endTreeLog();
        //////////////////////////////////////////////////////////////
        this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                msg.getClass(), this.keyboardView.getClass()
        });

        // Note: 涉及重建视图的情况，因此，需在最后转发消息到子视图
        this.keyboardView.onMsg(msg);

        this.log.endTreeLog();
        ////////////////////////////////////////////////////////////
        this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                msg.getClass(), this.inputboardView.getClass()
        });

        this.inputboardView.onMsg(msg);

        this.log.endTreeLog();
    }

    private void handleMsg(InputMsg msg) {
        // Note: 快捷输入没有确定的隐藏时机，故而，需针对每个消息做一次处理，在数据为 null 时隐藏，有数据时显示
        showInputQuickPopupWindow(msg.inputQuickList);

        switch (msg.type) {
            case Keyboard_Start_Doing: {
                toggleShowKeyboardWarning(true);
                break;
            }
            case Keyboard_Start_Done: {
                toggleShowKeyboardWarning(false);
                // Note: 键盘启动时，可能涉及横竖屏的转换，故而，需做一次更新
                updateBottomSpacing(false);
                break;
            }
            case Keyboard_HandMode_Switch_Done: {
                KeyboardHandModeSwitchMsgData data = msg.data();
                this.config.set(ConfigKey.hand_mode, data.mode);

                updatePopupViewLayout(this.inputQuickPopupWindow.getContentView());
                break;
            }
            case Config_Update_Done: {
                on_Config_Update_Done_Msg(msg.data());
                break;
            }
            case InputAudio_Play_Doing: {
                on_InputAudio_Play_Doing_Msg(msg.data());
                break;
            }
            case InputChars_Input_Popup_Show_Doing: {
                InputCharsInputPopupShowMsgData data = msg.data();

                showInputKeyPopupWindow(data.text, data.hideDelayed);
                break;
            }
            case InputChars_Input_Popup_Hide_Doing: {
                showInputKeyPopupWindow(null, false);
                break;
            }
            default: {
                this.log.warn("Ignore message %s", () -> new Object[] { msg.type });
            }
        }
    }

    private void on_Config_Update_Done_Msg(ConfigUpdateMsgData data) {
        switch (data.configKey) {
            case theme: {
                updateLayout();
                break;
            }
            case enable_x_input_pad:
            case adapt_desktop_swipe_up_gesture: {
                updateBottomSpacing(false);
                break;
            }
        }
    }

    private void on_InputAudio_Play_Doing_Msg(InputAudioPlayMsgData data) {
        if (data.audioType == InputAudioPlayMsgData.AudioType.PageFlip) {
            if (this.config.bool(ConfigKey.disable_input_candidates_paging_audio)) {
                return;
            }
        } else if (this.config.bool(ConfigKey.disable_key_clicked_audio)) {
            return;
        }

        this.audioPlayer.play(data.audioType.resId);
    }

    // =============================== End: 消息处理 ===================================

    // =============================== Start: 视图更新 ===================================

    /** 更新布局 */
    private void updateLayout() {
        // 必须先清除已有的子视图，否则，重复 inflate 会无法即时生效
        removeAllViews();

        Keyboard.Theme theme = this.config.get(ConfigKey.theme);
        int themeResId = theme.getResId(getContext());

        View rootView = inflateWithTheme(R.layout.ime_root_view, themeResId, true);

        this.keyboardWarningView = rootView.findViewById(R.id.keyboard_warning);
        this.keyboardView = rootView.findViewById(R.id.keyboard);
        this.keyboardView.setConfig(this.config);
        this.keyboardView.setListener(this);
        updateBottomSpacing(true);

        this.inputboardView = rootView.findViewById(R.id.inputboard);
        this.inputboardView.setConfig(this.config);
        this.inputboardView.setListener(this);

        // <<<<<<<<<< 气泡提示
        this.popupAnchor = rootView.findViewById(R.id.popup_anchor);

        View inputKeyView = inflateWithTheme(R.layout.popup_input_key_view, themeResId, false);
        this.inputKeyPopupWindow = preparePopupWindow(this.inputKeyPopupWindow, inputKeyView);

        InputQuickView inputQuickView = inflateWithTheme(R.layout.popup_input_quick_view, themeResId, false);
        inputQuickView.setListener(this);
        updatePopupViewLayout(inputQuickView);
        this.inputQuickPopupWindow = preparePopupWindow(this.inputQuickPopupWindow, inputQuickView);
        // >>>>>>>>>>>
    }

    private void updateBottomSpacing(boolean force) {
        // Note: 仅竖屏模式下需要添加底部空白
        boolean needSpacing = this.config.bool(ConfigKey.adapt_desktop_swipe_up_gesture)
                              && !this.config.bool(ConfigKey.enable_x_input_pad)
                              && this.config.get(ConfigKey.orientation) == Keyboard.Orientation.portrait;

        if (!force && this.needToAddBottomSpacing == needSpacing) {
            return;
        }
        this.needToAddBottomSpacing = needSpacing;

        float height = ScreenUtils.pxFromDimension(getContext(), R.dimen.keyboard_bottom_spacing);
        height -= this.keyboardView.getBottomSpacing();

        View bottomSpacingView = this.findViewById(R.id.bottom_spacing_view);
        if (needSpacing && height > 0) {
            ViewUtils.show(bottomSpacingView);
            ViewUtils.setHeight(bottomSpacingView, (int) height);
        } else {
            ViewUtils.hide(bottomSpacingView);
        }
    }

    private void toggleShowKeyboardWarning(boolean shown) {
        ViewUtils.visible(this.keyboardView, !shown);
        ViewUtils.visible(this.keyboardWarningView, shown);
    }

    private <T extends View> T inflateWithTheme(int resId, int themeResId, boolean attachToRoot) {
        // 通过 Context Theme 仅对键盘自身的视图设置主题样式，
        // 以避免通过 AppCompatDelegate.setDefaultNightMode 对配置等视图造成影响
        return ThemeUtils.inflate(this, resId, themeResId, attachToRoot);
    }

    // =============================== End: 视图更新 ===================================

    // ==================== Start: 气泡提示 ==================

    private void showInputQuickPopupWindow(List<?> dataList) {
        PopupWindow window = this.inputQuickPopupWindow;
        if (this.config.bool(ConfigKey.disable_input_clip_popup_tips) //
            || CollectionUtils.isEmpty(dataList) //
        ) {
            post(window::dismiss);
            return;
        }

        InputQuickView inputQuickView = (InputQuickView) window.getContentView();
        inputQuickView.update(dataList);

        showPopupWindow(window);
    }

    private void showInputKeyPopupWindow(String key, boolean hideDelayed) {
        PopupWindow window = this.inputKeyPopupWindow;
        if (this.config.bool(ConfigKey.disable_input_key_popup_tips) //
            || CharUtils.isBlank(key) //
        ) {
            // Note: 存在因滑动太快而无法隐藏的问题，故而，延迟隐藏
            post(window::dismiss);
            return;
        }

        View contentView = window.getContentView();
        TextView textView = contentView.findViewById(R.id.fg_view);
        textView.setText(key);

        showPopupWindow(window);

        if (hideDelayed) {
            postDelayed(window::dismiss, 600);
        }
    }

    private PopupWindow preparePopupWindow(PopupWindow window, View contentView) {
        // Note: 重建窗口，以便于更新主题样式
        ObjectUtils.invokeWhenNonNull(window, PopupWindow::dismiss);

        window = new PopupWindow(contentView,
                                 WindowManager.LayoutParams.MATCH_PARENT,
                                 WindowManager.LayoutParams.WRAP_CONTENT);

        window.setClippingEnabled(false);
        window.setBackgroundDrawable(null);
        window.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);

        window.setAnimationStyle(R.style.Theme_Kuaizi_PopupWindow_Animation);

        return window;
    }

    private void showPopupWindow(PopupWindow window) {
        // Note: 初始启动时，测量内容尺寸将返回 0，故而，需在视图渲染完毕后，再取值
        post(() -> {
            // 测量内容高度
            View contentView = window.getContentView();
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

            int height = contentView.getMeasuredHeight();

            // 放置于被布局的键盘之上
            View parent = this.popupAnchor;
            int[] location = new int[2];
            parent.getLocationInWindow(location);

            int x = location[0];
            int y = location[1] - height;

            // 设置初始显示位置：其仅在未显示时有效
            window.showAtLocation(this.popupAnchor, Gravity.START | Gravity.TOP, x, y);

            // 确保窗口按照内容高度调整位置：其仅在显示时有效
            // Note: 需要强制更新，否则，内容布局会出现跳动
            window.update(x, y, window.getWidth(), window.getHeight(), true);
        });
    }

    private void updatePopupViewLayout(View view) {
        if (view == null) {
            return;
        }

        Keyboard.HandMode handMode = this.config.get(ConfigKey.hand_mode);
        switch (handMode) {
            case left: {
                handMode = Keyboard.HandMode.right;
                break;
            }
            case right: {
                handMode = Keyboard.HandMode.left;
                break;
            }
        }
        ViewUtils.updateLayoutDirection(view, handMode);
    }

    // ==================== End: 气泡提示 ==================
}
