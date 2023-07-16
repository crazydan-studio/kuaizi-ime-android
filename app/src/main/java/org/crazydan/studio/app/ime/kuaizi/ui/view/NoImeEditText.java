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

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;

/**
 * 禁用输入法的文本编辑器，以用于筷字输入法的功能演示
 * <p/>
 * 注：无法解决系统输入法闪现问题
 * <p/>
 * 代码来自: https://stackoverflow.com/questions/8997225/how-to-hide-android-soft-keyboard-on-edittext#answer-21480217
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-16
 */
public class NoImeEditText extends androidx.appcompat.widget.AppCompatEditText {

    public NoImeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * This method is called before keyboard appears when text is selected.
     * So just hide the keyboard
     */
    @Override
    public boolean onCheckIsTextEditor() {
        hideKeyboard();

        return super.onCheckIsTextEditor();
    }

    /**
     * This method is called when text selection is changed, so hide keyboard to prevent it to appear
     */
    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);

        hideKeyboard();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }
}
