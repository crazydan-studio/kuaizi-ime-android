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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.view;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.utils.ResourceUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-24
 */
public class Alert {
    private final Context context;

    private View view;
    private CharSequence title;
    private CharSequence message;
    private Button negativeBtn;
    private Button positiveBtn;

    public static Alert with(Activity context) {
        return new Alert(context);
    }

    private Alert(Context context) {
        this.context = context;
    }

    public Alert setView(int resId) {
        View view = LayoutInflater.from(this.context).inflate(resId, null);
        return setView(view);
    }

    public Alert setView(View view) {
        this.view = view;
        return this;
    }

    public Alert setTitle(int resId, Object... args) {
        String title = this.context.getString(resId, args);
        return setTitle(title.trim());
    }

    public Alert setTitle(CharSequence title) {
        this.title = title;
        return this;
    }

    public Alert setMessage(int resId, Object... args) {
        String message = this.context.getString(resId, args);
        return setMessage(message.trim());
    }

    public Alert setRawMessage(int rawResId, Object... args) {
        String message = ResourceUtils.readString(this.context, rawResId, args);
        return setMessage(message.trim());
    }

    public Alert setMessage(CharSequence message) {
        this.message = message;
        return this;
    }

    public Alert setNegativeButton(int resId, DialogInterface.OnClickListener listener) {
        this.negativeBtn = new Button(this.context.getString(resId), listener);
        return this;
    }

    public Alert setPositiveButton(int resId, DialogInterface.OnClickListener listener) {
        this.positiveBtn = new Button(this.context.getString(resId), listener);
        return this;
    }

    public void show() {
        TextView titleView = this.view.findViewById(R.id.title_view);
        TextView messageView = this.view.findViewById(R.id.message_view);
        android.widget.Button negativeBtnView = this.view.findViewById(R.id.negative_btn_view);
        android.widget.Button positiveBtnView = this.view.findViewById(R.id.positive_btn_view);

        titleView.setText(this.title);
        if (this.message instanceof Spanned) {
            messageView.setText(this.message);
        } else {
            messageView.setText(ViewUtils.parseHtml(this.message.toString()));
        }

        // Note: AlertDialog 的 context 必须为 activity，不能是应用的 context
        // https://stackoverflow.com/questions/27087983/unable-to-add-window-token-null-is-not-valid-is-your-activity-running#answer-50716727
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this.context);

        AlertDialog alert = builder.setView(this.view).setCancelable(false).create();
        alert.show();

        negativeBtnView.setText(this.negativeBtn.text);
        negativeBtnView.setOnClickListener((v) -> {
            this.negativeBtn.listener.onClick(alert, DialogInterface.BUTTON_NEGATIVE);
            alert.dismiss();
        });
        positiveBtnView.setText(this.positiveBtn.text);
        positiveBtnView.setOnClickListener((v) -> {
            this.positiveBtn.listener.onClick(alert, DialogInterface.BUTTON_POSITIVE);
            alert.dismiss();
        });
    }

    private static class Button {
        private final String text;
        private final DialogInterface.OnClickListener listener;

        private Button(String text, DialogInterface.OnClickListener listener) {
            this.text = text;
            this.listener = listener;
        }
    }
}
