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

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Inputboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserEditorActionSingleTapMsgData;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Cancel_Clean_InputList;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Clean_InputList;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Close_Keyboard;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Editor_Action;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Open_Favoriteboard;

/**
 * {@link Inputboard} 的视图
 * <p/>
 * 由 {@link InputListView} 以及相关的功能按钮组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-06
 */
public class InputboardView extends BaseMsgListenerView {
    private final InputListView inputListView;

    private final BtnTools tools;

    private State state = new State(State.Type.Init);

    public InputboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // Note: 所布局的视图将作为当前视图的子视图插入，而不会替换当前视图
        inflate(context, R.layout.ime_board_input_view, this);

        int enterAnimResId = ThemeUtils.getResourceByAttrId(context, R.attr.anim_fade_in);
        Animation enterAnim = AnimationUtils.loadAnimation(context, enterAnimResId);
        this.tools = new BtnTools(enterAnim);

        this.inputListView = findViewById(R.id.input_list);
        this.inputListView.setListener(this);

        this.tools.addGroup(BtnGroupType.inputbar, findViewById(R.id.inputbar));
        this.tools.addGroup(BtnGroupType.toolbar, findViewById(R.id.toolbar));

        this.tools.showToolbar = new Btn(this, R.id.btn_show_toolbar, this::onShowToolbar);
        this.tools.hideToolbar = new Btn(this, R.id.btn_hide_toolbar, this::onHideToolbar);

        this.tools.settings = new Btn(this, R.id.btn_open_settings, this::onShowPreferences);
        this.tools.favoriteboard = new Btn(this, R.id.btn_open_favoriteboard, this::onOpenFavoriteboard);
        this.tools.switchIme = new Btn(this, R.id.btn_switch_ime, this::onSwitchIme);
        this.tools.closeKeyboard = new Btn(this, R.id.btn_close_keyboard, this::onCloseKeyboard);

        this.tools.editorCopy = new Btn(this, R.id.btn_editor_copy, this::onEditorCopy);
        this.tools.editorPaste = new Btn(this, R.id.btn_editor_paste, this::onEditorPaste);
        this.tools.editorCut = new Btn(this, R.id.btn_editor_cut, this::onEditorCut);
        this.tools.editorSelectAll = new Btn(this, R.id.btn_editor_select_all, this::onEditorSelectAll);
        this.tools.editorUndo = new Btn(this, R.id.btn_editor_undo, this::onEditorUndo);
        this.tools.editorRedo = new Btn(this, R.id.btn_editor_redo, this::onEditorRedo);

        this.tools.cleanInputList = new Btn(this, R.id.btn_clean_input_list, this::onCleanInputList);
        this.tools.cancelCleanInputList = new Btn(this, R.id.btn_cancel_clean_input_list, this::onCancelCleanInputList);
    }

    public void update() {
        updateToolsByState(false);
    }

    // =============================== Start: 消息处理 ===================================

    /** 响应 {@link InputMsg} 消息：向下传递消息给内部视图 */
    @Override
    public void onMsg(InputMsg msg) {
        handleMsg(msg);

        this.inputListView.onMsg(msg);
    }

    private void handleMsg(InputMsg msg) {
        switch (msg.type) {
            // 忽略不更改输入面板状态的消息
            case InputFavorite_Save_Done:
            case InputClip_CanBe_Favorite:
            case Editor_Cursor_Move_Doing:
            case Editor_Range_Select_Doing:
            case Editor_Edit_Doing:
            case InputAudio_Play_Doing:
            case InputChars_Input_Popup_Show_Doing:
            case InputChars_Input_Popup_Hide_Doing: {
                return;
            }
        }

        if (msg.inputList.frozen) {
            this.state = new State(State.Type.Input_Freeze_Doing);
        } else if (!msg.inputList.empty) {
            this.state = new State(State.Type.Input_Doing);
        } else if (msg.inputList.canCancelClean) {
            this.state = new State(State.Type.Input_Cleaned_Cancel_Waiting);
        } else {
            this.state = new State(State.Type.Init);
        }

        updateToolsByState(false);
    }

    /**
     * 根据输入面板状态更新工具状态
     * <p/>
     * 仅显示和隐藏工具栏时，才需要动画效果
     */
    private void updateToolsByState(boolean animation) {
        this.tools.reset();

        this.tools.favoriteboard.disabled = false;
        this.tools.settings.disabled = this.config.bool(ConfigKey.disable_settings_btn);
        this.tools.switchIme.disabled = this.config.bool(ConfigKey.disable_switch_ime_btn);
        this.tools.closeKeyboard.disabled = this.config.bool(ConfigKey.disable_close_keyboard_btn);

        switch (this.state.type) {
            case Init: {
                this.tools.activeGroup(BtnGroupType.toolbar);

                this.tools.switchIme.shown = true;
                this.tools.closeKeyboard.shown = true;
                break;
            }
            case Input_Freeze_Doing: {
                this.tools.activeGroup(BtnGroupType.inputbar);

                this.tools.switchIme.shown = true;
                this.tools.closeKeyboard.shown = true;
                break;
            }
            case Input_Doing: {
                this.tools.activeGroup(BtnGroupType.inputbar);

                this.tools.showToolbar.shown = true;
                this.tools.cleanInputList.shown = true;
                break;
            }
            case Input_Cleaned_Cancel_Waiting: {
                this.tools.activeGroup(BtnGroupType.inputbar);

                this.tools.showToolbar.shown = true;
                this.tools.cancelCleanInputList.shown = true;
                break;
            }
            case Toolbar_Show_Doing: {
                this.tools.activeGroup(BtnGroupType.toolbar);

                this.tools.hideToolbar.shown = true;
                break;
            }
        }

        this.tools.update(animation);
    }

    // =============================== End: 消息处理 ===================================

    // ==================== Start: 按键事件处理 ==================

    private static void toggleDisableBtn(View btn, boolean disabled, View.OnClickListener listener) {
        if (disabled) {
            btn.setAlpha(0.4f);
            btn.setOnClickListener(null);
        } else {
            btn.setAlpha(1.0f);
            btn.setOnClickListener(listener);
        }
    }

    private static void toggleShowBtn(View btn, boolean shown, View.OnClickListener listener) {
        if (shown) {
            ViewUtils.show(btn);
            btn.setOnClickListener(listener);
        } else {
            ViewUtils.hide(btn);
            btn.setOnClickListener(null);
        }
    }

    private void onShowToolbar(View v) {
        this.state = new State(State.Type.Toolbar_Show_Doing, this.state);
        updateToolsByState(true);
    }

    private void onHideToolbar(View v) {
        this.state = this.state.prev;
        updateToolsByState(true);
    }

    private void onShowPreferences(View v) {
        SystemUtils.showAppPreferences(getContext());
    }

    private void onSwitchIme(View v) {
        SystemUtils.switchIme(getContext());
    }

    private void onCleanInputList(View v) {
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Clean_InputList));
        onMsg(msg);
    }

    private void onCancelCleanInputList(View v) {
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Cancel_Clean_InputList));
        onMsg(msg);
    }

    private void onCloseKeyboard(View v) {
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Close_Keyboard));
        onMsg(msg);
    }

    private void onOpenFavoriteboard(View v) {
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Open_Favoriteboard));
        onMsg(msg);
    }

    private void onEditorCopy(View v) {
        fire_Editor_Action_Msg(EditorAction.copy);
    }

    private void onEditorPaste(View v) {
        fire_Editor_Action_Msg(EditorAction.paste);
    }

    private void onEditorCut(View v) {
        fire_Editor_Action_Msg(EditorAction.cut);
    }

    private void onEditorSelectAll(View v) {
        fire_Editor_Action_Msg(EditorAction.select_all);
    }

    private void onEditorUndo(View v) {
        fire_Editor_Action_Msg(EditorAction.undo);
    }

    private void onEditorRedo(View v) {
        fire_Editor_Action_Msg(EditorAction.redo);
    }

    private void fire_Editor_Action_Msg(EditorAction action) {
        UserEditorActionSingleTapMsgData data = new UserEditorActionSingleTapMsgData(action);
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Editor_Action).data(data));

        onMsg(msg);
    }

    // ==================== End: 按键事件处理 ==================

    private static class State {
        public final Type type;
        public final State prev;

        State(Type type) {
            this(type, null);
        }

        State(Type type, State prev) {
            this.type = type;
            this.prev = prev;
        }

        enum Type {
            /** 初始 */
            Init,
            /** 输入冻结中 */
            Input_Freeze_Doing,
            /** 输入中 */
            Input_Doing,
            /** 等待撤销对输入的清空操作 */
            Input_Cleaned_Cancel_Waiting,
            /** 工具栏显示中 */
            Toolbar_Show_Doing,
        }
    }

    private static class BtnTools {
        private final Animation enterAnim;
        private final Map<BtnGroupType, View> groups;

        private BtnGroupType activeGroup;

        // <<<<<<<<<<<<<< 输入栏按钮
        public Btn switchIme;
        public Btn showToolbar;

        public Btn hideToolbar;
        public Btn closeKeyboard;

        public Btn cleanInputList;
        public Btn cancelCleanInputList;
        // >>>>>>>>>>>>>>

        // <<<<<<<<<<<<<< 工具栏按钮
        public Btn favoriteboard;
        public Btn editorCopy;
        public Btn editorPaste;
        public Btn editorCut;
        public Btn editorSelectAll;
        public Btn editorUndo;
        public Btn editorRedo;

        public Btn settings;
        // >>>>>>>>>>>>>>

        BtnTools(Animation enterAnim) {
            this.enterAnim = enterAnim;
            this.groups = new HashMap<>();
        }

        public void addGroup(BtnGroupType type, View view) {
            this.groups.put(type, view);
        }

        public void activeGroup(BtnGroupType type) {
            this.activeGroup = type;
        }

        private Btn[] getBtnInToolbar() {
            return new Btn[] {
                    this.favoriteboard,
                    this.editorCopy,
                    this.editorPaste,
                    this.editorCut,
                    this.editorSelectAll,
                    this.editorUndo,
                    this.editorRedo,
                    this.settings,
                    };
        }

        private Btn[] getBtnInInputbar() {
            return new Btn[] {
                    this.switchIme,
                    this.showToolbar,
                    this.hideToolbar,
                    this.closeKeyboard,
                    this.cleanInputList,
                    this.cancelCleanInputList,
                    };
        }

        public void reset() {
            this.activeGroup = null;

            // 工具栏中的按钮：默认均显示且启用
            for (Btn btn : getBtnInToolbar()) {
                btn.shown = true;
                btn.disabled = false;
            }

            // 其他按钮：默认均不显示且禁用
            for (Btn btn : getBtnInInputbar()) {
                btn.shown = false;
                btn.disabled = false;
            }
        }

        public void update(boolean animation) {
            View activeGroupView = this.groups.get(this.activeGroup);
            if (activeGroupView == null) {
                return;
            }

            showGroupView(activeGroupView, animation);

            // /////////////////////////////////////////
            for (Btn btn : getBtnInInputbar()) {
                toggleShowBtn(btn.view, btn.shown, btn.listener);
                toggleDisableBtn(btn.view, btn.disabled, btn.listener);
            }
            for (Btn btn : getBtnInToolbar()) {
                toggleShowBtn(btn.view, btn.shown, btn.listener);
                toggleDisableBtn(btn.view, btn.disabled, btn.listener);
            }
        }

        private void showGroupView(View groupView, boolean animation) {
            for (View view : BtnTools.this.groups.values()) {
                if (view != groupView) {
                    ViewUtils.hide(view);
                }
            }

            if (ViewUtils.isVisible(groupView)) {
                return;
            }

            if (animation) {
                this.enterAnim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        BtnTools.this.enterAnim.setAnimationListener(null);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });

                groupView.startAnimation(this.enterAnim);
            }

            // Note: 只有已显示的视图才能应用动画
            ViewUtils.show(groupView);
        }
    }

    private enum BtnGroupType {
        inputbar,
        toolbar,
    }

    private static class Btn {
        public final View view;
        public final View.OnClickListener listener;

        public boolean disabled;
        public boolean shown;

        Btn(View rootView, int viewId, OnClickListener listener) {
            this.view = rootView.findViewById(viewId);
            this.listener = listener;
        }
    }
}
