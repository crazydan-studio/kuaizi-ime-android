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

package org.crazydan.studio.app.ime.kuaizi;

import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-29
 */
public class Service extends InputMethodService {
    private InputMethodManager inputMethodManager;

    private ContextThemeWrapper inputViewTheme;
    private ImeInputView inputView;
    private Keyboard.Type inputKeyboardType;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        this.inputViewTheme = new ContextThemeWrapper(getApplicationContext(), R.style.Theme_DayNight_KuaiziIME);
    }

    @Override
    public void onInitializeInterface() {

    }

    @Override
    public View onCreateInputView() {
        // 只能通过 Context Theme 设置输入视图的暗黑模式，
        // 而 AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) 方式无效
        // https://stackoverflow.com/questions/65433795/unable-to-update-the-day-and-night-modes-in-android-with-window-manager-screens#answer-67340930
        this.inputView = (ImeInputView) LayoutInflater.from(this.inputViewTheme).inflate(R.layout.ime_input_view, null);

        return this.inputView;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        InputMethodSubtype subtype = this.inputMethodManager.getCurrentInputMethodSubtype();
        onCurrentInputMethodSubtypeChanged(subtype);
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        Keyboard.Type keyboardType = this.inputKeyboardType;
        if (keyboardType == Keyboard.Type.Pinyin //
            && subtype != null //
            && ("en_US".equals(subtype.getLocale()) //
                || "en_US".equals(subtype.getLanguageTag()))) {
            keyboardType = Keyboard.Type.English;
        }
        this.inputView.keyboard.startInput(keyboardType);
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        Keyboard.Type keyboardType = Keyboard.Type.Pinyin;
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                keyboardType = Keyboard.Type.Number;
                break;
            case InputType.TYPE_CLASS_PHONE:
                keyboardType = Keyboard.Type.Phone;
                break;
        }

        this.inputKeyboardType = keyboardType;
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();

        this.inputView.keyboard.finishInput();
    }
}
