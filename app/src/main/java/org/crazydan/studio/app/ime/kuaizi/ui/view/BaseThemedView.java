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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.ConfigUpdateMsgData;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-19
 */
public abstract class BaseThemedView extends BaseMsgListenerView {
    private final int layoutResId;
    private final boolean reverseLayoutDirection;

    public BaseThemedView(
            @NonNull Context context, @Nullable AttributeSet attrs, int layoutResId, boolean reverseLayoutDirection
    ) {
        super(context, attrs);

        this.layoutResId = layoutResId;
        this.reverseLayoutDirection = reverseLayoutDirection;
    }

    @Override
    public void setConfig(Config config) {
        super.setConfig(config);

        doLayout();
        updateLayoutDirection();
    }

    /** 布局视图 */
    protected void doLayout() {
        // 必须先清除已有的子视图，否则，重复 inflate 会无法即时生效
        removeAllViews();

        Context context = getContext();
        Keyboard.Theme theme = this.config.get(ConfigKey.theme);
        int themeResId = theme.getResId(context);

        // 通过 Context Theme 仅对键盘自身的视图设置主题样式，
        // 以避免通过 AppCompatDelegate.setDefaultNightMode 对配置等视图造成影响
        // Note:
        // - 其内部的视图也会按照主题样式进行更新，无需在子视图中单独处理
        // - 所布局的视图将作为当前视图的子视图插入，而不会替换当前视图
        ThemeUtils.inflate(this, this.layoutResId, themeResId, true);
    }

    // =============================== Start: 消息处理 ===================================

    /** 响应 {@link InputMsg} 消息：向下传递消息给内部视图 */
    @Override
    public void onMsg(InputMsg msg) {
        this.log.beginTreeLog("Handle %s", () -> new Object[] { msg.getClass() }) //
                .debug("Message Type: %s", () -> new Object[] { msg.type }) //
                .debug("Message Data: %s", () -> new Object[] { msg.data() });

        if (!handleInputMsg(msg)) {
            handleMsg(msg);
        }

        this.log.endTreeLog();
    }

    /** 仅处理除主题变更以外的消息 */
    protected abstract void handleMsg(InputMsg msg);

    private boolean handleInputMsg(InputMsg msg) {
        switch (msg.type) {
            case Config_Update_Done: {
                ConfigUpdateMsgData data = msg.data();
                switch (data.configKey) {
                    // 主题变更，必须重建视图
                    case theme: {
                        doLayout();
                        return true;
                    }
                    case hand_mode: {
                        updateLayoutDirection();
                        return true;
                    }
                }
                break;
            }
            case Keyboard_HandMode_Switch_Done: {
                updateLayoutDirection();
                return true;
            }
        }
        return false;
    }

    // =============================== End: 消息处理 ===================================

    /** 若当前为左手模式，则采用从右到左的布局方向，否则，采用从左到右的布局方向 */
    protected void updateLayoutDirection() {
        updateLayoutDirection(this, this.reverseLayoutDirection);
    }

    protected void updateLayoutDirection(View view, boolean reverse) {
        Keyboard.HandMode handMode = this.config.get(ConfigKey.hand_mode);

        ViewUtils.updateLayoutDirection(view, handMode, reverse);
    }
}
