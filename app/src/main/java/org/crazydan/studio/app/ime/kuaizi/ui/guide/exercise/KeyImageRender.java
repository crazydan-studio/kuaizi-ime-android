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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise;

import android.graphics.drawable.Drawable;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;

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
    String withKey(Key<?> key);

    /** 根据 {@link #withKey} 所生成的唯一标识，渲染该标识对应的 {@link Key} 的图像 */
    Drawable renderKey(String code, int width, int height);
}
