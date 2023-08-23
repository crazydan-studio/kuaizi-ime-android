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

package org.crazydan.studio.app.ime.kuaizi.internal;

/**
 * {@link Key 按键}配色
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-23
 */
public class KeyColor {
    /** 前景色资源 id */
    public final int fg;
    /** 背景色资源 id */
    public final int bg;

    private KeyColor(int fg, int bg) {
        this.fg = fg;
        this.bg = bg;
    }

    public static KeyColor create(int fg, int bg) {
        return new KeyColor(fg, bg);
    }

    public static KeyColor none() {
        return create(-1, -1);
    }
}
