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

package org.crazydan.studio.app.ime.kuaizi.ui.guide;

import android.graphics.drawable.Drawable;
import org.crazydan.studio.app.ime.kuaizi.core.Key;

/**
 * {@link Key} 的图形渲染器
 * <p/>
 * 负责获取与实际输入按键相同的图形
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-19
 */
public interface KeyImageRender {

    /** 添加待渲染的 {@link Key}，并返回按键唯一标识 */
    String withKey(Key key);

    /** 根据 {@link #withKey} 所生成的唯一标识，渲染该标识对应的 {@link Key} 的图像 */
    Drawable renderKey(String code, int width, int height);
}
