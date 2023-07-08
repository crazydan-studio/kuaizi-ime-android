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

package org.crazydan.studio.app.ime.kuaizi.internal.view.key;

import android.os.Handler;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.KeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.KeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.FingerMoveMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.KeyboardView;

/**
 * {@link Keyboard 键盘}{@link Key 按键}的点击事件监听器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class KeyViewTouchListener implements RecyclerView.OnItemTouchListener {
    private static final long LONG_PRESS_TIMEOUT_MILLS = 500;
    // https://stackoverflow.com/questions/6519748/how-to-determine-a-long-touch-on-android/24050544#24050544
    private final Handler longPressListenerHandler = new Handler();
    private boolean longPressing;
    private boolean moving;
    private Runnable longPressListener;
    private KeyView<?, ?> longPressKeyView;

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        // 启用对触屏事件的拦截支持
        return true;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        KeyboardView keyboardView = (KeyboardView) rv;
        KeyView<?, ?> keyView = keyboardView.findVisibleKeyViewUnder(e);

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                startLongPress(keyboardView, keyView);
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (!this.longPressing //
                    && !this.moving) {
                    onClick(keyboardView, keyView);
                }
                onTouchEnd(keyboardView, keyView);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!this.longPressing) {
                    cleanLongPress();
                }

                // TODO 增加移动方向以表明是进入按键还是离开按键
                KeyView<?, ?> nearKeyView = keyboardView.findVisibleKeyViewNear(e, 8);
                onMove(keyboardView, keyView, nearKeyView);
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                onTouchEnd(keyboardView, keyView);
                break;
            }
        }
    }

    private void onTouchEnd(KeyboardView keyboardView, KeyView<?, ?> keyView) {
        if (this.longPressing) {
            onLongPressEnd(keyboardView, keyView);
        }
        if (this.moving) {
            onMoveEnd(keyboardView, keyView);
        }

        cleanLongPress();
    }

    private void startLongPress(KeyboardView keyboardView, KeyView<?, ?> keyView) {
        cleanLongPress();

        this.longPressListener = () -> onLongPress(keyboardView, keyView);
        this.longPressListenerHandler.postDelayed(this.longPressListener, LONG_PRESS_TIMEOUT_MILLS);
    }

    private void cleanLongPress() {
        if (this.longPressListener != null) {
            this.longPressListenerHandler.removeCallbacks(this.longPressListener);
            this.longPressListener = null;
        }

        this.longPressing = false;
        this.longPressKeyView = null;
    }

    private void onLongPress(KeyboardView keyboardView, KeyView<?, ?> keyView) {
        this.longPressing = true;
        this.longPressKeyView = keyView;

        KeyMsgData data = new KeyMsgData(keyView.key());
        keyboardView.onKeyMsg(KeyMsg.KeyLongPress, data);
    }

    private void onLongPressEnd(KeyboardView keyboardView, KeyView<?, ?> keyView) {
        KeyMsgData data = new KeyMsgData(this.longPressKeyView.key());
        keyboardView.onKeyMsg(KeyMsg.KeyLongPressEnd, data);
    }

    private void onClick(KeyboardView keyboardView, KeyView<?, ?> keyView) {
        KeyMsgData data = new KeyMsgData(keyView != null ? keyView.key() : null);
        keyboardView.onKeyMsg(KeyMsg.KeyClick, data);
    }

    private void onMove(KeyboardView keyboardView, KeyView<?, ?> keyView, KeyView<?, ?> nearKeyView) {
        this.moving = true;

        FingerMoveMsgData data = new FingerMoveMsgData(keyView != null ? keyView.key() : null,
                                                       nearKeyView != null ? nearKeyView.key() : null);
        keyboardView.onKeyMsg(KeyMsg.FingerMove, data);
    }

    private void onMoveEnd(KeyboardView keyboardView, KeyView<?, ?> keyView) {
        this.moving = false;
    }
}
