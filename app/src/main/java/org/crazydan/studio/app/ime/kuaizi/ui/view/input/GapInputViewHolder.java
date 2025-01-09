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

package org.crazydan.studio.app.ime.kuaizi.ui.view.input;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.GapInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData;

/**
 * {@link GapInput} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class GapInputViewHolder extends InputViewHolder {
    private final View blinkView;

    public GapInputViewHolder(@NonNull View itemView) {
        super(itemView);

        this.blinkView = itemView.findViewById(R.id.blink_view);
    }

    @Override
    public void bind(InputViewData data) {
        // Note: 空白只能加在根视图上，否则不会立即生效
        addGapSpacePadding(this.itemView, data.gapSpaces);

        whenViewReady(this.blinkView, (view) -> {
            ViewUtils.visible(view, data.selected);

            if (data.selected) {
                startCursorBlink(view);
            } else {
                stopCursorBlink(view);
            }
        });
    }

    private void stopCursorBlink(View view) {
        view.clearAnimation();
    }

    private void startCursorBlink(View view) {
        // 图形扩散淡化消失的效果
        // https://cloud.tencent.com/developer/article/1742156
        AnimationSet animationSet = new AnimationSet(true);

        Animation[] animations = new Animation[] {
                new AlphaAnimation(0.8f, 0.3f),
                };
        for (Animation animation : animations) {
            // Note: 可以将闪动当作秒表
            animation.setDuration(1000);
            animation.setRepeatCount(ValueAnimator.INFINITE);

            animationSet.addAnimation(animation);
        }

        view.startAnimation(animationSet);
    }
}
