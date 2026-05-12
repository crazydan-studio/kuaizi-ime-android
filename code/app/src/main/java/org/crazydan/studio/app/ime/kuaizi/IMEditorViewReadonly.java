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

package org.crazydan.studio.app.ime.kuaizi;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.ui.view.CandidatesView;

/**
 * 只读的 {@link IMEditorView}
 * <p/>
 * 只做布局和样式展示，不响应交互
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-16
 */
public class IMEditorViewReadonly extends IMEditorView {

    public IMEditorViewReadonly(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected CandidatesView initCandidatesView() {
        // 只读视图不显示候选窗口
        return null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 禁止传递事件，以确保输入键盘不能接收到事件，
        // 也就不能响应用户操作，变成为只读键盘了
        return true;
    }
}
