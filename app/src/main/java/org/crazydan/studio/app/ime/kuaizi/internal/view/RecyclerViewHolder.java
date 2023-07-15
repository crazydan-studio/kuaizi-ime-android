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

package org.crazydan.studio.app.ime.kuaizi.internal.view;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.utils.ColorUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public abstract class RecyclerViewHolder extends RecyclerView.ViewHolder {
    private final Handler animationCallbackHandler = new Handler();

    public RecyclerViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public boolean isHidden() {
        return this.itemView.getVisibility() == View.GONE;
    }

    public final Context getContext() {
        return this.itemView.getContext();
    }

    public void disable() {
        // Note: RecyclerView 在显示子项目时，
        // 会通过 DefaultItemAnimator 动画将 this.itemView 的透明度设置为 1，
        // 故而，只能对 this.itemView 中的全部子视图修改透明度，从而实现对其的启用/禁用效果
        // https://stackoverflow.com/questions/8395168/android-get-children-inside-a-view
        for (int i = 0; i < ((ViewGroup) this.itemView).getChildCount(); i++) {
            View child = ((ViewGroup) this.itemView).getChildAt(i);
            child.setAlpha(0.5f);
        }
    }

    public void enable() {
        for (int i = 0; i < ((ViewGroup) this.itemView).getChildCount(); i++) {
            View child = ((ViewGroup) this.itemView).getChildAt(i);
            child.setAlpha(1.0f);
        }
    }

    public void setTextColorByAttrId(TextView text, int attrId) {
        int color = getColorByAttrId(attrId);
        text.setTextColor(color);
    }

    public void setBackgroundColorByAttrId(View view, int attrId) {
        int color = getColorByAttrId(attrId);
        view.setBackgroundColor(color);
    }

    public int getColorByAttrId(int attrId) {
        return ColorUtils.getByAttrId(getContext(), attrId);
    }

    public void touchDown() {
        this.itemView.setAlpha(0.3f);
    }

    public void touchUp() {
        this.itemView.setAlpha(1.0f);
    }

    public void fadeOut(Runnable cb) {
        clearAnimation();
        this.itemView.setAlpha(1.0f);

        // 图形扩散淡化消失的效果
        // https://cloud.tencent.com/developer/article/1742156
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

        startAnimation(cb, animations, 500, 0);
    }

    private void startAnimation(Runnable cb, Animation[] animations, long duration, int repeatCount) {
        AnimationSet animationSet = new AnimationSet(true);
        for (Animation animation : animations) {
            animation.setDuration(duration);
            animation.setRepeatCount(repeatCount);

            animationSet.addAnimation(animation);
        }

        this.itemView.startAnimation(animationSet);

        if (repeatCount >= 0) {
            this.animationCallbackHandler.postDelayed(cb, (duration - 100) * (repeatCount + 1));
        }
    }

    private void clearAnimation() {
        this.itemView.clearAnimation();
    }
}
