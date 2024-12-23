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

package org.crazydan.studio.app.ime.kuaizi.ui.view.key;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.MathOpKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.SymbolKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.ui.view.KeyboardView;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link KeyboardView} 的 {@link RecyclerView} 适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class KeyboardViewAdapter extends RecyclerViewAdapter<KeyViewHolder<?, ?>> {
    private static final int VIEW_TYPE_CHAR_KEY = 0;
    private static final int VIEW_TYPE_CTRL_KEY = 1;
    private static final int VIEW_TYPE_NULL_KEY = 2;
    private static final int VIEW_TYPE_INPUT_WORD_KEY = 3;
    private static final int VIEW_TYPE_TOGGLE_INPUT_SPELL_KEY = 4;
    private static final int VIEW_TYPE_SYMBOL_KEY = 6;
    private static final int VIEW_TYPE_MATH_OP_KEY = 7;
    private static final int VIEW_TYPE_XPAD_KEY = 8;

    private HexagonOrientation orientation;

    private List<Key<?>> dataList = new ArrayList<>();
    private Integer themeResId;

    public KeyboardViewAdapter(HexagonOrientation orientation) {
        this.orientation = orientation;
    }

    /** 更新按键表，并对发生变更的按键发送变更消息，以仅对变化的按键做渲染 */
    public void updateDataList(Key<?>[][] keys, Integer themeResId, HexagonOrientation orientation) {
        Integer oldThemeResId = this.themeResId;
        HexagonOrientation oldOrientation = this.orientation;
        this.themeResId = themeResId;
        this.orientation = orientation;

        List<Key<?>> oldDataList = this.dataList;
        this.dataList = new ArrayList<>();

        for (Key<?>[] key : keys) {
            this.dataList.addAll(Arrays.asList(key));
        }

        if (!Objects.equals(oldThemeResId, this.themeResId) //
            || !Objects.equals(oldOrientation, this.orientation) //
        ) {
            // Note：若正六边形方向或者主题样式发生了变化，则始终更新视图
            updateItemsForce(oldDataList, this.dataList);
        } else {
            updateItems(oldDataList, this.dataList);
        }
    }

    @Override
    public int getItemCount() {
        return this.dataList.size();
    }

    @Override
    public void onBindViewHolder(@NonNull KeyViewHolder<?, ?> holder, int position) {
        Key<?> data = this.dataList.get(position);

        bindKeyView(holder, data, this.orientation);
    }

    @Override
    public int getItemViewType(int position) {
        Key<?> data = this.dataList.get(position);

        return getKeyViewType(data);
    }

    @NonNull
    @Override
    public KeyViewHolder<?, ?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        if (this.themeResId != null) {
            context = new ContextThemeWrapper(context, this.themeResId);
        }

        return createKeyViewHolder(context, parent, viewType);
    }

    public XPadKey getXPadKey() {
        for (Key<?> key : this.dataList) {
            if (key instanceof XPadKey) {
                return (XPadKey) key;
            }
        }
        return null;
    }

    protected static int getKeyViewType(Key<?> key) {
        if (key instanceof CtrlKey) {
            switch (((CtrlKey) key).getType()) {
                case Toggle_Pinyin_spell:
                    return VIEW_TYPE_TOGGLE_INPUT_SPELL_KEY;
                default:
                    return VIEW_TYPE_CTRL_KEY;
            }
        } else if (key instanceof InputWordKey) {
            return VIEW_TYPE_INPUT_WORD_KEY;
        } else if (key instanceof SymbolKey) {
            return VIEW_TYPE_SYMBOL_KEY;
        } else if (key instanceof MathOpKey) {
            return VIEW_TYPE_MATH_OP_KEY;
        } else if (key instanceof XPadKey) {
            return VIEW_TYPE_XPAD_KEY;
        } else if (key == null) {
            return VIEW_TYPE_NULL_KEY;
        } else {
            return VIEW_TYPE_CHAR_KEY;
        }
    }

    /** 注：创建的视图未附加到 root 上 */
    private static KeyViewHolder<?, ?> createKeyViewHolder(Context context, ViewGroup root, int viewType) {
        if (viewType == VIEW_TYPE_CTRL_KEY) {
            return new CtrlKeyViewHolder(inflateItemView(context, root, R.layout.key_ctrl_view));
        } else if (viewType == VIEW_TYPE_TOGGLE_INPUT_SPELL_KEY) {
            return new CtrlToggleInputSpellKeyViewHolder(inflateItemView(context,
                                                                         root,
                                                                         R.layout.key_ctrl_toggle_input_spell_view));
        } else if (viewType == VIEW_TYPE_INPUT_WORD_KEY) {
            return new InputWordKeyViewHolder(inflateItemView(context, root, R.layout.key_char_input_word_view));
        } else if (viewType == VIEW_TYPE_SYMBOL_KEY) {
            return new SymbolKeyViewHolder(inflateItemView(context, root, R.layout.key_char_view));
        } else if (viewType == VIEW_TYPE_MATH_OP_KEY) {
            return new MathOpKeyViewHolder(inflateItemView(context, root, R.layout.key_char_view));
        } else if (viewType == VIEW_TYPE_XPAD_KEY) {
            return new XPadKeyViewHolder(inflateItemView(context, root, R.layout.key_xpad_view));
        } else if (viewType == VIEW_TYPE_NULL_KEY) {
            return new NullKeyViewHolder(inflateItemView(context, root, R.layout.key_ctrl_view));
        } else {
            return new CharKeyViewHolder(inflateItemView(context, root, R.layout.key_char_view));
        }
    }

    private static void bindKeyView(KeyViewHolder<?, ?> holder, Key<?> key, HexagonOrientation orientation) {
        if (key instanceof CtrlKey) {
            switch (((CtrlKey) key).getType()) {
                case Toggle_Pinyin_spell:
                    ((CtrlToggleInputSpellKeyViewHolder) holder).bind((CtrlKey) key, orientation);
                    break;
                default:
                    ((CtrlKeyViewHolder) holder).bind((CtrlKey) key, orientation);
            }
        } else if (key instanceof InputWordKey) {
            ((InputWordKeyViewHolder) holder).bind((InputWordKey) key, orientation);
        } else if (key instanceof SymbolKey) {
            ((SymbolKeyViewHolder) holder).bind((SymbolKey) key, orientation);
        } else if (key instanceof MathOpKey) {
            ((MathOpKeyViewHolder) holder).bind((MathOpKey) key, orientation);
        } else if (key instanceof XPadKey) {
            ((XPadKeyViewHolder) holder).bind((XPadKey) key);
        } else if (key == null) {
            ((NullKeyViewHolder) holder).bind();
        } else {
            ((CharKeyViewHolder) holder).bind((CharKey) key, orientation);
        }
    }
}
