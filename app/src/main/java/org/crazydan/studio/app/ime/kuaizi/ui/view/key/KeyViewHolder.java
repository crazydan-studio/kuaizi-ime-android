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

package org.crazydan.studio.app.ime.kuaizi.ui.view.key;

import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.HexagonDrawable;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewHolder;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link Key} 视图的 {@link RecyclerView.ViewHolder}
 * <p/>
 * 视图存在重复使用的情况，故在
 * {@link #bind(Key, HexagonOrientation)}
 * 内需先复位视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public abstract class KeyViewHolder<V extends View> extends RecyclerViewHolder {
    protected final ImageView bgView;
    protected final V fgView;

    public KeyViewHolder(@NonNull View itemView) {
        super(itemView);

        this.bgView = itemView.findViewById(R.id.bg_view);
        this.fgView = itemView.findViewById(R.id.fg_view);
    }

    public void bind(Key<?> key, HexagonOrientation orientation) {
        if (key.isDisabled()) {
            disable();
        } else {
            enable();
        }

        updateBgView(key, orientation);
    }

    private void updateBgView(Key<?> key, HexagonOrientation orientation) {
        whenViewReady(this.bgView, (view) -> {
            if (key.getColor().bg == null) {
                view.setImageDrawable(null);
                return;
            }
            ViewUtils.enableHardwareAccelerated(view);

            int bgColor = getColorByAttrId(key.getColor().bg);
            float cornerRadius = ScreenUtils.pxFromDimension(getContext(), R.dimen.key_view_corner_radius);

            HexagonDrawable drawable = new HexagonDrawable(orientation);
            drawable.setCornerRadius(cornerRadius);
            drawable.setFillColor(bgColor);

            if (!key.isDisabled()) {
                drawable.setShadow(getStringByAttrId(R.attr.key_shadow_style));
                drawable.setBorder(getStringByAttrId(R.attr.key_border_style));
            } else {
                drawable.setBorder(getStringByAttrId(R.attr.key_disabled_border_style));
            }

            view.setImageDrawable(drawable);
        });
    }
}
