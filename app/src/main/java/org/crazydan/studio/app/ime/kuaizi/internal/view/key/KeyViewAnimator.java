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

import java.util.HashSet;
import java.util.Set;

import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;

/**
 * {@link Keyboard 键盘}{@link Key 按键}的{@link RecyclerView}动效
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-16
 */
public class KeyViewAnimator extends DefaultItemAnimator {
    private final Set<Key<?>> fadeOutKeys = new HashSet<>();
    private KeyView<?, ?> closedKeyView;

    public void reset() {
        clearFadeOutKey();
    }

    @Override
    public boolean animateChange(
            RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromX, int fromY, int toX, int toY
    ) {
        resetViewScale(oldHolder.itemView);
        resetViewScale(newHolder.itemView);

        boolean result = super.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY);

        // 在原动画效果上增加放大效果
        if (result && needToFadeOut(oldHolder)) {
            View view = oldHolder.itemView;

            ViewPropertyAnimator anim = view.animate();
            anim.scaleX(2.5f);
            anim.scaleY(2.5f);
        }
        return result;
    }

    @Override
    public void onChangeFinished(RecyclerView.ViewHolder holder, boolean oldHolder) {
        resetViewScale(holder.itemView);

        if (oldHolder && needToFadeOut(holder)) {
            this.fadeOutKeys.remove(((KeyView<?, ?>) holder).getData());
        }
    }

    public void startClosedKeyViewAnimation(KeyView<?, ?> keyView) {
        if (this.closedKeyView == keyView) {
            return;
        }

        cancelPrevClosedKeyViewAnimation();

        this.closedKeyView = keyView;
        if (this.closedKeyView != null) {
            View view = this.closedKeyView.itemView;

            ViewUtils.startAnimationInfinite(view,
                                             700,
                                             new ScaleAnimation(1.0f,
                                                                2.0f,
                                                                1.0f,
                                                                2.0f,
                                                                ScaleAnimation.RELATIVE_TO_SELF,
                                                                0.5f,
                                                                ScaleAnimation.RELATIVE_TO_SELF,
                                                                0.5f),
                                             new AlphaAnimation(0.8f, 0.4f),
                                             new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF,
                                                                    0,
                                                                    TranslateAnimation.RELATIVE_TO_SELF,
                                                                    0,
                                                                    TranslateAnimation.RELATIVE_TO_SELF,
                                                                    0,
                                                                    TranslateAnimation.RELATIVE_TO_SELF,
                                                                    -1.0f));
        }
    }

    public void cancelPrevClosedKeyViewAnimation() {
        if (this.closedKeyView != null) {
            View view = this.closedKeyView.itemView;
            ViewUtils.stopAnimation(view);
        }
        this.closedKeyView = null;
    }

    public void addFadeOutKey(Key<?> fadeOutKey) {
        if (fadeOutKey != null) {
            this.fadeOutKeys.add(fadeOutKey);
        }
    }

    private void clearFadeOutKey() {
        this.fadeOutKeys.clear();
    }

    private boolean needToFadeOut(RecyclerView.ViewHolder holder) {
        return holder instanceof KeyView //
               && this.fadeOutKeys.contains(((KeyView<?, ?>) holder).getData());
    }

    private void resetViewScale(View view) {
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
    }
}
