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

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputClip;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputCharsInputPopupShowMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputClipMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputClipMsgData;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Save_As_Favorite;

/**
 * 提示面板视图
 * <p/>
 * 负责按键提示、快捷输入列表的显示
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-19
 */
public class PopupboardView extends BaseMsgListenerView {
    private final InputQuickListView quickListView;
    private final TextView tooltipView;
    private final View snackbarView;

    public PopupboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // Note: 所布局的视图将作为当前视图的子视图插入，而不会替换当前视图
        inflate(context, R.layout.ime_board_popup_root_view, this);

        this.quickListView = findViewById(R.id.quick_list);
        this.tooltipView = findViewById(R.id.tooltip);
        this.snackbarView = findViewById(R.id.snackbar);

        this.quickListView.setListener(this);
    }

    public boolean shouldVisible() {
        return ViewUtils.isVisible(this.tooltipView) //
               || ViewUtils.isVisible(this.snackbarView) //
               || ViewUtils.isVisible(this.quickListView);
    }

    // =============================== Start: 视图更新 ===================================

    /** 由上层视图调用以更新布局方向 */
    public void updateLayoutDirection() {
        Keyboard.HandMode handMode = this.config.get(ConfigKey.hand_mode);

        ViewUtils.updateLayoutDirection(this, handMode, true);
        // Note: Snackbar 按普通的方式调整布局方向
        ViewUtils.updateLayoutDirection(this.snackbarView, handMode, false);
    }

    // =============================== End: 视图更新 ===================================

    // =============================== Start: 消息处理 ===================================

    /** 响应 {@link InputMsg} 消息：向下传递消息给内部视图 */
    @Override
    public void onMsg(InputMsg msg) {
        // Note: 快捷输入没有确定的隐藏时机，故而，需针对每个消息做一次处理，在数据为 null 时隐藏，有数据时显示
        showInputQuickPopupWindow(msg.inputQuickList);

        switch (msg.type) {
            case InputChars_Input_Popup_Show_Doing: {
                InputCharsInputPopupShowMsgData data = msg.data();
                showInputKeyTip(data.text, data.hideDelayed);
                break;
            }
            case InputChars_Input_Popup_Hide_Doing: {
                showInputKeyTip(null, false);
                break;
            }
            case InputFavorite_Save_Done: {
                showTooltip(EditorAction.favorite.tipResId);
                break;
            }
            case InputFavorite_Paste_Done: {
                showTooltip(EditorAction.paste.tipResId);
                break;
            }
            case Editor_Edit_Doing: {
                EditorEditMsgData data = msg.data();
                on_Editor_Edit_Doing_Msg(data.action);
                break;
            }
            case InputClip_CanBe_Favorite: {
                on_InputClip_CanBe_Favorite_Msg(msg.data());
                break;
            }
            default: {
                this.log.warn("Ignore message %s", () -> new Object[] { msg.type });
            }
        }
    }

    private void on_InputClip_CanBe_Favorite_Msg(InputClipMsgData data) {
        dismiss(this.snackbarView);

        TextView textView = this.snackbarView.findViewById(R.id.text);
        textView.setText(data.source.confirmResId);

        Button actionBtn = this.snackbarView.findViewById(R.id.action);
        actionBtn.setText(R.string.btn_save_as_favorite);
        actionBtn.setOnClickListener((v) -> saveClipToFavorite(data.clip));

        show(this.snackbarView, 5000);
    }

    private void on_Editor_Edit_Doing_Msg(EditorAction action) {
        // 对编辑内容的操作做气泡提示，以告知用户处理结果，避免静默处理造成的困惑
        // Note: 复制和粘贴可能内容为空，不会提示可收藏，因此，仍需提示
        switch (action) {
            case backspace: // 已作气泡提示
            case favorite: { // 有专门的收藏确认提示
                break;
            }
            default: {
                showTooltip(action.tipResId);
                break;
            }
        }
    }

    private void saveClipToFavorite(InputClip clip) {
        UserInputClipMsgData data = new UserInputClipMsgData(clip);
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Save_As_Favorite).data(data));

        onMsg(msg);
    }

    // =============================== End: 消息处理 ===================================

    // ==================== Start: 气泡提示 ==================

    private void showInputQuickPopupWindow(List<?> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            dismiss(this.quickListView);
            return;
        }

        this.quickListView.update(dataList);

        show(this.quickListView, 0);
    }

    private void showInputKeyTip(String key, boolean hideDelayed) {
        if (this.config.bool(ConfigKey.disable_input_key_popup_tips)) {
            showTooltip(null, false);
            return;
        }

        showTooltip(key, hideDelayed);
    }

    private void showTooltip(int tipResId) {
        String tips = tipResId != 0 ? getContext().getString(tipResId) : null;

        showTooltip(tips, true);
    }

    private void showTooltip(String tip, boolean hideDelayed) {
        if (CharUtils.isBlank(tip)) {
            dismiss(this.tooltipView);
            return;
        }

        this.tooltipView.setText(tip);

        show(this.tooltipView, hideDelayed ? 600 : 0);
    }

    // ==================== End: 气泡提示 ==================

    private void dismiss(View view) {
        // TODO 动画渐入
        ViewUtils.hide(view);
    }

    private void show(View view, long closeDelayMillis) {
        // TODO 动画渐出
        ViewUtils.show(view);

        if (closeDelayMillis > 0) {
            postDelayed(() -> dismiss(view), closeDelayMillis);
        }
    }
}
