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
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewLinearLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.core.input.clip.ClipInputData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserClipInputDataSingleTapMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.clip.ClipInputDataListViewAdapter;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_ClipInputData;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-08
 */
public class ClipInputDataListView extends RecyclerView<ClipInputDataListViewAdapter, ClipInputData>
        implements ViewGestureDetector.Listener {
    private UserInputMsgListener listener;

    public ClipInputDataListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        RecyclerViewGestureDetector<ClipInputData> gesture = new RecyclerViewGestureDetector<>(context, this);
        gesture.addListener(this);
    }

    @Override
    protected ClipInputDataListViewAdapter createAdapter() {
        return new ClipInputDataListViewAdapter();
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
        ClipInputData clip = getAdapter().getItem(position);

        UserClipInputDataSingleTapMsgData msgData = new UserClipInputDataSingleTapMsgData(clip);
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_ClipInputData).data(msgData));

        this.listener.onMsg(msg);
    }

    // =============================== End: 消息处理 ===================================

    public void update(List<ClipInputData> clips) {
        getAdapter().updateItems(clips);

        // 复位滚动位置
        getLayoutManager().scrollToPosition(0);
    }
}
