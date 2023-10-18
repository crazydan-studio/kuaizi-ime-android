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

package org.crazydan.studio.app.ime.kuaizi.internal.view.input;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.GapInput;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;

/**
 * {@link GapInput} 的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class GapInputView extends InputView<GapInput> {
    private final View cursorView;
    private final View pendingView;
    private final View blinkView;

    public GapInputView(@NonNull View itemView) {
        super(itemView);

        this.cursorView = itemView.findViewById(R.id.cursor_view);
        this.pendingView = itemView.findViewById(R.id.pending_view);
        this.blinkView = itemView.findViewById(R.id.blink_view);
    }

    public void bind(Input.Option option, GapInput data, CharInput pending, boolean selected, int gapSpaceCount) {
        super.bind(data);

        boolean hasPending = !Input.isEmpty(pending);
        if (hasPending) {
            addLeftSpaceMargin(this.pendingView, gapSpaceCount);

            ViewUtils.hide(this.cursorView);
            ViewUtils.show(this.pendingView);

            showWord(option, pending, selected);
            setSelectedBgColor(this.pendingView, selected);
        } else {
            addLeftSpaceMargin(this.cursorView, gapSpaceCount);

            ViewUtils.show(this.cursorView);
            ViewUtils.hide(this.pendingView);
        }

        if (selected && !hasPending) {
            ViewUtils.show(this.blinkView);
            startCursorBlink();
        } else {
            ViewUtils.hide(this.blinkView);
            stopCursorBlink();
        }
    }

    public void stopCursorBlink() {
        this.blinkView.clearAnimation();
    }

    public void startCursorBlink() {
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

        this.blinkView.startAnimation(animationSet);
    }
}
