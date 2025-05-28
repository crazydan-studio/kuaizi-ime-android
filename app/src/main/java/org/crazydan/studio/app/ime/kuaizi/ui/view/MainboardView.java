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
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewClosable;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.ui.view.key.XPadKeyViewHolder;
import org.crazydan.studio.app.ime.kuaizi.ui.view.xpad.XPadView;

/**
 * 主面板视图
 * <p/>
 * 由 {@link KeyboardView} 和 {@link InputboardView} 组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class MainboardView extends BaseMsgListenerView implements ViewClosable {
    protected final Logger log = Logger.getLogger(getClass());

    private final TextView warningView;
    private final KeyboardView keyboardView;
    private final InputboardView inputboardView;

    private boolean needToAddBottomSpacing;

    public MainboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // Note: 所布局的视图将作为当前视图的子视图插入，而不会替换当前视图
        inflate(context, R.layout.ime_board_main_view, this);

        this.warningView = findViewById(R.id.warning);
        this.keyboardView = findViewById(R.id.keyboard);
        this.keyboardView.setListener(this);

        this.inputboardView = findViewById(R.id.inputboard);
        this.inputboardView.setListener(this);

        // 监听系统导航高度的变化以添加底部空白，避免系统导航遮挡键盘
        setOnApplyWindowInsetsListener((v, insets) -> {
            int navBarHeight = insets.getStableInsetBottom();
            if (!needBottomSpacing()) {
                setBottomSpacing(navBarHeight);
            }

            return insets;
        });
    }

    @Override
    public void setConfig(Config config) {
        super.setConfig(config);

        this.keyboardView.setConfig(this.config);
        this.inputboardView.setConfig(this.config);

        // 做一次强制更新
        updateBottomSpacing(true);
    }

    public XPadView getXPadView() {
        XPadKeyViewHolder holder = this.keyboardView.getXPadKeyViewHolder();
        return holder != null ? holder.getXPad() : null;
    }

    @Override
    public void update() {
        this.inputboardView.update();

        updateBottomSpacing(false);
    }

    @Override
    public void close() {
    }

    // =============================== Start: 消息处理 ===================================

    /** 响应 {@link InputMsg} 消息：向下传递消息给内部视图 */
    @Override
    public void onMsg(InputMsg msg) {
        this.log.beginTreeLog("Handle %s", () -> new Object[] { msg.getClass() }) //
                .debug("Message Type: %s", () -> new Object[] { msg.type }) //
                .debug("Message Data: %s", () -> new Object[] { msg.data() });

        handleMsg(msg);

        this.log.endTreeLog();

        // Note: 涉及重建视图的情况，因此，需在最后转发消息到子视图
        this.keyboardView.onMsg(msg);
        this.inputboardView.onMsg(msg);
    }

    private void handleMsg(InputMsg msg) {
        switch (msg.type) {
            case Keyboard_Start_Doing: {
                toggleShowKeyboardWarning(true);
                break;
            }
            case Keyboard_Start_Done: {
                toggleShowKeyboardWarning(false);
                break;
            }
            default: {
                this.log.warn("Ignore message %s", () -> new Object[] { msg.type });
            }
        }
    }

    // =============================== End: 消息处理 ===================================

    // =============================== Start: 视图更新 ===================================

    private void updateBottomSpacing(boolean force) {
        // Note: 仅竖屏模式下需要添加底部空白
        boolean needSpacing = needBottomSpacing();
        if (!force && this.needToAddBottomSpacing == needSpacing) {
            return;
        }
        this.needToAddBottomSpacing = needSpacing;

        float height = getBottomSpacingHeight();
        setBottomSpacing(needSpacing ? height : -1);
    }

    private void toggleShowKeyboardWarning(boolean shown) {
        ViewUtils.visible(this.keyboardView, !shown);
        ViewUtils.visible(this.warningView, shown);
    }

    private boolean needBottomSpacing() {
        return this.config.bool(ConfigKey.adapt_desktop_swipe_up_gesture)
               && !this.config.bool(ConfigKey.enable_x_input_pad)
               && this.config.get(ConfigKey.orientation) == Keyboard.Orientation.portrait;
    }

    private void setBottomSpacing(float height) {
        height -= this.keyboardView.getBottomSpacing();
        height = Math.max(0, height);

        float origHeight = ScreenUtils.pxFromDimension(getContext(), R.dimen.keyboard_view_height);
        View parent = (View) this.keyboardView.getParent();

        ViewUtils.setHeight(parent, (int) (origHeight + height));

        this.keyboardView.updateGridBottomReservedHeight(height);
    }

    private float getBottomSpacingHeight() {
        Resources resources = getResources();

        // 优先选取系统导航的高度
        try {
            int resId = resources.getIdentifier("navigation_bar_height", "dimen", "android");

            return resources.getDimensionPixelSize(resId);
        } catch (Exception ignore) {
            return ScreenUtils.pxFromDimension(getContext(), R.dimen.keyboard_bottom_spacing);
        }
    }

    // =============================== End: 视图更新 ===================================
}
