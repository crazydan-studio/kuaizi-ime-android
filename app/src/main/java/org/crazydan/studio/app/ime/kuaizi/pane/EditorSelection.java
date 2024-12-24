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

package org.crazydan.studio.app.ime.kuaizi.pane;

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
    /** 选区的起点位置，已排序，其始终小于 {@link #end} */
    public final int start;
    /** 选区的起点位置，已排序，其始终大于 {@link #start} */
    public final int end;
    /** 选区原始的起点位置，反向选择时，该值将大于 {@link #origEnd} */
    public final int origStart;
    /** 选区原始的终点位置，反向选择时，该值将小于 {@link #origStart} */
    public final int origEnd;

    /** 在从 {@link #start} 至 {@link #end} 的选区范围内的已选中内容，其可能为空，即，未选中内容 */
    public final CharSequence content;

    public EditorSelection(int start, int end, int origStart, int origEnd, CharSequence content) {
        this.start = start;
        this.end = end;
        this.origStart = origStart;
        this.origEnd = origEnd;

        this.content = content;
    }

    public static EditorSelection from(InputConnection ic) {
        // https://stackoverflow.com/questions/40521324/selection-using-android-ime#answer-58778722
        // Note: ExtractedText#text 为当前编辑器的全部内容
        ExtractedText extractedText = ic.getExtractedText(new ExtractedTextRequest(), 0);

        int start = Math.min(extractedText.selectionStart, extractedText.selectionEnd);
        int end = Math.max(extractedText.selectionStart, extractedText.selectionEnd);

        return new EditorSelection(start,
                                   end,
                                   extractedText.selectionStart,
                                   extractedText.selectionEnd,
                                   ic.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES));
    }

    public static EditorSelection from(EditText editText) {
        Editable editable = editText.getText();

        int start = Math.min(editText.getSelectionStart(), editText.getSelectionEnd());
        int end = Math.max(editText.getSelectionStart(), editText.getSelectionEnd());

        return new EditorSelection(start,
                                   end,
                                   editText.getSelectionStart(),
                                   editText.getSelectionEnd(),
                                   editable.subSequence(start, end));
    }
}
