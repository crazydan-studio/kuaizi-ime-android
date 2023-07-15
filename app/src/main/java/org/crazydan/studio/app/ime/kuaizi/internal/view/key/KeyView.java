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

package org.crazydan.studio.app.ime.kuaizi.internal.view.key;

import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewHolder;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link Keyboard 键盘}{@link Key 按键}的视图
 * <p/>
 * 视图存在重复使用的情况，故在
 * {@link #bind(Key, HexagonOrientation)}
 * 内需先复位视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public abstract class KeyView<K extends Key<?>, V extends View> extends RecyclerViewHolder {
    protected final ImageView bgView;
    protected final V fgView;

    private K key;

    public KeyView(@NonNull View itemView) {
        super(itemView);

        this.bgView = itemView.findViewById(R.id.bg_view);
        this.fgView = itemView.findViewById(R.id.fg_view);
    }

    public K getKey() {
        return this.key;
    }

    public void bind(K key, HexagonOrientation orientation) {
        this.key = key;

        if (key.isDisabled()) {
            disable();
        } else {
            enable();
        }

        KeyViewDrawable drawable = new KeyViewDrawable(orientation);

        int bgColor = getColorByAttrId(key.getBgColorAttrId());
        drawable.setColor(bgColor);
        drawable.setCornerRadius(10);

        this.bgView.setImageDrawable(drawable);
    }
}
