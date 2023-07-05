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
import org.crazydan.studio.app.ime.kuaizi.internal.view.KeyboardView;

/**
 * {@link Keyboard 键盘}{@link Key 按键}的点击事件监听器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class KeyViewTouchListener implements RecyclerView.OnItemTouchListener {
    private static final long LONG_PRESS_TIMEOUT_MILLS = 1000;

    private enum Status {
        LongPressing,
        Moving,
        None,
    }

    private Status status = Status.None;

    // https://stackoverflow.com/questions/6519748/how-to-determine-a-long-touch-on-android/24050544#24050544
    private final Handler longPressListenerHandler = new Handler();
    private Runnable longPressListener;
    private KeyView<?, ?> currentKeyView;

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
            case MotionEvent.ACTION_DOWN:
                startLongPress(keyboardView, keyView);
                break;
            case MotionEvent.ACTION_UP: {
                if (this.status != Status.LongPressing) {
                    if (this.currentKeyView == keyView) {
                        onClick(keyboardView, keyView);
                    }
                }
            }
            break;
            case MotionEvent.ACTION_MOVE:
                onMove(keyboardView, keyView);
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }

        this.currentKeyView = keyView;
        if (e.getAction() == MotionEvent.ACTION_CANCEL //
            || e.getAction() == MotionEvent.ACTION_UP) {
            if (this.status == Status.LongPressing) {
                onLongPressEnd(keyboardView, keyView);
            } else if (this.status == Status.Moving) {
                onMoveEnd(keyboardView, keyView);
            }

            cleanLongPress();
        }
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
        this.status = Status.None;
    }

    private void onLongPress(KeyboardView keyboardView, KeyView<?, ?> keyView) {
        this.status = Status.LongPressing;
        keyboardView.onLongPress(keyView);
    }

    private void onLongPressEnd(KeyboardView keyboardView, KeyView<?, ?> keyView) {
        keyboardView.onLongPressEnd(keyView);
    }

    private void onClick(KeyboardView keyboardView, KeyView<?, ?> keyView) {
        keyboardView.onClick(keyView);
    }

    private void onMove(KeyboardView keyboardView, KeyView<?, ?> keyView) {
        this.status = Status.Moving;
        keyboardView.onMove(keyView);
    }

    private void onMoveEnd(KeyboardView keyboardView, KeyView<?, ?> keyView) {
        keyboardView.onMoveEnd(keyView);
    }
}
