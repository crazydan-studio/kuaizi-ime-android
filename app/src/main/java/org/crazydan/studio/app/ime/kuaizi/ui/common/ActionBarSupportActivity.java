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

package org.crazydan.studio.app.ime.kuaizi.ui.common;

import java.util.Locale;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.Toast;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-07
 */
public abstract class ActionBarSupportActivity extends AppCompatActivity {
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        if (isActionBarEnabled()) {
            prepareActionBar();
        } else if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    /** 点击顶部的返回按钮的响应动作 */
    @Override
    public boolean onSupportNavigateUp() {
        if (isActionBarEnabled()) {
            onBackPressed();
        }
        return true;
    }

    protected void prepareActionBar() {
        // https://medium.com/swlh/how-to-create-custom-appbar-actionbar-toolbar-in-android-studio-java-61907fa1e44
        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(this.toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // 标题居中控制：设置自定义视图的文本内容，并禁用 toolbar 的默认标题
        // https://stackoverflow.com/questions/26533510/android-toolbar-center-title-and-custom-font#answer-38175403
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        TextView titleView = this.toolbar.findViewById(R.id.toolbar_title);
        titleView.setText(getTitle());
    }

    protected Toolbar getToolbar() {
        return this.toolbar;
    }

    protected boolean isActionBarEnabled() {
        return true;
    }

    /**
     * 采用 SnackBar 显示提示信息
     * <p/>
     * 仅针对应用内显示的提示信息
     */
    protected void toast(String msg, Object... args) {
        String text = String.format(Locale.getDefault(), msg, args);

        Toast.with(getToolbar()).setHtml(text).setDuration(Toast.LENGTH_LONG).show();
    }

    /**
     * 采用 {@link android.widget.Toast Toast} 显示提示信息
     * <p/>
     * 适用于全局提示，可确保在窗口退出后依然能够显示提示信息
     */
    protected void alert(String msg, Object... args) {
        String text = String.format(Locale.getDefault(), msg, args);

        android.widget.Toast.makeText(getApplicationContext(),
                                      Html.fromHtml(text, FROM_HTML_MODE_COMPACT),
                                      android.widget.Toast.LENGTH_LONG).show();
    }
}
