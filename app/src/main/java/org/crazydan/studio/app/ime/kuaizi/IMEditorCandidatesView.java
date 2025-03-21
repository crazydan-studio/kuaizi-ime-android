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

package org.crazydan.studio.app.ime.kuaizi;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputClip;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputCharsInputPopupShowMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputClipMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputClipMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.BaseThemedView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputQuickListView;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Save_As_Favorite;

/**
 * {@link IMEditor} 的输入候选视图
 * <p/>
 * 负责按键提示、快捷输入列表的显示
 * <p/>
 * 与 {@link IMEditorView} 分离显示
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-19
 */
public class IMEditorCandidatesView extends BaseThemedView {
    public interface OnShowListener {
        void onShow(boolean shown);
    }

    private InputQuickListView quickListView;
    private TextView tooltipView;
    private View snackbarView;

    private OnShowListener onShowListener;
    private final Runnable dismissSnackbarCb;

    public IMEditorCandidatesView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, R.layout.ime_candidates_root_view, true);

        this.dismissSnackbarCb = () -> snackbarDimiss(this.snackbarView);
    }

    public void setOnShowListener(OnShowListener onShowListener) {
        this.onShowListener = onShowListener;
    }

    public void close() {
        showInputQuickPopupWindow(null);
        showTooltip(null, false);
    }

    // =============================== Start: 视图更新 ===================================

    @Override
    protected void doLayout() {
        super.doLayout();

        this.quickListView = findViewById(R.id.quick_list);
        this.tooltipView = findViewById(R.id.tooltip);
        this.snackbarView = findViewById(R.id.snackbar);

        this.quickListView.setListener(this);
    }

    @Override
    protected void updateLayoutDirection() {
        super.updateLayoutDirection();

        // Note: Snackbar 按普通的方式调整布局方向
        updateLayoutDirection(this.snackbarView, false);
    }

    public boolean shouldVisible() {
        return ViewUtils.isVisible(this.tooltipView) //
               || ViewUtils.isVisible(this.snackbarView) //
               || ViewUtils.isVisible(this.quickListView);
    }

    // =============================== End: 视图更新 ===================================

    // =============================== Start: 消息处理 ===================================

    @Override
    protected void handleMsg(InputMsg msg) {
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

        this.onShowListener.onShow(shouldVisible());
    }

    private void on_InputClip_CanBe_Favorite_Msg(InputClipMsgData data) {
        removeCallbacks(this.dismissSnackbarCb);

        TextView textView = this.snackbarView.findViewById(R.id.text);
        textView.setText(data.source.confirmResId);

        Button actionBtn = this.snackbarView.findViewById(R.id.action);
        actionBtn.setText(R.string.btn_save_as_favorite);
        actionBtn.setOnClickListener((v) -> saveClipToFavorite(data.clip));

        snackbarShow(this.snackbarView);
        postDelayed(this.dismissSnackbarCb, 5000);
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
        this.quickListView.update(dataList == null ? new ArrayList<>() : dataList);

        if (CollectionUtils.isEmpty(dataList)) {
            dismiss(this.quickListView);
        } else {
            show(this.quickListView, 0);
        }
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

    private void snackbarShow(View view) {
        Context context = view.getContext();
        int animResId = ThemeUtils.getResourceByAttrId(context, R.attr.anim_slide_in);
        Animation anim = AnimationUtils.loadAnimation(context, animResId);

        view.startAnimation(anim);
        ViewUtils.show(view);
    }

    private void snackbarDimiss(View view) {
        // Note: 从当前子视图的主题上下文中查找动画资源
        Context context = view.getContext();
        int animResId = ThemeUtils.getResourceByAttrId(context, R.attr.anim_slide_out);
        Animation anim = AnimationUtils.loadAnimation(context, animResId);

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                anim.setAnimationListener(null);
                ViewUtils.hide(view);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        view.startAnimation(anim);
    }
}
