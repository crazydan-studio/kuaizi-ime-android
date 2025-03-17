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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-17
 */
public abstract class DirectionBoardView extends FrameLayout {
    protected Config config;

    public DirectionBoardView(@NonNull Context context, @Nullable AttributeSet attrs, int layoutResId) {
        super(context, attrs);

        // Note: 所布局的视图将作为当前视图的子视图插入，而不会替换当前视图
        inflate(context, layoutResId, this);
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    /** 若当前为左手模式，则采用从右到左的布局方向，否则，采用从左到右的布局方向 */
    protected void updateLayoutDirection() {
        Keyboard.HandMode handMode = this.config.get(ConfigKey.hand_mode);

        ViewUtils.updateLayoutDirection(this, handMode, false);
    }

    /**
     * 与 {@link #updateLayoutDirection()} 相反：
     * 若当前为左手模式，则采用从左到右的布局方向，否则，采用从右到左的布局方向
     */
    protected void updateReverseLayoutDirection(View view) {
        if (view == null) {
            return;
        }

        Keyboard.HandMode handMode = this.config.get(ConfigKey.hand_mode);

        ViewUtils.updateLayoutDirection(view, handMode, true);
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        super.setLayoutDirection(layoutDirection);

        // 修改当前视图所布局的子视图的布局方向
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.setLayoutDirection(layoutDirection);
        }
    }
}
