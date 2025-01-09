/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.ui.view.key;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.MathOpKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.SymbolKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.ui.view.KeyboardView;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link KeyboardView} 的 {@link RecyclerView} 适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class KeyboardViewAdapter extends RecyclerViewAdapter<Key, KeyViewHolder<?>> {
    private static final int VIEW_TYPE_CHAR_KEY = 0;
    private static final int VIEW_TYPE_CTRL_KEY = 1;
    private static final int VIEW_TYPE_NULL_KEY = 2;
    private static final int VIEW_TYPE_INPUT_WORD_KEY = 3;
    private static final int VIEW_TYPE_TOGGLE_PINYIN_SPELL_KEY = 4;
    private static final int VIEW_TYPE_SYMBOL_KEY = 6;
    private static final int VIEW_TYPE_MATH_OP_KEY = 7;
    private static final int VIEW_TYPE_XPAD_KEY = 8;

    private Integer themeResId;
    private HexagonOrientation orientation;

    public KeyboardViewAdapter() {
        super(ItemUpdatePolicy.manual);
    }

    /** 更新按键表，并对发生变更的按键发送变更消息，以仅对变化的按键做渲染 */
    public void updateItems(Key[][] keys, Integer themeResId, HexagonOrientation orientation) {
        Integer oldThemeResId = this.themeResId;
        HexagonOrientation oldOrientation = this.orientation;
        this.themeResId = themeResId;
        this.orientation = orientation;

        List<Key> newItems = new ArrayList<>();
        for (Key[] key : keys) {
            Collections.addAll(newItems, key);
        }

        List<Key> oldItems = super.updateItems(newItems);

        if (!Objects.equals(oldThemeResId, this.themeResId) //
            || !Objects.equals(oldOrientation, this.orientation) //
        ) {
            // Note：若正六边形方向或者主题样式发生了变化，则始终更新视图
            updateItemsByFull(oldItems, newItems);
        } else {
            updateItemsByDiffer(oldItems, newItems);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull KeyViewHolder<?> holder, int position) {
        Key item = getItem(position);

        bindKeyView(holder, item, this.orientation);
    }

    @Override
    public int getItemViewType(int position) {
        Key item = getItem(position);

        return getKeyViewType(item);
    }

    @NonNull
    @Override
    public KeyViewHolder<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        if (this.themeResId != null) {
            context = new ContextThemeWrapper(context, this.themeResId);
        }

        return createKeyViewHolder(context, parent, viewType);
    }

    public XPadKey getXPadKey() {
        for (Key key : this.items) {
            if (key instanceof XPadKey) {
                return (XPadKey) key;
            }
        }
        return null;
    }

    protected static int getKeyViewType(Key key) {
        if (key == null) {
            return VIEW_TYPE_NULL_KEY;
        } else if (key instanceof CtrlKey) {
            if (((CtrlKey) key).type == CtrlKey.Type.Toggle_Pinyin_Spell) {
                return VIEW_TYPE_TOGGLE_PINYIN_SPELL_KEY;
            }
            return VIEW_TYPE_CTRL_KEY;
        } else if (key instanceof InputWordKey) {
            return VIEW_TYPE_INPUT_WORD_KEY;
        } else if (key instanceof SymbolKey) {
            return VIEW_TYPE_SYMBOL_KEY;
        } else if (key instanceof MathOpKey) {
            return VIEW_TYPE_MATH_OP_KEY;
        } else if (key instanceof XPadKey) {
            return VIEW_TYPE_XPAD_KEY;
        }
        return VIEW_TYPE_CHAR_KEY;
    }

    /** 注：创建的视图未附加到 root 上 */
    private static KeyViewHolder<?> createKeyViewHolder(Context context, ViewGroup root, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_CTRL_KEY: {
                View view = inflateItemView(context, root, R.layout.key_ctrl_view);
                return new CtrlKeyViewHolder(view);
            }
            case VIEW_TYPE_TOGGLE_PINYIN_SPELL_KEY: {
                View view = inflateItemView(context, root, R.layout.key_ctrl_toggle_pinyin_spell_view);
                return new CtrlKeyPinyinToggleViewHolder(view);
            }
            case VIEW_TYPE_INPUT_WORD_KEY: {
                View view = inflateItemView(context, root, R.layout.key_char_input_word_view);
                return new InputWordKeyViewHolder(view);
            }
            case VIEW_TYPE_SYMBOL_KEY: {
                View view = inflateItemView(context, root, R.layout.key_char_view);
                return new SymbolKeyViewHolder(view);
            }
            case VIEW_TYPE_MATH_OP_KEY: {
                View view = inflateItemView(context, root, R.layout.key_char_view);
                return new MathOpKeyViewHolder(view);
            }
            case VIEW_TYPE_XPAD_KEY: {
                View view = inflateItemView(context, root, R.layout.key_xpad_view);
                return new XPadKeyViewHolder(view);
            }
            case VIEW_TYPE_NULL_KEY: {
                View view = inflateItemView(context, root, R.layout.key_ctrl_view);
                return new NullKeyViewHolder(view);
            }
            default: {
                View view = inflateItemView(context, root, R.layout.key_char_view);
                return new CharKeyViewHolder(view);
            }
        }
    }

    private static void bindKeyView(KeyViewHolder<?> holder, Key key, HexagonOrientation orientation) {
        if (key == null) {
            ((NullKeyViewHolder) holder).bind();
        } else if (key instanceof CtrlKey) {
            if (((CtrlKey) key).type == CtrlKey.Type.Toggle_Pinyin_Spell) {
                ((CtrlKeyPinyinToggleViewHolder) holder).bind((CtrlKey) key, orientation);
            } else {
                ((CtrlKeyViewHolder) holder).bind((CtrlKey) key, orientation);
            }
        } else if (key instanceof InputWordKey) {
            ((InputWordKeyViewHolder) holder).bind((InputWordKey) key, orientation);
        } else if (key instanceof XPadKey) {
            ((XPadKeyViewHolder) holder).bind((XPadKey) key);
        } else {
            holder.bind(key, orientation);
        }
    }
}
