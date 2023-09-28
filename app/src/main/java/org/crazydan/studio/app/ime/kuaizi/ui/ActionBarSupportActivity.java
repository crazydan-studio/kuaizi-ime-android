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

package org.crazydan.studio.app.ime.kuaizi.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import org.crazydan.studio.app.ime.kuaizi.R;

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
}
