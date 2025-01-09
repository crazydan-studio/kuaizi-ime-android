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

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputCompletion;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputCompletionSingleTapMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.completion.InputCompletionListViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.view.completion.InputCompletionListViewLayoutManager;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_InputCompletion;

/**
 * 输入补全列表视图
 * <p/>
 * 需在显示前调用 {@link #update} 更新列表数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-11
 */
public class InputCompletionListView extends RecyclerView<InputCompletionListViewAdapter, InputCompletion.ViewData>
        implements ViewGestureDetector.Listener {
    private UserInputMsgListener listener;

    public InputCompletionListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        getAdapter().setLayoutManager((InputCompletionListViewLayoutManager) getLayoutManager());

        RecyclerViewGestureDetector<InputCompletion.ViewData> gesture = new RecyclerViewGestureDetector<>(this);
        gesture.addListener(this);
    }

    @Override
    protected InputCompletionListViewAdapter createAdapter() {
        return new InputCompletionListViewAdapter();
    }

    @Override
    protected LayoutManager createLayoutManager(Context context) {
        return new InputCompletionListViewLayoutManager(context);
    }

    // =============================== Start: 消息处理 ===================================

    public void setListener(UserInputMsgListener listener) {
        this.listener = listener;
    }

    /** 向上传递 {@link UserInputMsg} 消息 */
    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        if (type != ViewGestureDetector.GestureType.SingleTap) {
            return;
        }

        ViewHolder holder = getViewHolderUnder(data.x, data.y);
        if (holder == null) {
            return;
        }

        int position = holder.getAdapterPosition();
        UserInputCompletionSingleTapMsgData msgData = new UserInputCompletionSingleTapMsgData(position);
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_InputCompletion).data(msgData));

        this.listener.onMsg(msg);
    }

    // =============================== End: 消息处理 ===================================

    public void update(List<InputCompletion.ViewData> completions) {
        getAdapter().updateItems(completions);
    }
}
