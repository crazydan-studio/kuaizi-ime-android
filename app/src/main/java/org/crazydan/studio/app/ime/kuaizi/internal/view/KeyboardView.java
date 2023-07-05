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

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.PinyinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewTouchListener;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link Keyboard 键盘}视图
 * <p/>
 * 负责显示各类键盘的按键布局，并提供事件监听等处理
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class KeyboardView extends RecyclerView {
    private final KeyViewAdapter adapter;
    private final KeyViewLayoutManager layoutManager;
    private final KeyViewTouchListener touchListener;

    private HexagonOrientation keyViewOrientation;

    private Keyboard keyboard;
    private Keyboard.Orientation keyboardOrientation;

    public KeyboardView(@NonNull Context context) {
        this(context, null);
    }

    public KeyboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.keyViewOrientation = HexagonOrientation.POINTY_TOP;
        this.keyboardOrientation = Keyboard.Orientation.Portrait;

        this.adapter = new KeyViewAdapter(this.keyViewOrientation);
        this.layoutManager = new KeyViewLayoutManager(this.keyViewOrientation);
        this.touchListener = new KeyViewTouchListener();

        setAdapter(this.adapter);
        setLayoutManager(this.layoutManager);
        addOnItemTouchListener(this.touchListener);
    }

    public void startInput(Keyboard.Type type) {
        changeKeyboardType(type);
    }

    public void finishInput() {
        //
    }

    public void changeKeyboardType(Keyboard.Type type) {
        Keyboard oldKeyboard = this.keyboard;

        switch (type) {
            default:
                if (!(this.keyboard instanceof PinyinKeyboard)) {
                    this.keyboard = new PinyinKeyboard();
                }
        }

        if (oldKeyboard != this.keyboard) {
            relayout();
        } else {
            this.keyboard.reset();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void relayout() {
        Key[][] keys = this.keyboard.getKeys(this.keyboardOrientation);
        int columns = keys[0].length;
        int rows = keys.length;
        this.layoutManager.configGrid(columns, rows, 8);

        this.adapter.setKeys(keys);
        this.adapter.notifyDataSetChanged();
    }

    /** 找到事件坐标下可见的{@link  KeyView 按键视图} */
    public KeyView<?, ?> findVisibleKeyViewUnder(MotionEvent e) {
        View child = this.layoutManager.findChildViewUnder(e.getX(), e.getY());

        KeyView<?, ?> keyView = child != null ? (KeyView<?, ?>) getChildViewHolder(child) : null;

        return keyView != null && !keyView.isHidden() ? keyView : null;
    }

    public void onLongPress(KeyView<?, ?> keyView) {}

    public void onLongPressEnd(KeyView<?, ?> keyView) {
    }

    public void onClick(KeyView<?, ?> keyView) {}

    public void onMove(KeyView<?, ?> keyView) {
    }

    public void onMoveEnd(KeyView<?, ?> keyView) {
    }
}
