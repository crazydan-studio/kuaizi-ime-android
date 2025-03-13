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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.core.Clipboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputFavoriteMsgData;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Close_Clipboard;

/**
 * {@link Clipboard} 的视图
 * <p/>
 * 由 {@link InputFavoriteListView} 以及相关的功能按钮组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-13
 */
public class ClipboardView extends LinearLayout implements UserMsgListener, InputMsgListener {
    private final InputFavoriteListView favoriteListView;
    private final TextView titleView;
    private final TextView warningView;

    private Config config;
    private UserMsgListener listener;

    public ClipboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.ime_board_clip_view, this);

        this.titleView = findViewById(R.id.title);
        this.warningView = findViewById(R.id.warning);

        this.favoriteListView = findViewById(R.id.favorite_list);
        this.favoriteListView.setListener(this);

        View closeBtnView = findViewById(R.id.close);
        closeBtnView.setOnClickListener(this::onCloseClipboard);
    }

    public void setConfig(Config config) {
        this.config = config;
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
            case InputFavorite_Create_Done: {
                InputFavoriteMsgData data = msg.data();
                int total = data.favorites.size();
                String title = getContext().getString(R.string.title_clipboard, total > 999 ? "999+" : total);

                this.titleView.setText(title);
                this.favoriteListView.update(data.favorites);

                boolean showWarning = total == 0;
                ViewUtils.visible(this.warningView, showWarning);
                ViewUtils.visible(this.favoriteListView, !showWarning);
                break;
            }
        }
    }

    // =============================== End: 消息处理 ===================================

    // ==================== Start: 按键事件处理 ==================

    private void onCloseClipboard(View v) {
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Close_Clipboard));
        onMsg(msg);
    }

    // ==================== End: 按键事件处理 ==================
}
