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

import android.animation.ValueAnimator;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.utils.ColorUtils;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link Keyboard 键盘}{@link CharKey 字符按键}的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class CharKeyView extends KeyView<CharKey, TextView> {

    public CharKeyView(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(CharKey key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        this.fgView.setText(key.getText());

        int fgColor = ColorUtils.getByAttrId(getContext(), key.getFgColorAttrId());
        this.fgView.setTextColor(fgColor);
    }

    public void showTouchDown() {
    }

    public void showTouchUp() {
    }

    public void clearAnimation() {
        this.itemView.clearAnimation();
    }

    public void startAnimation() {
        // 图形扩散淡化消失的效果
        // https://cloud.tencent.com/developer/article/1742156
        AnimationSet animationSet = new AnimationSet(true);

        Animation[] animations = new Animation[] {
                new ScaleAnimation(1.4f,
                                   1.8f,
                                   1.4f,
                                   1.8f,
                                   ScaleAnimation.RELATIVE_TO_SELF,
                                   0.5f,
                                   ScaleAnimation.RELATIVE_TO_SELF,
                                   0.5f),
                new AlphaAnimation(0.5f, 0.1f),
                new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF,
                                       0,
                                       TranslateAnimation.RELATIVE_TO_SELF,
                                       0,
                                       TranslateAnimation.RELATIVE_TO_SELF,
                                       0,
                                       TranslateAnimation.RELATIVE_TO_SELF,
                                       -0.5f)
        };
        for (Animation animation : animations) {
            animation.setDuration(1000);
            animation.setRepeatCount(ValueAnimator.INFINITE);

            animationSet.addAnimation(animation);
        }

        this.itemView.startAnimation(animationSet);
    }

    private void touchDown(ImageView view) {
        if (view == null || view.getDrawable() == null) {
            return;
        }

        // https://stackoverflow.com/a/14483533
        // overlay is black with transparency
        view.getDrawable().setColorFilter(0x88000000, PorterDuff.Mode.SRC_ATOP);
        view.invalidate();
    }

    private void touchUp(ImageView view) {
        if (view == null || view.getDrawable() == null) {
            return;
        }

        // https://stackoverflow.com/a/14483533
        // clear the overlay
        view.getDrawable().clearColorFilter();
        view.invalidate();
    }
}
