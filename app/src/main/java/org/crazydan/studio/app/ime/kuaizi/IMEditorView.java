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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ObjectUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.AudioPlayer;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.common.widget.Toast;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewClosable;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputClip;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.ConfigUpdateMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputAudioPlayMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputCharsInputPopupShowMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputClipMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputClipMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.FavoriteboardView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputQuickListView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.MainboardView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.xpad.XPadView;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Save_As_Favorite;

/**
 * {@link IMEditor} 的视图
 * <p/>
 * 由 {@link MainboardView}、{@link FavoriteboardView} 等组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-13
 */
public class IMEditorView extends FrameLayout implements UserMsgListener, InputMsgListener {
    protected final Logger log = Logger.getLogger(getClass());

    private final AudioPlayer audioPlayer;

    private Config config;
    private UserMsgListener listener;

    private Animation enterAnim;
    private View popupAnchor;
    private PopupWindow tipPopupWindow;
    private PopupWindow inputQuickPopupWindow;

    private BoardType activeBoard = BoardType.main;
    private Map<BoardType, View> boards;

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

    public void setConfig(Config config) {
        this.config = config;
        doLayout();
    }

    public XPadView getXPadView() {
        MainboardView mainboard = getBoard(BoardType.main);
        return mainboard.getXPadView();
    }

    public void close() {
        MainboardView mainboard = getBoard(BoardType.main);
        mainboard.close();

        showInputQuickPopupWindow(null);
        showTipPopupWindow(null, false);
    }

    // =============================== Start: 视图更新 ===================================

    /** 布局视图 */
    private void doLayout() {
        // 必须先清除已有的子视图，否则，重复 inflate 会无法即时生效
        removeAllViews();

        Context context = getContext();
        Keyboard.Theme theme = this.config.get(ConfigKey.theme);
        int themeResId = theme.getResId(context);

        // 通过 Context Theme 仅对键盘自身的视图设置主题样式，
        // 以避免通过 AppCompatDelegate.setDefaultNightMode 对配置等视图造成影响
        // Note:
        // - 其内部的视图也会按照主题样式进行更新，无需在子视图中单独处理
        // - 所布局的视图将作为当前视图的子视图插入，而不会替换当前视图
        ThemeUtils.inflate(this, R.layout.ime_root_view, themeResId, true);

        int[] animAttrs = new int[] { android.R.attr.windowEnterAnimation };
        int[] animResIds = ThemeUtils.getStyledAttrs(context, R.style.Theme_Kuaizi_PopupWindow_Animation, animAttrs);
        int enterAnimResId = animResIds[0];

        this.enterAnim = AnimationUtils.loadAnimation(context, enterAnimResId);

        MainboardView mainboardView = findViewById(R.id.mainboard);
        mainboardView.setConfig(this.config);
        mainboardView.setListener(this);

        FavoriteboardView favoriteboardView = findViewById(R.id.favoriteboard);
        favoriteboardView.setConfig(this.config);
        favoriteboardView.setListener(this);

        // <<<<<<<<<<<< 面板管理
        this.boards = new HashMap<BoardType, View>() {{
            put(BoardType.main, mainboardView);
            put(BoardType.favorite, favoriteboardView);
        }};

        // 确保按已激活的类型显隐面板，而不是按视图中的设置
        activeBoard(this.activeBoard);
        // >>>>>>>>>>>>

        // <<<<<<<<<< 气泡提示
        this.popupAnchor = findViewById(R.id.popup_anchor);

        View inputKeyView = inflate(context, R.layout.popup_input_key_view, null);
        this.tipPopupWindow = preparePopupWindow(this.tipPopupWindow, inputKeyView);

        InputQuickListView inputQuickListView = (InputQuickListView) inflate(context,
                                                                             R.layout.popup_input_quick_list_view,
                                                                             null);
        inputQuickListView.setListener(this);

        this.inputQuickPopupWindow = preparePopupWindow(this.inputQuickPopupWindow, inputQuickListView);

        updateReverseLayoutDirection(this.inputQuickPopupWindow.getContentView());
        // >>>>>>>>>>>
    }

    // =============================== End: 视图更新 ===================================

    // =============================== Start: 消息处理 ===================================

    public void setListener(UserMsgListener listener) {
        this.listener = listener;
    }

    /** 响应内部视图的 {@link UserKeyMsg} 消息：从视图向上传递给外部监听者 */
    @Override
    public void onMsg(UserKeyMsg msg) {
        this.listener.onMsg(msg);
    }

    /** 响应内部视图的 {@link UserInputMsg} 消息：从视图向上传递给外部监听者 */
    @Override
    public void onMsg(UserInputMsg msg) {
        this.listener.onMsg(msg);

        // 直接处理面板关闭消息：关闭无需清理等工作
        switch (msg.type) {
            case SingleTap_Btn_Close_Favoriteboard: {
                activeBoard(BoardType.main);
                break;
            }
        }
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

        // Note: 涉及重建视图和视图显隐切换等情况，因此，需在最后转发消息到子视图
        InputMsgListener current = currentBoard();
        current.onMsg(msg);
    }

    private void handleMsg(InputMsg msg) {
        // Note: 快捷输入没有确定的隐藏时机，故而，需针对每个消息做一次处理，在数据为 null 时隐藏，有数据时显示
        showInputQuickPopupWindow(msg.inputQuickList);

        switch (msg.type) {
            case Keyboard_Start_Done: {
                // 确保键盘启动后，始终在主面板上，
                // 从而保证可粘贴提示等在主面板上的弹窗能够正常显示
                activeBoard(BoardType.main);
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
            case Keyboard_HandMode_Switch_Done: {
                updateReverseLayoutDirection(this.inputQuickPopupWindow.getContentView());
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
            case InputFavorite_Save_Done: {
                showTipPopupWindow(EditorAction.favorite.tipResId);
                break;
            }
            case InputFavorite_Paste_Done: {
                showTipPopupWindow(EditorAction.paste.tipResId);
                break;
            }
            case Editor_Edit_Doing: {
                EditorEditMsgData data = msg.data();
                on_Editor_Edit_Doing_Msg(data.action);
                break;
            }
            case InputFavorite_Be_Ready: {
                activeBoard(BoardType.favorite);
                break;
            }
            case InputClip_CanBe_Favorite: {
                on_InputClip_CanBe_Favorite_Msg(msg.data());
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
                // 主题变更，必须重建视图
                doLayout();
                break;
            }
            // 视图相关的配置变更，需要更新视图
            case hand_mode:
            case enable_x_input_pad:
            case adapt_desktop_swipe_up_gesture: {
                activeBoard(this.activeBoard);
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

    private void on_InputClip_CanBe_Favorite_Msg(InputClipMsgData data) {
        Toast.with(this)
             .setText(data.source.confirmResId)
             .setDuration(5000)
             .setAction(R.string.btn_save_as_favorite, (v) -> saveClipToFavorite(data.clip))
             .show();
    }

    private void on_Editor_Edit_Doing_Msg(EditorAction action) {
        // 对编辑内容的操作做气泡提示，以告知用户处理结果，避免静默处理造成的困惑
        // Note: 复制和粘贴可能内容为空，不会提示可收藏，因此，仍需提示
        switch (action) {
            case backspace: // 已作气泡提示
            case favorite: { // 有专门的收藏确认提示
                break;
            }
            default: {
                showTipPopupWindow(action.tipResId);
                break;
            }
        }
    }

    private void saveClipToFavorite(InputClip clip) {
        UserInputClipMsgData data = new UserInputClipMsgData(clip);
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Save_As_Favorite).data(data));

        onMsg(msg);
    }

    // =============================== End: 消息处理 ===================================

    private <T> T getBoard(BoardType type) {
        return (T) this.boards.get(type);
    }

    private <T> T currentBoard() {
        return getBoard(this.activeBoard);
    }

    private void activeBoard(BoardType activeType) {
        BoardType currentType = this.activeBoard;
        this.activeBoard = activeType;

        View activeView = this.boards.get(activeType);
        assert activeView != null;

        // Note: 仅视图进场才需要动画，退场视图直接隐藏即可，从而避免二者出现重影
        View currentView = currentType != activeType ? this.boards.get(currentType) : null;
        hideView(currentType, currentView);

        showView(activeView);
    }

    private void showView(View view) {
        if (view instanceof ViewClosable) {
            ((ViewClosable) view).update();
        }

        if (ViewUtils.isVisible(view)) {
            return;
        }

        Animation enterAnim = this.enterAnim;
        enterAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                enterAnim.setAnimationListener(null);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        view.startAnimation(enterAnim);
        // Note: 只有已显示的视图才能应用动画
        ViewUtils.show(view);
    }

    private void hideView(BoardType type, View view) {
        if (view == null || !ViewUtils.isVisible(view)) {
            return;
        }

        if (type == BoardType.main) {
            // Note: 因为主面板在最上层，因此，隐藏时需要直接隐藏，以便于下层的面板的进场动画能够显示，
            // 且主面板需始终保有其所占空间，从而确保其他面板在该空间内布局
            view.setVisibility(View.INVISIBLE);
            return;
        }

        if (view instanceof ViewClosable) {
            ((ViewClosable) view).close();
        }
        // 延迟隐藏，以便于给足视图 close 的时间，避免再次显示时还留有未复位的子视图
        post(() -> ViewUtils.hide(view));
    }

    private enum BoardType {
        main,
        favorite,
        ;
    }

    // ==================== Start: 气泡提示 ==================

    /** 若当前为左手模式，则采用从左到右的布局方向，否则，采用从右到左的布局方向 */
    protected void updateReverseLayoutDirection(View view) {
        if (view == null) {
            return;
        }

        Keyboard.HandMode handMode = this.config.get(ConfigKey.hand_mode);

        ViewUtils.updateLayoutDirection(view, handMode, true);
    }

    private void showInputQuickPopupWindow(List<?> dataList) {
        PopupWindow window = this.inputQuickPopupWindow;
        if (CollectionUtils.isEmpty(dataList)) {
            post(window::dismiss);
            return;
        }

        InputQuickListView inputQuickListView = (InputQuickListView) window.getContentView();
        inputQuickListView.update(dataList);

        showPopupWindow(window);
    }

    private void showInputKeyPopupWindow(String key, boolean hideDelayed) {
        if (this.config.bool(ConfigKey.disable_input_key_popup_tips)) {
            showTipPopupWindow(null, false);
            return;
        }

        showTipPopupWindow(key, hideDelayed);
    }

    private void showTipPopupWindow(int resId) {
        String tips = resId != 0 ? getContext().getString(resId) : null;

        showTipPopupWindow(tips, true);
    }

    private void showTipPopupWindow(String tip, boolean hideDelayed) {
        PopupWindow window = this.tipPopupWindow;
        if (CharUtils.isBlank(tip)) {
            // Note: 存在因提示频率太高而无法隐藏的问题，故而，延迟隐藏
            post(window::dismiss);
            return;
        }

        View contentView = window.getContentView();
        TextView textView = contentView.findViewById(R.id.fg);
        textView.setText(tip);

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
            contentView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

            int height = contentView.getMeasuredHeight();

            // 放置于被布局的键盘之上
            View parent = this.popupAnchor;
            int[] location = new int[2];
            parent.getLocationInWindow(location);

            int x = location[0];
            int y = location[1] - height;

            // 设置初始显示位置：其仅在未显示时有效
            window.showAtLocation(parent, Gravity.START | Gravity.TOP, x, y);

            // 确保窗口按照内容高度调整位置：其仅在显示时有效
            // Note: 需要强制更新，否则，内容布局会出现跳动
            window.update(x, y, window.getWidth(), window.getHeight(), true);
        });
    }

    // ==================== End: 气泡提示 ==================
}
