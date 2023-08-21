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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
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
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewAnimator;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewGestureListener;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewLayoutManager;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link Keyboard 键盘}视图
 * <p/>
 * 负责显示各类键盘的按键布局，并提供事件监听等处理
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class KeyboardView extends RecyclerView implements InputMsgListener {
    private final int keySpacing = 8;
    private final KeyViewAdapter adapter;
    private final KeyViewLayoutManager layoutManager;
    private final RecyclerViewGestureDetector gesture;
    private final KeyViewAnimator animator;
    private final AudioPlayer audioPlayer;

    private Keyboard keyboard;

    public KeyboardView(@NonNull Context context) {
        this(context, null);
    }

    public KeyboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        HexagonOrientation keyViewOrientation = HexagonOrientation.POINTY_TOP;

        this.adapter = new KeyViewAdapter(keyViewOrientation);
        this.layoutManager = new KeyViewLayoutManager(keyViewOrientation);
        this.animator = new KeyViewAnimator();

        setAdapter(this.adapter);
        setLayoutManager(this.layoutManager);
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

        Key<?>[][] keys = createKeys(this.keyboard.getKeyFactory());
        int columns = keys[0].length;
        int rows = keys.length;
        this.layoutManager.configGrid(columns, rows, this.keySpacing);

        updateKeys(keys);
    }

    /** 响应按键点击、双击等消息 */
    public void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data) {
        Key<?> key = data.target;
        if (key != null && key.isDisabled()) {
            return;
        }

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
            case InputChars_InputtingEnd:
            case InputCandidate_Choosing:
                break;
        }

        updateKeysByInputMsg(data);
    }

    private void onInputtingCharsMsg(InputCharsInputtingMsgData data) {
        Keyboard.Config config = this.keyboard.getConfig();

        if (!config.isGlidingInputAnimationDisabled()) {
            // Note: 单击输入不会有渐隐动画，因为不会发生按键重绘
            this.animator.addFadeOutKey(data.current);
        }
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
        Keyboard.KeyFactory keyFactory = data.getKeyFactory();
        if (keyFactory == null) {
            return;
        }

        Key<?>[][] keys = createKeys(keyFactory);
        updateKeys(keys);
    }

    private void updateKeys(Key<?>[][] keys) {
        this.adapter.bindKeys(keys);
    }

    private Key<?>[][] createKeys(Keyboard.KeyFactory keyFactory) {
        return keyFactory.create();
    }

    /** 找到指定坐标下可见的{@link  KeyView 按键视图} */
    public KeyView<?, ?> findVisibleKeyViewUnderLoose(float x, float y) {
        View child = this.layoutManager.findChildViewUnderLoose(x, y);

        return getVisibleKeyView(child);
    }

    /** 找到指定坐标下可见的{@link  KeyView 按键视图} */
    public KeyView<?, ?> findVisibleKeyViewUnder(float x, float y) {
        View child = this.layoutManager.findChildViewUnder(x, y);

        return getVisibleKeyView(child);
    }

    /** 找到指定坐标附近可见的{@link  KeyView 按键视图} */
    public KeyView<?, ?> findVisibleKeyViewNear(float x, float y) {
        View child = this.layoutManager.findChildViewNear(x, y, this.keySpacing * 2);

        return getVisibleKeyView(child);
    }

    private KeyView<?, ?> getVisibleKeyView(View view) {
        KeyView<?, ?> keyView = view != null ? (KeyView<?, ?>) getChildViewHolder(view) : null;

        return keyView != null && !keyView.isHidden() ? keyView : null;
    }

    private KeyView<?, ?> getKeyViewByKey(Key<?> key) {
        if (key == null) {
            return null;
        }

        List<KeyView<?, ?>> list = filterKeyViews(keyView -> key.equals(keyView.getKey()));
        return list.isEmpty() ? null : list.get(0);
    }

    private List<KeyView<?, ?>> filterKeyViews(Predicate<KeyView<?, ?>> filter) {
        List<KeyView<?, ?>> list = new ArrayList<>();

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            KeyView<?, ?> keyView = (KeyView<?, ?>) getChildViewHolder(view);

            if (filter.test(keyView)) {
                list.add(keyView);
            }
        }
        return list;
    }
}
