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

import java.util.function.BiFunction;

import android.text.Editable;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

/**
 * 编辑的选区信息，包含选区的起始位置以及被选中的{@link #content 内容}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-22
 */
public class EditorSelection {
    /** 选区的起点位置，其始终小于 {@link #end} */
    public final int start;
    /** 选区的终点位置，其始终大于 {@link #start} */
    public final int end;
    /** 是否为反向选择，即，实际的起点位置大于终点位置 */
    public final boolean reversed;

    /** 在从 {@link #start} 至 {@link #end} 的选区范围内的已选中内容，其可能为空，即，未选中内容 */
    public final CharSequence content;

    public EditorSelection(int start, int end, BiFunction<Integer, Integer, CharSequence> contentGetter) {
        this.start = Math.min(start, end);
        this.end = Math.max(start, end);
        this.reversed = start > end;

        this.content = contentGetter.apply(this.start, this.end);
    }

    public static EditorSelection from(InputConnection ic) {
        // https://stackoverflow.com/questions/40521324/selection-using-android-ime#answer-58778722
        // Note: ExtractedText#text 为当前编辑器的全部内容
        ExtractedText extractedText = ic.getExtractedText(new ExtractedTextRequest(), 0);

        return new EditorSelection(extractedText.selectionStart,
                                   extractedText.selectionEnd,
                                   (s, e) -> ic.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES));
    }

    public static EditorSelection from(EditText editText) {
        Editable editable = editText.getText();

        return new EditorSelection(editText.getSelectionStart(), editText.getSelectionEnd(), editable::subSequence);
    }

    /** 还原对 {@link EditorSelection} 选区的变更 */
    public static class ChangeRevertion {
        public final EditorSelection before;
        public final EditorSelection after;

        public ChangeRevertion(EditorSelection before, EditorSelection after) {
            this.before = before;
            this.after = after;
        }
    }
}
