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

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public abstract class RecyclerViewHolder extends RecyclerView.ViewHolder {

    public RecyclerViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public boolean isHidden() {
        return this.itemView.getVisibility() == View.GONE;
    }

    public final Context getContext() {
        return this.itemView.getContext();
    }

    public void hide() {
        this.itemView.setVisibility(View.GONE);
    }

    public void show() {
        this.itemView.setVisibility(View.VISIBLE);
    }
}
