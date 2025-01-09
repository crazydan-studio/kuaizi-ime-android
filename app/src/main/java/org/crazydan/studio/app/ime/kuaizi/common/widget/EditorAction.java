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

package org.crazydan.studio.app.ime.kuaizi.common.widget;

/**
 * 编辑器的编辑动作
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-14
 */
public enum EditorAction {
    /** 回删 */
    backspace("回删"),
    /** 全选 */
    select_all("全选"),
    /** 复制 */
    copy("复制"),
    /** 粘贴 */
    paste("粘贴"),
    /** 剪切 */
    cut("剪切"),
    /** 撤销 */
    undo("撤销"),
    /** 重做 */
    redo("重做"),
    ;

    public final String label;

    EditorAction(String label) {
        this.label = label;
    }

    /** 检查指定的编辑动作是否会造成内容修改 */
    public static boolean hasEffect(EditorAction action) {
        switch (action) {
            case select_all:
            case copy:
                return false;
        }
        return true;
    }
}
