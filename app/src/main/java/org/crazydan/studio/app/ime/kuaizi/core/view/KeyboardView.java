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

package org.crazydan.studio.app.ime.kuaizi.core.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputAudioPlayDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.KeyboardConfigUpdateDoneMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.view.key.KeyViewAnimator;
import org.crazydan.studio.app.ime.kuaizi.core.view.key.KeyViewGestureListener;
import org.crazydan.studio.app.ime.kuaizi.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.widget.AudioPlayer;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewGestureTrailer;

/**
 * {@link Keyboard 键盘}视图
 * <p/>
 * 负责显示各类键盘的按键布局，并提供事件监听等处理
 * <p/>
 * 注：在 {@link org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView ImeInputView}
 * 中统一分发 {@link InputMsg} 消息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class KeyboardView extends BaseKeyboardView implements UserKeyMsgListener, InputMsgListener {
    private final RecyclerViewGestureDetector gesture;
    private final RecyclerViewGestureTrailer gestureTrailer;
    private final KeyViewAnimator animator;
    private final AudioPlayer audioPlayer;

    private Keyboard keyboard;

    public KeyboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        int trailColor = ThemeUtils.getColorByAttrId(context, R.attr.input_trail_color);
        RecyclerViewGestureTrailer trailer = this.gestureTrailer = new RecyclerViewGestureTrailer(this, true);
        this.gestureTrailer.setColor(trailColor);
        addItemDecoration(new ItemDecoration() {
            @Override
            public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull State state) {
                trailer.draw(canvas);
            }
        });

        this.animator = new KeyViewAnimator();
        setItemAnimator(this.animator);

        this.audioPlayer = new AudioPlayer();
        this.audioPlayer.load(getContext(),
                              R.raw.tick_single,
                              R.raw.tick_double,
                              R.raw.page_flip,
                              R.raw.tick_clock,
                              R.raw.tick_ping);

        this.gesture = new RecyclerViewGestureDetector();
        this.gesture.bind(this) //
                    // Note：以下监听的执行顺序与注册顺序一致
                    .addListener(new KeyViewGestureListener(this)) //
                    .addListener(this.gestureTrailer);
    }

    public Keyboard getKeyboard() {
        return this.keyboard;
    }

    public boolean isGestureTrailerDisabled() {
        return getKeyboard() != null && getKeyboard().getConfig().isGestureSlippingTrailDisabled();
    }

    /** 更新键盘，并重绘键盘 */
    public void updateKeyboard(Keyboard keyboard) {
        reset();
        this.keyboard = keyboard;

        updateKeys(keyboard, keyboard.getKeyFactory());
    }

    /** 响应按键点击、双击等消息 */
    @Override
    public void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data) {
        switch (msg) {
            case FingerMovingStart: {
                // 对光标移动和文本选择按键启用轨迹
                if ((CtrlKey.is(data.target, CtrlKey.Type.Editor_Cursor_Locator) //
                     || CtrlKey.is(data.target, CtrlKey.Type.Editor_Range_Selector)) //
                    && !this.keyboard.getConfig().isGestureSlippingTrailDisabled()) {
                    this.gestureTrailer.setDisabled(false);
                }
                break;
            }
            case FingerMovingEnd: {
                // 确保已绘制的轨迹被重绘，以避免出现轨迹残留
                if (!this.gestureTrailer.isDisabled()) {
                    invalidate();
                }
                this.gestureTrailer.setDisabled(true);
                break;
            }
        }

        this.keyboard.onUserKeyMsg(msg, data);
    }

    @Override
    public void onMsg(Keyboard keyboard, InputMsg msg, InputMsgData msgData) {
        switch (msg) {
            case InputAudio_Play_Doing: {
                on_InputAudio_Play_Doing_Msg(keyboard, (InputAudioPlayDoingMsgData) msgData);
                break;
            }
            case Keyboard_Config_Update_Done: {
                Keyboard.Config config = ((KeyboardConfigUpdateDoneMsgData) msgData).after;
                if (config.isKeyAnimationDisabled()) {
                    setItemAnimator(null);
                } else {
                    setItemAnimator(this.animator);
                }
                break;
            }
            case InputChars_Input_Doing: {
                // 滑屏输入显示轨迹
                if (((InputCharsInputtingMsgData) msgData).keyInputType == InputCharsInputtingMsgData.KeyInputType.slip
                    && !keyboard.getConfig().isGestureSlippingTrailDisabled()) {
                    this.gestureTrailer.setDisabled(false);
                }
                break;
            }
        }

        updateKeys(keyboard, msgData.getKeyFactory());
    }

    private void on_InputAudio_Play_Doing_Msg(Keyboard keyboard, InputAudioPlayDoingMsgData data) {
        Keyboard.Config config = keyboard.getConfig();

        switch (data.audioType) {
            case SingleTick:
                if (!config.isKeyClickedAudioDisabled()) {
                    this.audioPlayer.play(R.raw.tick_single);
                }
                break;
            case DoubleTick:
                if (!config.isKeyClickedAudioDisabled()) {
                    this.audioPlayer.play(R.raw.tick_double);
                }
                break;
            case ClockTick:
                if (!config.isKeyClickedAudioDisabled()) {
                    this.audioPlayer.play(R.raw.tick_clock);
                }
                break;
            case PingTick:
                if (!config.isKeyClickedAudioDisabled()) {
                    this.audioPlayer.play(R.raw.tick_ping);
                }
                break;
            case PageFlip:
                if (!config.isPagingAudioDisabled()) {
                    this.audioPlayer.play(R.raw.page_flip);
                }
                break;
        }
    }

    private void reset() {
        this.keyboard = null;
        this.gesture.reset();
        this.animator.reset();

        // Note：不清空按键，以避免子键盘切换过程中出现闪动
        //updateKeys(new Key[][] {});
    }

    private void updateKeys(Keyboard keyboard, Keyboard.KeyFactory keyFactory) {
        if (keyFactory == null) {
            return;
        }

        Key<?>[][] keys = keyFactory.create();

        boolean animationDisabled = keyFactory instanceof Keyboard.NoAnimationKeyFactory;
        if (animationDisabled) {
            setItemAnimator(null);
            // Note：post 的参数是在当前渲染线程执行完毕后再调用的，
            // 因此，在无动效的按键渲染完毕后可以恢复原动画设置，
            // 确保其他需要动画的按键能够正常显示动画效果
            post(() -> setItemAnimator(this.animator));
        }

        super.updateKeys(keys, keyboard.getConfig().isLeftHandMode());
    }
}
