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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ObjectUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ConfirmPopup;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewClosable;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.core.Favoriteboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputFavoriteMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserDeleteSelectedBtnSingleTapMsgData;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Clear_All_InputFavorite;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Close_Favoriteboard;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Delete_Selected_InputFavorite;

/**
 * {@link Favoriteboard} 的视图
 * <p/>
 * 由 {@link InputFavoriteListView} 以及相关的功能按钮组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-13
 */
public class FavoriteboardView extends LinearLayout implements UserMsgListener, InputMsgListener, ViewClosable {
    private final InputFavoriteListView favoriteListView;
    private final TextView titleView;
    private final TextView warningView;
    private final View dataPaneView;

    private final TextView deleteSelectedBtnView;
    private final TextView clearAllBtnView;

    private ConfirmPopup deleteSelectedPopup;
    private ConfirmPopup clearAllPopup;

    private Config config;
    private UserMsgListener listener;

    public FavoriteboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.ime_board_favorite_view, this);

        this.titleView = findViewById(R.id.title);
        this.warningView = findViewById(R.id.warning);
        this.dataPaneView = findViewById(R.id.data_pane);
        this.deleteSelectedBtnView = findViewById(R.id.delete_selected);
        this.clearAllBtnView = findViewById(R.id.clear_all);

        this.favoriteListView = findViewById(R.id.favorite_list);
        this.favoriteListView.setListener(this);

        View closeBtnView = findViewById(R.id.close);
        closeBtnView.setOnClickListener(this::onCloseFavoriteboard);
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public void close() {
        this.favoriteListView.update(new ArrayList<>());

        ObjectUtils.invokeWhenNonNull(this.deleteSelectedPopup, ConfirmPopup::dismiss);
        ObjectUtils.invokeWhenNonNull(this.clearAllPopup, ConfirmPopup::dismiss);

        this.deleteSelectedPopup = null;
        this.clearAllPopup = null;
    }

    // =============================== Start: 消息处理 ===================================

    public void setListener(UserMsgListener listener) {
        this.listener = listener;
    }

    /** 响应内部视图的 {@link UserInputMsg} 消息：从视图向上传递给外部监听者 */
    @Override
    public void onMsg(UserInputMsg msg) {
        switch (msg.type) {
            // 仅内部处理的消息，不必向上传递
            case SingleTap_Btn_Select_InputFavorite: {
                on_SingleTap_Select_InputFavorite_Msg();
                break;
            }
            default: {
                this.listener.onMsg(msg);
            }
        }
    }

    // -------------------------------------------

    /** 响应 {@link InputMsg} 消息：向下传递消息给内部视图 */
    @Override
    public void onMsg(InputMsg msg) {
        switch (msg.type) {
            case InputFavorite_Apply_Done:
            case InputFavorite_Delete_Done:
            case Favoriteboard_Start_Done: {
                InputFavoriteMsgData data = msg.data();
                on_InputFavorite_Update_Done_Msg(data);
                break;
            }
        }
    }

    private void on_InputFavorite_Update_Done_Msg(InputFavoriteMsgData data) {
        int total = data.favorites.size();
        CharSequence title = textWithNumber(R.string.title_favorites, total);

        this.titleView.setText(title);
        this.favoriteListView.update(data.favorites);

        boolean showWarning = total == 0;
        ViewUtils.visible(this.warningView, showWarning);
        ViewUtils.visible(this.dataPaneView, !showWarning);

        updateBtnStatus();
    }

    private void on_SingleTap_Select_InputFavorite_Msg() {
        updateBtnStatus();
    }

    // =============================== End: 消息处理 ===================================

    // ==================== Start: 按键事件处理 ==================

    private void onCloseFavoriteboard(View v) {
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Close_Favoriteboard));
        onMsg(msg);
    }

    private void onDeleteSelected(View v) {
        List<Integer> selected = this.favoriteListView.getAdapter().getSelectedItems();
        if (selected.isEmpty()) {
            return;
        }

        this.deleteSelectedPopup = //
                ConfirmPopup.with(this)
                            .setMessage(R.string.tip_whether_delete_selected)
                            .setPositiveButton(R.string.btn_confirm, (vv) -> {
                                UserDeleteSelectedBtnSingleTapMsgData data = new UserDeleteSelectedBtnSingleTapMsgData(
                                        selected);
                                UserInputMsg msg = UserInputMsg.build((b) -> b.type(
                                        SingleTap_Btn_Delete_Selected_InputFavorite).data(data));

                                onMsg(msg);
                            })
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show();
    }

    private void onClearAll(View v) {
        this.clearAllPopup = //
                ConfirmPopup.with(this)
                            .setMessage(R.string.tip_whether_clear_all)
                            .setPositiveButton(R.string.btn_confirm, (vv) -> {
                                UserInputMsg msg = UserInputMsg.build((b) -> b.type(
                                        SingleTap_Btn_Clear_All_InputFavorite));
                                onMsg(msg);
                            })
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show();
    }

    // ==================== End: 按键事件处理 ==================

    private void updateBtnStatus() {
        List<Integer> selected = this.favoriteListView.getAdapter().getSelectedItems();
        int total = selected.size();
        boolean disabled = total == 0;

        updateBtn(this.deleteSelectedBtnView,
                  textWithNumber(R.string.btn_delete_selected, total),
                  disabled,
                  this::onDeleteSelected);
        updateBtn(this.clearAllBtnView, getContext().getString(R.string.btn_clear_all), disabled, this::onClearAll);
    }

    private void updateBtn(TextView view, CharSequence text, boolean disabled, View.OnClickListener listener) {
        if (disabled) {
            view.setAlpha(.1f);
            view.setOnClickListener(null);
        } else {
            text = ViewUtils.parseHtml("<a href='#'>" + text + "</a>");

            view.setAlpha(1f);
            view.setOnClickListener(listener);
        }

        view.setText(text);
    }

    private CharSequence textWithNumber(int resId, int number) {
        return getContext().getString(resId, number > 999 ? "999+" : number);
    }
}
