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

package org.crazydan.studio.app.ime.kuaizi.utils;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-08
 */
public class ViewUtils {

    public static void visible(View view, boolean shown) {
        view.setVisibility(shown ? View.VISIBLE : View.GONE);
    }

    public static <T extends View> T show(T view) {
        visible(view, true);
        return view;
    }

    public static <T extends View> T hide(T view) {
        visible(view, false);
        return view;
    }

    public static void startAnimationOnce(View view, long duration, Animation... animations) {
        startAnimation(view, duration, 0, animations);
    }

    public static void startAnimationInfinite(View view, long duration, Animation... animations) {
        startAnimation(view, duration, ValueAnimator.INFINITE, animations);
    }

    public static void startAnimation(View view, long duration, int repeatCount, Animation... animations) {
        AnimationSet set = new AnimationSet(true);

        for (Animation animation : animations) {
            animation.setDuration(duration);
            animation.setRepeatCount(repeatCount);

            set.addAnimation(animation);
        }

        view.startAnimation(set);
    }

    public static void stopAnimation(View view) {
        view.clearAnimation();
    }
}
