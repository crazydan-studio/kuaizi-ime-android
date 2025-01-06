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

package org.crazydan.studio.app.ime.kuaizi.ui.view.input;

import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData;

/**
 * 空格类型 {@link Input} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class SpaceInputViewHolder extends InputViewHolder {
    private final ImageView spaceView;

    public SpaceInputViewHolder(@NonNull View itemView) {
        super(itemView);

        this.spaceView = itemView.findViewById(R.id.space_view);
    }

    public void bind(InputViewData data, boolean selected) {
        whenViewReady(this.spaceView, (view) -> {
            addLeftSpaceMargin(this.itemView, data.gapSpaces);
            setSelectedBgColor(this.itemView, selected);
        });
    }
}
