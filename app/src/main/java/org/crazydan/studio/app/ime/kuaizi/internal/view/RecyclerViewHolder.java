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

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.internal.ViewData;
import org.crazydan.studio.app.ime.kuaizi.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public abstract class RecyclerViewHolder<T extends ViewData> extends RecyclerView.ViewHolder {
    private T data;

    public RecyclerViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public T getData() {
        return this.data;
    }

    public void bind(T data) {
        this.data = data;
    }

    public boolean isHidden() {
        return this.itemView.getVisibility() == View.GONE;
    }

    public final Context getContext() {
        return this.itemView.getContext();
    }

    public void disable() {
        setAlpha(0.4f);
    }

    public void enable() {
        setAlpha(1.0f);
    }

    public void setTextColorByAttrId(TextView text, Integer attrId) {
        if (attrId != null) {
            int color = getColorByAttrId(attrId);
            text.setTextColor(color);
        }
    }

    public void setBackgroundColorByAttrId(View view, Integer attrId) {
        if (attrId != null) {
            int color = getColorByAttrId(attrId);
            view.setBackgroundColor(color);
        }
    }

    public int getColorByAttrId(int attrId) {
        return ThemeUtils.getColorByAttrId(getContext(), attrId);
    }

    public String getStringByAttrId(int attrId) {
        return ThemeUtils.getStringByAttrId(getContext(), attrId);
    }

    public void touchDown() {
        setScale(0.9f);
        disable();
    }

    public void touchUp() {
        enable();
        setScale(1.0f);
    }

    public void setAlpha(float alpha) {
        // Note: RecyclerView 在显示子项目时，
        // 会通过 DefaultItemAnimator 动画将 this.itemView 的透明度设置为 1，
        // 故而，只能对 this.itemView 中的全部子视图修改透明度，从而实现对其的启用/禁用效果
        // https://stackoverflow.com/questions/8395168/android-get-children-inside-a-view
        for (int i = 0; i < ((ViewGroup) this.itemView).getChildCount(); i++) {
            View child = ((ViewGroup) this.itemView).getChildAt(i);
            child.setAlpha(alpha);
        }
    }

    public void setScale(float scale) {
        setScale(this.itemView, scale);
    }

    public static void setScale(View view, float scale) {
        view.setScaleX(scale);
        view.setScaleY(scale);
    }

    /** 背景色渐隐/显动画 */
    public void fadeBackgroundColor(View view, int fromAttrId, int toAttrId) {
        int fromColor = getColorByAttrId(fromAttrId);
        int toColor = getColorByAttrId(toAttrId);

        if (ViewUtils.getBackgroundColor(view) == toColor) {
            return;
        }

        // https://stackoverflow.com/questions/5200811/in-android-how-do-i-smoothly-fade-the-background-from-one-color-to-another-ho#answer-14282231
        ObjectAnimator fade = ObjectAnimator.ofObject(view, "backgroundColor", new ArgbEvaluator(), fromColor, toColor);
        fade.setDuration(500);
        fade.start();
    }
}
