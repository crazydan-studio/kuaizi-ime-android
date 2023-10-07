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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.TextView;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

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

    public static boolean isVisible(View view) {
        return view.getVisibility() == View.VISIBLE;
    }

    public static void setHeight(View view, int height) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = height;
    }

    public static int getBackgroundColor(View view) {
        Drawable background = view.getBackground();

        // https://stackoverflow.com/questions/14779259/get-background-color-of-a-layout#answer-14779461
        if (background instanceof ColorDrawable) {
            return ((ColorDrawable) background).getColor();
        }
        return Color.TRANSPARENT;
    }

    public static void setHtmlText(TextView view, String text) {
        // https://developer.android.com/guide/topics/resources/string-resource#StylingWithHTML
        Spanned html = parseHtml(text);
        view.setText(html);

        // https://stackoverflow.com/questions/4438713/android-html-in-textview-with-link-clickable#answer-8722574
        view.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static Spanned parseHtml(String text) {
        text = text.replaceAll("(?m)(^\\s+|\\s+$)", "").replaceAll("\n", "");

        // https://developer.android.com/guide/topics/resources/string-resource#StylingWithHTML
        return Html.fromHtml(text, FROM_HTML_MODE_COMPACT);
    }

    /**
     * 将指定视图转换为指定尺寸的 {@link Drawable} 位图
     * <p/>
     * 注：若 <code>view</code> 为 layout 资源视图，
     * 则其必须为已被主窗口布局的视图，不能为动态添加的视图
     */
    public static Drawable toDrawable(View view, int width, int height) {
        if (view == null) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Note：必须调用 layout 进行布局，否则，其不会绘制视图
        view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        view.draw(canvas);

        Drawable drawable = new BitmapDrawable(null, bitmap);
        // Note：该边界设置会让图形按指定尺寸进行缩放
        drawable.setBounds(0, 0, width, height);

        return drawable;
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
