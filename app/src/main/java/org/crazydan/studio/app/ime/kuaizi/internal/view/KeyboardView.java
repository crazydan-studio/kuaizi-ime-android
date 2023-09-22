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

package org.crazydan.studio.app.ime.kuaizi.internal.view;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputAudioPlayingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewAnimator;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewGestureListener;

/**
 * {@link Keyboard 键盘}视图
 * <p/>
 * 负责显示各类键盘的按键布局，并提供事件监听等处理
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class KeyboardView extends BaseKeyboardView implements InputMsgListener {
    private final RecyclerViewGestureDetector gesture;
    private final KeyViewAnimator animator;
    private final AudioPlayer audioPlayer;

    private Keyboard keyboard;

    public KeyboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.animator = new KeyViewAnimator();
        setItemAnimator(this.animator);

        this.audioPlayer = new AudioPlayer();
        this.audioPlayer.load(getContext(), R.raw.tick_single, R.raw.tick_double, R.raw.page_flip);

        this.gesture = new RecyclerViewGestureDetector();
        this.gesture.bind(this) //
                    .addListener(new KeyViewGestureListener(this));
    }

    /** 重置视图 */
    public void reset() {
        this.gesture.reset();
        this.animator.reset();

        // Note：不清空按键，以避免子键盘切换过程中出现闪动
        //updateKeys(new Key[][] {});
        if (this.keyboard != null) {
            this.keyboard.removeInputMsgListener(this);
        }

        this.keyboard = null;
    }

    /** 更新键盘，并重绘键盘 */
    public void updateKeyboard(Keyboard keyboard) {
        reset();

        this.keyboard = keyboard;
        this.keyboard.addInputMsgListener(this);

        updateKeys(this.keyboard.getKeyFactory());
    }

    /** 响应按键点击、双击等消息 */
    public void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data) {
        this.keyboard.onUserKeyMsg(msg, data);
    }

    /** 响应键盘输入消息 */
    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        switch (msg) {
            case InputAudio_Playing: {
                onPlayingInputAudioMsg((InputAudioPlayingMsgData) data);
                break;
            }
            case InputChars_Inputting: {
                onInputtingCharsMsg((InputCharsInputtingMsgData) data);
                break;
            }
        }

        updateKeysByInputMsg(data);
    }

    private void onInputtingCharsMsg(InputCharsInputtingMsgData data) {
    }

    private void onPlayingInputAudioMsg(InputAudioPlayingMsgData data) {
        Keyboard.Config config = this.keyboard.getConfig();

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
            case PageFlip:
                if (!config.isPagingAudioDisabled()) {
                    this.audioPlayer.play(R.raw.page_flip);
                }
                break;
        }
    }

    private void updateKeysByInputMsg(InputMsgData data) {
        updateKeys(data.getKeyFactory());
    }

    private void updateKeys(Keyboard.KeyFactory keyFactory) {
        if (keyFactory == null) {
            return;
        }

        Key<?>[][] keys = keyFactory.create();

        super.updateKeys(keys, this.keyboard.getConfig().isLeftHandMode());
    }
}
