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

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Clipboard;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;

/**
 * {@link Clipboard} 的视图
 * <p/>
 * 由 {@link InputFavoriteListView} 以及相关的功能按钮组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-13
 */
public class ClipboardView extends FrameLayout implements UserMsgListener, InputMsgListener {
    private InputListView inputListView;

    private Config config;
    private UserMsgListener listener;

    public ClipboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setConfig(Config config) {
        this.config = config;
        doLayout();
    }

    // =============================== Start: 消息处理 ===================================

    public void setListener(UserMsgListener listener) {
        this.listener = listener;
    }

    /** 响应内部视图的 {@link UserInputMsg} 消息：从视图向上传递给外部监听者 */
    @Override
    public void onMsg(UserInputMsg msg) {
        this.listener.onMsg(msg);
    }

    // -------------------------------------------

    /** 响应 {@link InputMsg} 消息：向下传递消息给内部视图 */
    @Override
    public void onMsg(InputMsg msg) {
        // Note: 本视图为上层视图的子视图，在主题样式更新时，上层视图会自动重建本视图，
        // 因此，不需要重复处理本视图的布局更新等问题

        handleMsg(msg);

        this.inputListView.onMsg(msg);
    }

    private void handleMsg(InputMsg msg) {
    }

    // =============================== End: 消息处理 ===================================

    // =============================== Start: 视图更新 ===================================

    /** 布局视图 */
    private void doLayout() {
        Keyboard.Theme theme = this.config.get(ConfigKey.theme);
        int themeResId = theme.getResId(getContext());

        View rootView = inflateWithTheme(R.layout.clipboard_root_view, themeResId);

        this.inputListView = rootView.findViewById(R.id.input_list);
        this.inputListView.setListener(this);
    }

    private <T extends View> T inflateWithTheme(int resId, int themeResId) {
        // 通过 Context Theme 仅对面板自身的视图设置主题样式，
        // 以避免通过 AppCompatDelegate.setDefaultNightMode 对配置等视图造成影响
        return ThemeUtils.inflate(this, resId, themeResId, true);
    }

    // =============================== End: 视图更新 ===================================

    // ==================== Start: 按键事件处理 ==================

    // ==================== End: 按键事件处理 ==================
}
