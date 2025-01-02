/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import java.util.Arrays;
import java.util.Objects;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewGestureTrailer;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.TypedKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.ConfigUpdateMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCharsInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.key.KeyboardViewKeyAnimator;

/**
 * {@link Keyboard 键盘}的视图
 * <p/>
 * 负责显示各类键盘的按键布局，并提供事件监听等处理
 * <p/>
 * 注：在 {@link InputPaneView} 中统一分发 {@link InputMsg} 消息
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
        this.gesture = new RecyclerViewGestureDetector<>(this);
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
        } else if (key1 instanceof XPadKey) {
            XPadKey k1 = (XPadKey) key1;
            XPadKey k2 = (XPadKey) key2;

            return Objects.equals(k1.zone_0_key, k2.zone_0_key)
                   && Arrays.equals(k1.zone_1_keys, k2.zone_1_keys)
                   && Arrays.deepEquals(k1.zone_2_keys, k2.zone_2_keys);
        }

        // Note: SymbolKey/InputWordKey 可以根据其 value 值确定数据相同性，
        // 具体见其 Builder 处理 value 的逻辑
        return true;
    }

    // =============================== Start: 视图更新 ===================================

    private void reset() {
        this.gesture.reset();
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
        switch (msg.type) {
            case FingerMoving_Start: {
                // 对光标移动和文本选择按键禁用轨迹
                if ((CtrlKey.Type.Editor_Cursor_Locator.match(msg.data().key) //
                     || CtrlKey.Type.Editor_Range_Selector.match(msg.data().key)) //
                    && !isGestureTrailerDisabled() //
                ) {
                    this.gestureTrailer.setDisabled(false);
                }
                break;
            }
            case FingerMoving_Stop: {
                // 确保已绘制的轨迹被重绘，以避免出现轨迹残留
                if (!this.gestureTrailer.isDisabled()) {
                    invalidate();
                }
                this.gestureTrailer.setDisabled(true);
                break;
            }
        }

        this.listener.onMsg(msg);
    }

    /** 响应来自上层派发的 {@link InputMsg} 消息 */
    @Override
    public void onMsg(InputMsg msg) {
        Config config = this.config;
        KeyFactory keyFactory = msg.keyFactory;

        switch (msg.type) {
            case Config_Update_Done: {
                ConfigUpdateMsgData data = msg.data();
                // Note: 若非主题更新，则无需更新视图
                if (data.key != ConfigKey.theme) {
                    return;
                }
            }
            case Keyboard_Switch_Done:
            case Keyboard_Start_Done:
            case Keyboard_HandMode_Switch_Done: {
                if (config.bool(ConfigKey.disable_key_animation)) {
                    setItemAnimator(null);
                } else {
                    setItemAnimator(this.animator);
                }
                reset();
                break;
            }
            case InputChars_Input_Doing: {
                // 滑屏输入显示轨迹
                InputCharsInputMsgData data = msg.data();
                if (data.inputMode == InputCharsInputMsgData.InputMode.slip //
                    && !isGestureTrailerDisabled() //
                ) {
                    this.gestureTrailer.setDisabled(false);
                }
                break;
            }
            // 不影响布局的消息直接忽略
            case InputAudio_Play_Doing: {
                return;
            }
        }

        update(keyFactory);
    }

    // =============================== End: 消息处理 ===================================
}
