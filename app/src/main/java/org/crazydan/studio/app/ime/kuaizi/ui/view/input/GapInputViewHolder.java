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

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.GapInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.InputViewData;

/**
 * {@link GapInput} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class GapInputViewHolder extends InputViewHolder {
    private final View cursorView;
    private final View pendingView;
    private final View blinkView;

    public GapInputViewHolder(@NonNull View itemView) {
        super(itemView);

        this.cursorView = itemView.findViewById(R.id.cursor_view);
        this.pendingView = itemView.findViewById(R.id.pending_view);
        this.blinkView = itemView.findViewById(R.id.blink_view);
    }

    public void bind(InputViewData data, boolean selected) {
        CharInput pending = data.pending;
        boolean hasPending = !Input.isEmpty(pending);
        whenViewReady(this.pendingView, (view) -> {
            if (hasPending) {
                addLeftSpaceMargin(view, data.gapSpaces);
                setSelectedBgColor(view, selected);

                showWord(data.option, pending, selected);
            }
            ViewUtils.visible(view, hasPending);
        });

        whenViewReady(this.cursorView, (view) -> {
            if (!hasPending) {
                addLeftSpaceMargin(view, data.gapSpaces);
            }
            ViewUtils.visible(view, !hasPending);
        });

        whenViewReady(this.blinkView, (view) -> {
            boolean shown = selected && !hasPending;
            ViewUtils.visible(view, shown);

            if (shown) {
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
