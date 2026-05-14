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
import java.util.Objects;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ObjectUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewLinearLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputClip;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputClipMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputCompletionMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.InputQuickListViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.quick.InputQuickViewData;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_InputClip;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_InputCompletion;

/**
 * 快捷输入的列表视图
 * <p/>
 * 需在显示前调用 {@link #update} 更新列表数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-11
 */
public class InputQuickListView extends RecyclerView<InputQuickListViewAdapter, InputQuickViewData>
        implements ViewGestureDetector.Listener {
    private UserInputMsgListener listener;

    public InputQuickListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        RecyclerViewGestureDetector<InputQuickViewData> gesture = new RecyclerViewGestureDetector<>(context, this);
        gesture.addListener(this);
    }

    @Override
    protected InputQuickListViewAdapter createAdapter() {
        return new InputQuickListViewAdapter();
    }

    @Override
    protected LayoutManager createLayoutManager(Context context) {
        return new RecyclerViewLinearLayoutManager(context);
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

        ViewHolder holder = findViewHolderUnder(data.at.x, data.at.y);
        if (holder == null) {
            return;
        }

        int position = holder.getAdapterPosition();
        InputQuickViewData item = getAdapter().getItem(position);

        UserInputMsg msg = null;
        switch (item.type) {
            case input_completion: {
                UserInputCompletionMsgData msgData = new UserInputCompletionMsgData(position);
                msg = UserInputMsg.build((b) -> b.type(SingleTap_InputCompletion).data(msgData));
                break;
            }
            case input_clip: {
                UserInputClipMsgData msgData = new UserInputClipMsgData(position, (InputClip) item.data);
                msg = UserInputMsg.build((b) -> b.type(SingleTap_InputClip).data(msgData));
                break;
            }
        }

        ObjectUtils.invokeWhenNonNull(msg, (m) -> this.listener.onMsg(m));
    }

    // =============================== End: 消息处理 ===================================

    public void update(List<?> dataList) {
        List<InputQuickViewData> newItems = InputQuickViewData.from(dataList);
        List<InputQuickViewData> oldItems = getAdapter().updateItems(newItems);

        // 在发生变化时复位滚动位置
        if (!Objects.equals(newItems, oldItems)) {
            getLayoutManager().scrollToPosition(0);
        }
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        super.setLayoutDirection(layoutDirection);

        // Note: 触发布局方向的切换动画
        if (getAdapter().getItemCount() > 0) {
            getAdapter().notifyItemChanged(0);
        }
    }
}
