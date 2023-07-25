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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.PinyinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputCursorLocatingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputtingCharsMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.PlayingInputAudioMsgData;
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
    private final PinyinDictDB pinyinDict;
    private final InputList inputList;
    private final Set<InputMsgListener> inputMsgListeners = new HashSet<>();

    private final int keySpacing = 8;
    private final KeyViewAdapter adapter;
    private final KeyViewLayoutManager layoutManager;
    private final Keyboard.Orientation keyboardOrientation;
    private final RecyclerViewGestureDetector gesture;
    private final KeyViewAnimator animator;
    private final AudioPlayer audioPlayer;
    private Keyboard keyboard;

    public KeyboardView(@NonNull Context context) {
        this(context, null);
    }

    public KeyboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.inputList = new InputList();
        this.pinyinDict = PinyinDictDB.create(context);
        this.inputMsgListeners.add(this);

        HexagonOrientation keyViewOrientation = HexagonOrientation.POINTY_TOP;
        this.keyboardOrientation = Keyboard.Orientation.Portrait;

        this.adapter = new KeyViewAdapter(keyViewOrientation);
        this.layoutManager = new KeyViewLayoutManager(keyViewOrientation);
        this.animator = new KeyViewAnimator();

        setAdapter(this.adapter);
        setLayoutManager(this.layoutManager);
        setItemAnimator(this.animator);

        this.audioPlayer = new AudioPlayer();
        this.audioPlayer.load(context, R.raw.tick_single, R.raw.tick_double, R.raw.page_flip);

        this.gesture = new RecyclerViewGestureDetector();
        this.gesture.bind(this) //
                    .addListener(new KeyViewGestureListener(this));
    }

    public InputList getInputList() {
        return this.inputList;
    }

    public void startInput(Keyboard.Type type) {
        this.gesture.reset();

        changeKeyboardType(type);
    }

    public void finishInput() {
        this.keyboard.reset();
    }

    public void changeKeyboardType(Keyboard.Type type) {
        Keyboard oldKeyboard = this.keyboard;

        if (!(this.keyboard instanceof PinyinKeyboard)) {
            this.keyboard = new PinyinKeyboard(this.pinyinDict);
        }

        this.keyboard.setInputList(this.inputList);
        this.inputMsgListeners.forEach(this.keyboard::addInputMsgListener);

        if (oldKeyboard != this.keyboard) {
            relayout();
        } else {
            this.keyboard.reset();
        }
    }

    /**
     * 添加{@link InputMsg 输入消息监听}
     * <p/>
     * 忽略重复加入的监听，且执行顺序与添加顺序无关
     */
    public void addInputMsgListener(InputMsgListener listener) {
        this.inputMsgListeners.add(listener);
    }

    public void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data) {
        this.keyboard.onUserKeyMsg(msg, data);
    }

    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        switch (msg) {
            case PlayingInputAudio: {
                onPlayingInputAudioMsg((PlayingInputAudioMsgData) data);
                break;
            }
            case InputtingChars: {
                onInputtingCharsMsg((InputtingCharsMsgData) data);
                break;
            }
            case InputtingCharsDone:
            case ChoosingInputCandidate:
                break;
            case LocatingInputCursor: {
                onLocatingInputTargetCursorMsg((InputCursorLocatingMsgData) data);
                break;
            }
        }

        relayoutKeysByInputMsg(data);
    }

    private void onInputtingCharsMsg(InputtingCharsMsgData data) {
        // Note: 单击输入不会有渐隐动画，因为不会发生按键重绘
        this.animator.addFadeOutKey(data.current);
    }

    private void onPlayingInputAudioMsg(PlayingInputAudioMsgData data) {
        switch (data.audioType) {
            case SingleTick:
                this.audioPlayer.play(R.raw.tick_single);
                break;
            case DoubleTick:
                this.audioPlayer.play(R.raw.tick_double);
                break;
            case PageFlip:
                this.audioPlayer.play(R.raw.page_flip);
                break;
        }
    }

    private void onLocatingInputTargetCursorMsg(InputCursorLocatingMsgData data) {
    }

    private void relayout() {
        Key<?>[][] keys = createKeys(this.keyboard.getKeyFactory());
        int columns = keys[0].length;
        int rows = keys.length;
        this.layoutManager.configGrid(columns, rows, this.keySpacing);

        relayoutKeys(keys);
    }

    private void relayoutKeysByInputMsg(InputMsgData data) {
        Keyboard.KeyFactory keyFactory = data.getKeyFactory();
        if (keyFactory == null) {
            return;
        }

        Key<?>[][] keys = createKeys(keyFactory);
        relayoutKeys(keys);
    }

    private void relayoutKeys(Key<?>[][] keys) {
        this.adapter.bindKeys(keys);
    }

    private Key<?>[][] createKeys(Keyboard.KeyFactory keyFactory) {
        Keyboard.KeyFactory.Option option = new Keyboard.KeyFactory.Option();
        option.orientation = this.keyboardOrientation;

        return keyFactory.create(option);
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
