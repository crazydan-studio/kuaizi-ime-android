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
import java.util.stream.Collectors;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinCharTree;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.PinyinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.KeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.KeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputtingCharsMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.CharKeyView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewTouchListener;
import org.crazydan.studio.app.ime.kuaizi.utils.GsonUtils;
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
    private final PinyinCharTree pinyinCharTree;
    private final InputList inputList;
    private final Set<InputMsgListener> inputMsgListeners = new HashSet<>();

    private final KeyViewAdapter adapter;
    private final KeyViewLayoutManager layoutManager;
    private final Keyboard.Orientation keyboardOrientation;
    private Keyboard keyboard;

    public KeyboardView(@NonNull Context context) {
        this(context, null);
    }

    public KeyboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.inputList = new InputList();
        this.pinyinCharTree = GsonUtils.fromRawResourceJson(context, PinyinCharTree.class, R.raw.pinyin_char_tree);
        this.inputMsgListeners.add(this);

        HexagonOrientation keyViewOrientation = HexagonOrientation.POINTY_TOP;
        this.keyboardOrientation = Keyboard.Orientation.Portrait;

        this.adapter = new KeyViewAdapter(keyViewOrientation);
        this.layoutManager = new KeyViewLayoutManager(keyViewOrientation);

        setAdapter(this.adapter);
        setLayoutManager(this.layoutManager);
        addOnItemTouchListener(new KeyViewTouchListener());
    }

    public InputList getInputList() {
        return this.inputList;
    }

    public void startInput(Keyboard.Type type) {
        changeKeyboardType(type);
    }

    public void finishInput() {
        //
    }

    public void changeKeyboardType(Keyboard.Type type) {
        Keyboard oldKeyboard = this.keyboard;

        if (!(this.keyboard instanceof PinyinKeyboard)) {
            this.keyboard = new PinyinKeyboard(this.pinyinCharTree);
        }

        this.keyboard.inputList(this.inputList);
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

    private void relayout() {
        Key[][] keys = createKeys(this.keyboard.keyFactory());
        int columns = keys[0].length;
        int rows = keys.length;
        this.layoutManager.configGrid(columns, rows, 8);

        relayoutKeys(keys);
    }

    private void relayoutKeys(Key[][] keys) {
        this.adapter.setKeys(keys);
        this.adapter.notifyDataSetChanged();
    }

    /** 找到事件坐标下可见的{@link  KeyView 按键视图} */
    public KeyView<?, ?> findVisibleKeyViewUnder(MotionEvent e) {
        View child = this.layoutManager.findChildViewUnder(e.getX(), e.getY());

        KeyView<?, ?> keyView = child != null ? (KeyView<?, ?>) getChildViewHolder(child) : null;

        return keyView != null && !keyView.isHidden() ? keyView : null;
    }

    /** 找到事件坐标附近可见的{@link  KeyView 按键视图} */
    public KeyView<?, ?> findVisibleKeyViewNear(MotionEvent e, int deltaInDp) {
        return null;
    }

    public void onKeyMsg(KeyMsg msg, KeyMsgData data) {
        this.keyboard.onKeyMsg(msg, data);
    }

    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        switch (msg) {
            case InputtingChars:
                onInputtingCharsMsg((InputtingCharsMsgData) data);
                break;
            case InputtingCharsDone:
                onInputtingCharsDoneMsg(data);
                break;
        }
    }

    private void onInputtingCharsMsg(InputtingCharsMsgData data) {
        Key[][] keys = createKeys(data.keyFactory());
        relayoutKeys(keys);
    }

    private void onInputtingCharsDoneMsg(InputMsgData data) {
        Key[][] keys = createKeys(data.keyFactory());
        relayoutKeys(keys);
    }

    private void hideAllKeyViewsExclude(List<KeyView<?, ?>> exclude) {
        if (exclude != null) {
            filterKeyViews(keyView -> exclude.isEmpty() || !exclude.contains(keyView)).forEach(KeyView::hide);
            filterKeyViews(keyView -> !exclude.isEmpty() && exclude.contains(keyView)).forEach(KeyView::show);
        }
    }

    private void hideAllKeysExclude(List<Key> exclude) {
        if (exclude != null) {
            filterKeys(key -> exclude.isEmpty() || !exclude.contains(key)).forEach(Key::hide);
            filterKeys(key -> !exclude.isEmpty() && exclude.contains(key)).forEach(Key::show);
        }
    }

    private void showAllKeyViews() {
        filterKeyViews(k -> true).forEach(KeyView::show);
    }

    private void showAllKeys() {
        filterKeys(k -> true).forEach(Key::show);
    }

    private KeyView<?, ?> getKeyViewByKey(Key key) {
        if (key == null) {
            return null;
        }

        List<KeyView<?, ?>> list = filterKeyViews(keyView -> keyView.key() == key);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<KeyView<?, ?>> getKeyViewByChars(List<String> chars) {
        if (chars == null) {
            return null;
        } else if (chars.isEmpty()) {
            return new ArrayList<>();
        }

        return filterKeyViews(keyView -> keyView instanceof CharKeyView //
                                         && chars.contains(((CharKeyView) keyView).key().text()));
    }

    private List<Key> getKeyByChars(List<String> chars) {
        if (chars == null) {
            return null;
        } else if (chars.isEmpty()) {
            return new ArrayList<>();
        }

        return filterKeys(key -> key instanceof CharKey //
                                 && chars.contains(((CharKey) key).text()));
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

    private List<Key> filterKeys(Predicate<Key> predicate) {
        return this.adapter.keys().stream().filter(predicate).collect(Collectors.toList());
    }

    private Key[][] createKeys(Keyboard.KeyFactory keyFactory) {
        Keyboard.KeyFactory.Option option = new Keyboard.KeyFactory.Option();
        option.orientation = this.keyboardOrientation;

        return keyFactory.create(option);
    }
}
