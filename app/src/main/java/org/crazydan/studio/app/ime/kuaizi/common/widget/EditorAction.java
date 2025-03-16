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

import org.crazydan.studio.app.ime.kuaizi.R;

/**
 * 编辑器的编辑动作
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-14
 */
public enum EditorAction {
    /** 回删 */
    backspace("回删", 0),
    /** 全选 */
    select_all("全选", R.string.tip_editor_action_select_all),
    /** 复制 */
    copy("复制", R.string.tip_editor_action_copy),
    /** 粘贴 */
    paste("粘贴", R.string.tip_editor_action_paste),
    /** 剪切 */
    cut("剪切", R.string.tip_editor_action_cut),
    /** 撤销 */
    undo("撤销", R.string.tip_editor_action_undo),
    /** 重做 */
    redo("重做", R.string.tip_editor_action_redo),
    /**
     * 收藏
     * <p/>
     * 注意，对选中内容做收藏，需要先复制再监听剪贴板，最后还原剪贴板，过程比较复杂，
     * 可以通过复制操作触发收藏的方式添加收藏
     */
    favorite("收藏", R.string.tip_editor_action_favorite),
    ;

    public final String label;
    public final int tipResId;

    EditorAction(String label, int tipResId) {
        this.label = label;
        this.tipResId = tipResId;
    }

    /** 是否为修改编辑器内容的操作 */
    public static boolean hasEditorEffect(EditorAction action) {
        switch (action) {
            case select_all:
            case favorite:
            case copy:
                return false;
        }
        return true;
    }

    /** 是否为修改剪贴板的操作 */
    public static boolean hasClipEffect(EditorAction action) {
        switch (action) {
            case copy:
            case cut:
                return true;
        }
        return false;
    }
}
