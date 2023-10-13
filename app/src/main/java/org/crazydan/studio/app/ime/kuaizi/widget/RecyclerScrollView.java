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

package org.crazydan.studio.app.ime.kuaizi.widget;

import android.content.Context;
import android.util.AttributeSet;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 适配 {@link RecyclerView} 的 {ScrollView}，
 * 以支持滚动
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-13
 */
public class RecyclerScrollView extends NestedScrollView {

    public RecyclerScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);

//        setOnTouchListener((view, event) -> {
//            view.getParent().requestDisallowInterceptTouchEvent(false);
//            view.onTouchEvent(event);
//
//            return true;
//        });
    }
}
