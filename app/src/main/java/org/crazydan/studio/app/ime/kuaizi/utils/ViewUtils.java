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
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
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
import org.hexworks.mixite.core.api.HexagonOrientation;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-08
 */
public class ViewUtils {

    public static void visible(View view, boolean shown) {
        if (view != null) {
            view.setVisibility(shown ? View.VISIBLE : View.GONE);
        }
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

    public static Point getLocationInWindow(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);

        int x = location[0];
        int y = location[1];

        return new Point(x, y);
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

    /**
     * 获取正六边形顶点坐标
     *
     * @return 从最右上角顶点开始顺时针旋转一周所遇到的顶点
     */
    public static PointF[] createHexagon(HexagonOrientation orientation, PointF center, float radius) {
        return drawHexagon(null, orientation, center, radius);
    }

    /** 绘制正六边形，并返回其顶点坐标 */
    public static PointF[] drawHexagon(Path path, HexagonOrientation orientation, PointF center, float radius) {
        int vertexCount = 6;
        PointF[] vertexes = new PointF[vertexCount];

        for (int i = 0; i < vertexCount; i++) {
            int times = orientation == HexagonOrientation.FLAT_TOP //
                        ? 2 * i : 2 * i + 1;
            float radians = (float) Math.toRadians(30 * times);

            float x = (float) (center.x + radius * Math.cos(radians));
            float y = (float) (center.y + radius * Math.sin(radians));

            int vertexIndex = orientation == HexagonOrientation.FLAT_TOP //
                              ? i : (vertexCount + 1 + i) % vertexCount;
            vertexes[vertexIndex] = new PointF(x, y);

            if (path != null) {
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
        }

        if (path != null) {
            path.close();
        }

        return vertexes;
    }

    /** 为在 view 上绘制阴影做准备 */
    public static void prepareForShadow(View view) {
        // https://stackoverflow.com/questions/17410195/setshadowlayer-android-api-differences/17414651#17414651
        // https://developer.android.com/topic/performance/hardware-accel#determining
        // https://developer.android.com/topic/performance/hardware-accel#drawing-support
        // Note：在 API 28 以下版本中，若在未启用硬件加速的视图上通过 Drawable 画阴影（Paint.setShadowLayer），
        // 必须在视图上启用软件加速，否则，视图将会出现整体虚化，且阴影颜色也会使用其填充色而不是设置的颜色
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P //
            && !view.isHardwareAccelerated()) {
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    /** 判断指定的点是否在多边形内部 */
    public static boolean isPointInPolygon(PointF point, PointF[] polygon) {
        int intersectCount = 0;
        for (int i = 0; i < polygon.length; i++) {
            PointF p1 = polygon[i];
            PointF p2 = polygon[(i + 1) % polygon.length];

            if (point.y > Math.min(p1.y, p2.y)
                && point.y <= Math.max(p1.y, p2.y)
                && point.x <= Math.max(p1.x, p2.x)
                && p1.y != p2.y) {
                double xIntersection = (point.y - p1.y) * (p2.x - p1.x) / (p2.y - p1.y) + p1.x;

                if (p1.x == p2.x || point.x <= xIntersection) {
                    intersectCount++;
                }
            }
        }
        return intersectCount % 2 != 0;
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
