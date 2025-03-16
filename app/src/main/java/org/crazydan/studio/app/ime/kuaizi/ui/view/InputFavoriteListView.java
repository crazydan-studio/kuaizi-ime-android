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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewLinearLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputFavorite;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputFavoriteMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.InputFavoriteListViewAdapter;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Paste_InputFavorite;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Select_InputFavorite;

/**
 * {@link InputFavorite} 的列表视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-13
 */
public class InputFavoriteListView extends RecyclerView<InputFavoriteListViewAdapter, InputFavorite>
        implements InputFavoriteListViewAdapter.ItemListener {
    private UserInputMsgListener listener;

    public InputFavoriteListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected InputFavoriteListViewAdapter createAdapter() {
        return new InputFavoriteListViewAdapter(this);
    }

    @Override
    protected LayoutManager createLayoutManager(Context context) {
        return new RecyclerViewLinearLayoutManager(context, true);
    }

    public boolean isEmpty() {
        return getAdapter().getItemCount() == 0;
    }

    public List<Integer> getSelected() {
        return getAdapter().getSelectedItems();
    }

    public void clear() {
        getAdapter().updateItems(new ArrayList<>(), true);
    }

    public void update(List<InputFavorite> dataList, boolean reset) {
        getAdapter().updateItems(dataList, reset);
    }

    // =============================== Start: 消息处理 ===================================

    public void setListener(UserInputMsgListener listener) {
        this.listener = listener;
    }

    // -------------------------------------------

    @Override
    public void onItemCheck(int position, boolean checked) {
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Select_InputFavorite));

        this.listener.onMsg(msg);
    }

    @Override
    public void onItemPaste(int position) {
        InputFavorite favorite = getAdapter().getItem(position);

        UserInputFavoriteMsgData data = new UserInputFavoriteMsgData(position, favorite);
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Paste_InputFavorite).data(data));

        this.listener.onMsg(msg);
    }

    // =============================== End: 消息处理 ===================================
}
