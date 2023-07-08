/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2023 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.internal.view;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.view.input.InputCandidateViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.internal.view.input.InputCandidateViewLayoutManager;

/**
 * 输入候选字/词视图
 * <p/>
 * 负责显示输入的可选字列表
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class InputCandidatesView extends RecyclerView implements InputMsgListener {
    private final Set<Listener> listeners = new HashSet<>();

    private final InputCandidateViewAdapter adapter;
    private final InputCandidateViewLayoutManager layoutManager;

    public InputCandidatesView(Context context) {
        this(context, null);
    }

    public InputCandidatesView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.adapter = new InputCandidateViewAdapter();
        this.layoutManager = new InputCandidateViewLayoutManager(context);

        setAdapter(this.adapter);
        setLayoutManager(this.layoutManager);
    }

    public void setInputList(InputList inputList) {
        this.adapter.setInputList(inputList);
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        switch (msg) {
            case InputtingChars:
            case InputtingCharsDone:
                boolean shown = this.adapter.getItemCount() > 0;
                this.listeners.forEach(l -> l.onShown(shown));

                this.adapter.notifyDataSetChanged();
                if (shown) {
                    smoothScrollToPosition(this.adapter.getSelectedWordPosition());
                }
                break;
        }
    }

    public interface Listener {

        void onShown(boolean shown);
    }
}
