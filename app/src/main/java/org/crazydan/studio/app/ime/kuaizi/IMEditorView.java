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
import java.util.Map;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.AudioPlayer;
import org.crazydan.studio.app.ime.kuaizi.common.widget.Toast;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewClosable;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputClip;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.ConfigUpdateMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputAudioPlayMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputClipMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputListCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserSaveAsFavoriteBtnSingleTapMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.FavoriteboardView;
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
public class IMEditorView extends LinearLayout implements UserMsgListener, InputMsgListener {
    protected final Logger log = Logger.getLogger(getClass());

    private final AudioPlayer audioPlayer;

    private Config.Mutable config;
    private UserMsgListener listener;

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

    public void setConfig(Config.Mutable config) {
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
    }

    // =============================== Start: 视图更新 ===================================

    /** 布局视图 */
    private void doLayout() {
        // 必须先清除已有的子视图，否则，重复 inflate 会无法即时生效
        removeAllViews();

        Keyboard.Theme theme = this.config.get(ConfigKey.theme);
        int themeResId = theme.getResId(getContext());

        // 通过 Context Theme 仅对键盘自身的视图设置主题样式，
        // 以避免通过 AppCompatDelegate.setDefaultNightMode 对配置等视图造成影响
        // Note: 其内部的视图也会按照主题样式进行更新，无需单独处理
        ThemeUtils.inflate(this, R.layout.ime_root_view, themeResId, true);

        MainboardView mainboardView = findViewById(R.id.mainboard);
        mainboardView.setConfig(this.config);
        mainboardView.setListener(this);
        mainboardView.updateBottomSpacing(true);

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
        switch (msg.type) {
            case Keyboard_Start_Done: {
                // Note: 键盘启动时，可能涉及横竖屏的转换，故而，需做一次底部空白更新
                MainboardView mainboard = getBoard(BoardType.main);
                mainboard.updateBottomSpacing(false);
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
            case InputList_Commit_Doing: {
                InputListCommitMsgData data = msg.data();
                // 是否收藏已输入内容：仅针对非替换且可撤回的输入
                if (!data.canBeRevoked || !CollectionUtils.isEmpty(data.replacements)) {
                    break;
                }

                Toast.with(this)
                     .setText(R.string.tip_whether_save_commited_content)
                     .setDuration(5000)
                     .setAction(R.string.btn_save_as_favorite, (v) -> saveTextToFavorite(data.text))
                     .show();
                break;
            }
            case InputClip_CanBe_Favorite: {
                InputClipMsgData data = msg.data();

                Toast.with(this)
                     .setText(data.source == InputClipMsgData.SourceType.copy_cut
                              ? R.string.tip_whether_save_copied_content
                              : R.string.tip_whether_save_pasted_content)
                     .setDuration(5000)
                     .setAction(R.string.btn_save_as_favorite, (v) -> saveClipToFavorite(data.clip))
                     .show();
                break;
            }
            case Favoriteboard_Start_Done: {
                activeBoard(BoardType.favorite);
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
                doLayout();
                break;
            }
            case enable_x_input_pad:
            case adapt_desktop_swipe_up_gesture: {
                MainboardView mainboard = getBoard(BoardType.main);
                mainboard.updateBottomSpacing(false);
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

    private void saveTextToFavorite(CharSequence text) {
        UserInputMsgData data = new UserSaveAsFavoriteBtnSingleTapMsgData(text);
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Save_As_Favorite).data(data));

        onMsg(msg);
    }

    private void saveClipToFavorite(InputClip clip) {
        UserInputMsgData data = new UserSaveAsFavoriteBtnSingleTapMsgData(clip);
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Save_As_Favorite).data(data));

        onMsg(msg);
    }

    // =============================== End: 消息处理 ===================================

    private <T> T getBoard(BoardType type) {
        return (T) this.boards.get(type);
    }

    private void activeBoard(BoardType type) {
        this.activeBoard = type;

        this.boards.forEach((t, view) -> {
            if (t == type) {
                view.setVisibility(View.VISIBLE);
            } else {
                // TODO 增加淡出动画
                if (t == BoardType.main) {
                    // Note: 主面板需始终保有其所占空间，从而确保其他面板在该空间内布局
                    view.setVisibility(View.INVISIBLE);
                } else {
                    view.setVisibility(View.GONE);
                }

                if (view instanceof ViewClosable) {
                    ((ViewClosable) view).close();
                }
            }
        });
    }

    private <T> T currentBoard() {
        return getBoard(this.activeBoard);
    }

    private enum BoardType {
        main,
        favorite,
        settings,
        ;
    }
}
