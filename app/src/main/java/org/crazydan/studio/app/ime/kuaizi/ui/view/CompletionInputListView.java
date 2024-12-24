/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.view.completion.CompletionInputListViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.view.completion.CompletionInputListViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.ui.view.completion.CompletionInputViewHolder;

import static org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsgType.SingleTap_CompletionInput;

/**
 * 输入补全列表视图
 * <p/>
 * 需在显示前调用 {@link #update} 更新列表数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-11
 */
public class CompletionInputListView extends RecyclerView implements ViewGestureDetector.Listener {
    private final CompletionInputListViewAdapter adapter;

    private UserInputMsgListener listener;

    public CompletionInputListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        CompletionInputListViewLayoutManager layoutManager = new CompletionInputListViewLayoutManager(context);
        setLayoutManager(layoutManager);

        this.adapter = new CompletionInputListViewAdapter(layoutManager);
        setAdapter(this.adapter);

        RecyclerViewGestureDetector gesture = new RecyclerViewGestureDetector();
        gesture.bind(this) //
               .addListener(this);
    }

    // =============================== Start: 消息处理 ===================================

    public void setListener(UserInputMsgListener listener) {
        this.listener = listener;
    }

    /** 向上传递 {@link UserInputMsg} 消息 */
    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        CompletionInputViewHolder holder = findCompletionViewHolderUnder(data.x, data.y);
        if (holder == null) {
            return;
        }

        if (type == ViewGestureDetector.GestureType.SingleTap) {
            CompletionInput completion = holder.getData();

            UserInputMsg msg = new UserInputMsg(SingleTap_CompletionInput, new UserInputMsgData(completion));
            this.listener.onMsg(msg);
        }
    }

    // =============================== End: 消息处理 ===================================

    public void update(List<CompletionInput> completions) {
        this.adapter.updateDataList(completions);
    }

    private CompletionInputViewHolder findCompletionViewHolderUnder(float x, float y) {
        View view = findChildViewUnder(x, y);
        if (view == null) {
            return null;
        }

        CompletionInputViewHolder holder = (CompletionInputViewHolder) getChildViewHolder(view);
        this.adapter.updateViewHolder(holder);

        return holder;
    }
}
