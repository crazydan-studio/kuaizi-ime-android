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

import java.util.Objects;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.IMEditorView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewGestureTrailer;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.TypedKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.ConfigUpdateMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputCharsInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.key.KeyboardViewKeyAnimator;

/**
 * {@link Keyboard 键盘}的视图
 * <p/>
 * 负责显示各类键盘的按键布局，并提供事件监听等处理
 * <p/>
 * 注：在 {@link IMEditorView} 中统一分发 {@link InputMsg} 消息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class KeyboardView extends KeyboardViewBase implements UserKeyMsgListener, InputMsgListener {
    private final RecyclerViewGestureDetector<Key> gesture;
    private final RecyclerViewGestureTrailer gestureTrailer;
    private final KeyboardViewKeyAnimator animator;

    private Config config;
    private UserKeyMsgListener listener;

    /** 与 {@link Key} 相关的手势消息监听器 */
    public interface GestureListener {
        void onGesture(Key key, ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data);
    }

    public KeyboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.animator = new KeyboardViewKeyAnimator();
        setItemAnimator(this.animator);

        RecyclerViewGestureTrailer gestureTrailer = new RecyclerViewGestureTrailer(this, true);
        addItemDecoration(new ItemDecoration() {
            @Override
            public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull State state) {
                gestureTrailer.draw(canvas);
            }
        });

        int trailColor = ThemeUtils.getColorByAttrId(context, R.attr.input_trail_color);
        this.gestureTrailer = gestureTrailer;
        this.gestureTrailer.setColor(trailColor);

        KeyboardViewGestureListener gestureListener = new KeyboardViewGestureListener(this);
        this.gesture = new RecyclerViewGestureDetector<>(context, this);
        // Note：确保先处理视图消息，再处理轨迹消息，因为前者会对轨迹做禁用处理
        this.gesture.addListener(gestureListener) //
                    .addListener(gestureTrailer);
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public boolean isGestureTrailerDisabled() {
        return this.config.bool(ConfigKey.disable_gesture_slipping_trail);
    }

    @Override
    protected boolean isSameAdapterItem(Key key1, Key key2) {
        if (key1 == null || key2 == null //
            || key1.getClass() != key2.getClass() //
            || !Objects.equals(key1.value, key2.value) //
        ) {
            return false;
        }

        if (key1 instanceof TypedKey) {
            if (key1 instanceof CtrlKey //
                && !Objects.equals(((CtrlKey) key1).option, ((CtrlKey) key2).option) //
            ) {
                return false;
            }
            return Objects.equals(((TypedKey<?>) key1).type, ((TypedKey<?>) key2).type);
        }
        // Note: XPadKey 始终视为相同，以确保其视图不会被重建，其内部按键的变化，
        // 直接获取其视图进行重绘即可
        else if (key1 instanceof XPadKey) {
            return true;
        }

        // Note: SymbolKey/InputWordKey 可以根据其 value 值确定数据相同性，
        // 具体见其 Builder 处理 value 的逻辑
        return true;
    }

    // =============================== Start: 视图更新 ===================================

    private void reset() {
        // TODO 在 RecyclerViewGestureDetector 会根据模型变化而重置手势状态，
        //  不需要再显式重置了？：仍需继续测试和观察
        // Note: 在 XPad 模式下，对于其内部键盘的切换，不能重置手势，
        // 否则，在键盘切换时将不能同时启动划圈动作，因为，重置将导致手势被打断
        //this.gesture.reset();
        this.animator.reset();

        // Note：不清空按键，以避免子键盘切换过程中出现闪动
        //updateKeys(new Key[][] {});
    }

    private void update(KeyFactory keyFactory) {
        if (keyFactory == null) {
            return;
        }

        Key[][] keys = keyFactory.getKeys();

        boolean animationDisabled = keyFactory instanceof KeyFactory.NoAnimation;
        if (animationDisabled) {
            setItemAnimator(null);

            // Note：post 的参数是在当前渲染线程执行完毕后再调用的，
            // 因此，在无动效的按键渲染完毕后可以恢复原动画设置，
            // 确保其他需要动画的按键能够正常显示动画效果
            post(() -> setItemAnimator(this.animator));
        }

        boolean leftHandMode = keyFactory instanceof KeyFactory.LeftHandMode;
        super.update(keys, leftHandMode);
    }

    // =============================== End: 视图更新 ===================================

    // =============================== Start: 消息处理 ===================================

    public void setListener(UserKeyMsgListener listener) {
        this.listener = listener;
    }

    /** 响应按键点击、双击等消息，并向上传递 {@link UserKeyMsg} 消息 */
    @Override
    public void onMsg(UserKeyMsg msg) {
        this.log.beginTreeLog("Handle %s", () -> new Object[] { msg.getClass() }) //
                .debug("Message Type: %s", () -> new Object[] { msg.type }) //
                .debug("Message Data: %s", () -> new Object[] { msg.data() });

        handleMsg(msg);

        this.log.endTreeLog();
        //////////////////////////////////////////////////////////////////////
        this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                msg.getClass(), this.listener.getClass()
        });

        this.listener.onMsg(msg);

        this.log.endTreeLog();
    }

    /** 响应来自上层派发的 {@link InputMsg} 消息 */
    @Override
    public void onMsg(InputMsg msg) {
        this.log.beginTreeLog("Handle %s", () -> new Object[] { msg.getClass() }) //
                .debug("Message Type: %s", () -> new Object[] { msg.type }) //
                .debug("Message Data: %s", () -> new Object[] { msg.data() });

        switch (msg.type) {
            case Config_Update_Done: {
                ConfigUpdateMsgData data = msg.data();
                // Note: 仅关注与键盘布局相关的配置更新
                ConfigKey[] effects = new ConfigKey[] {
                        ConfigKey.theme,
                        ConfigKey.hand_mode,
                        ConfigKey.enable_x_input_pad,
                        ConfigKey.enable_latin_use_pinyin_keys_in_x_input_pad
                };
                if (!CollectionUtils.contains(effects, data.configKey)) {
                    this.log.warn("Ignore configuration %s", () -> new Object[] { data.configKey }) //
                            .endTreeLog();
                    return;
                }
                break;
            }
            case Keyboard_Switch_Done:
            case Keyboard_Start_Done:
            case Keyboard_HandMode_Switch_Done: {
                Config config = this.config;
                if (config.bool(ConfigKey.disable_key_animation)) {
                    setItemAnimator(null);
                } else {
                    setItemAnimator(this.animator);
                }
                reset();

                this.log.debug("Do reset for message %s", () -> new Object[] { msg.type });
                break;
            }
            case InputChars_Input_Doing: {
                // 滑屏输入显示轨迹
                InputCharsInputMsgData data = msg.data();
                if (data.inputMode == InputCharsInputMsgData.InputMode.slip //
                    && !isGestureTrailerDisabled() //
                ) {
                    this.gestureTrailer.setDisabled(false);

                    this.log.debug("Enable gesture trailer for message %s", () -> new Object[] { msg.type });
                }
                break;
            }
            // Note: 不影响按键布局的消息，直接返回
            case IME_Switch_Doing:
            case InputAudio_Play_Doing:
                //
            case Keyboard_Switch_Doing:
            case Keyboard_Start_Doing:
            case Keyboard_Hide_Done:
            case Keyboard_Exit_Done:
            case Keyboard_HandMode_Switch_Doing:
            case Keyboard_XPad_Simulation_Terminated:
                //
            case InputList_Clean_Done:
            case InputList_Cleaned_Cancel_Done:
                //
            case InputCompletion_Create_Done:
            case InputCompletion_Apply_Done:
                //
            case Input_Choose_Doing:
            case InputChars_Input_Popup_Hide_Doing:
            case InputChars_Input_Popup_Show_Doing: {
                this.log.warn("Ignore message %s", () -> new Object[] { msg.type }) //
                        .endTreeLog();
                return;
            }
        }

        this.log.debug("Update view for message %s", () -> new Object[] { msg.type });

        update(msg.keyFactory);

        this.log.endTreeLog();
    }

    private void handleMsg(UserKeyMsg msg) {
        switch (msg.type) {
            case FingerMoving_Start: {
                // 对光标移动和文本选择按键禁用轨迹
                if ((CtrlKey.Type.Editor_Cursor_Locator.match(msg.data().key) //
                     || CtrlKey.Type.Editor_Range_Selector.match(msg.data().key)) //
                    && !isGestureTrailerDisabled() //
                ) {
                    this.gestureTrailer.setDisabled(false);

                    this.log.debug("Enable gesture trailer for message %s", () -> new Object[] { msg.type });
                }
                break;
            }
            case FingerMoving_Stop: {
                // 确保已绘制的轨迹被重绘，以避免出现轨迹残留
                if (!this.gestureTrailer.isDisabled()) {
                    invalidate();
                }
                this.gestureTrailer.setDisabled(true);

                this.log.debug("Disable gesture trailer for message %s", () -> new Object[] { msg.type });
                break;
            }
            default: {
                this.log.warn("Ignore message %s", () -> new Object[] { msg.type });
            }
        }
    }

    // =============================== End: 消息处理 ===================================
}
