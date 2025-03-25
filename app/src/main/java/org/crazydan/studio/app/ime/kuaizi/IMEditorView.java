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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ObjectUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.AudioPlayer;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewClosable;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.ConfigUpdateMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputAudioPlayMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.BaseThemedView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.CandidatesView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.FavoriteboardView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.MainboardView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.xpad.XPadView;

/**
 * {@link IMEditor} 的视图
 * <p/>
 * 由 {@link MainboardView}、{@link FavoriteboardView} 等组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-13
 */
public class IMEditorView extends BaseThemedView {
    private enum BoardType {
        main,
        favorite,
        ;
    }

    private final AudioPlayer audioPlayer;

    /** Note: 在子类中，可能不存在输入候选视图 */
    private CandidatesView candidatesView;
    private Animation enterAnim;

    private BoardType activeBoard = BoardType.main;
    private Map<BoardType, View> boards;

    public IMEditorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, R.layout.ime_root_view, false);

        this.audioPlayer = new AudioPlayer();
        this.audioPlayer.load(getContext(),
                              R.raw.tick_single,
                              R.raw.tick_double,
                              R.raw.page_flip,
                              R.raw.tick_clock,
                              R.raw.tick_ping);
    }

    public XPadView getXPadView() {
        MainboardView mainboard = getBoard(BoardType.main);
        return mainboard.getXPadView();
    }

    public void close() {
        ObjectUtils.invokeWhenNonNull(this.candidatesView, CandidatesView::close);

        ObjectUtils.invokeWhenNonNull(this.boards, (boards) -> {
            boards.values().forEach((b) -> {
                if (b instanceof ViewClosable) {
                    ((ViewClosable) b).close();
                }
            });
        });
    }

    // =============================== Start: 视图更新 ===================================

    @Override
    protected void doLayout() {
        ObjectUtils.invokeWhenNonNull(this.candidatesView, CandidatesView::destroy);

        super.doLayout();

        // //////////////////////////////////////////////////////////
        this.candidatesView = initCandidatesView();

        // Note: 取当前视图主题的配置数据，需通过在主题上下文中布局的视图
        Context context = findViewById(R.id.root).getContext();
        int enterAnimResId = ThemeUtils.getResourceByAttrId(context, R.attr.anim_fade_in);
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
    }

    protected CandidatesView initCandidatesView() {
        CandidatesView view = findViewById(R.id.candidates);

        view.setConfig(this.config);
        view.setListener(this);

        return view;
    }

    // =============================== End: 视图更新 ===================================

    // =============================== Start: 消息处理 ===================================

    @Override
    public void onMsg(UserInputMsg msg) {
        super.onMsg(msg);

        // 直接处理面板关闭消息。Note: 关闭无需清理等工作
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
        super.onMsg(msg);

        // Note: 涉及重建视图和视图显隐切换等情况，因此，需在最后转发消息到子视图
        InputMsgListener current = currentBoard();
        current.onMsg(msg);

        ObjectUtils.invokeWhenNonNull(this.candidatesView, (v) -> post(() -> v.onMsg(msg)));
    }

    @Override
    protected void handleMsg(InputMsg msg) {
        switch (msg.type) {
            case InputAudio_Play_Doing: {
                on_InputAudio_Play_Doing_Msg(msg.data());
                break;
            }
            case Keyboard_Start_Done: {
                // 确保键盘启动后，始终在主面板上，
                // 从而保证可粘贴提示等在主面板上的弹窗能够正常显示
                activeBoard(BoardType.main);
                break;
            }
            case InputFavorite_Be_Ready: {
                activeBoard(BoardType.favorite);
                break;
            }
            case Config_Update_Done: {
                ConfigUpdateMsgData data = msg.data();
                // Note: 主题变更的处理将由父类处理
                switch (data.configKey) {
                    // 视图相关的配置变更，需要更新视图
                    case enable_x_input_pad:
                    case adapt_desktop_swipe_up_gesture: {
                        activeBoard(this.activeBoard);
                        break;
                    }
                }
                break;
            }
            default: {
                this.log.warn("Ignore message %s", () -> new Object[] { msg.type });
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
        hideBoardView(currentType, currentView);

        showBoardView(activeView);
    }

    private void showBoardView(View view) {
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

    private void hideBoardView(BoardType type, View view) {
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
}
