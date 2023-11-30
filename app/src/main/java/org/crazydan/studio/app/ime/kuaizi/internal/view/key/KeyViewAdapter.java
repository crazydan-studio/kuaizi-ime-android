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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.MathOpKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.SymbolKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewAdapter;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link Keyboard 键盘}{@link Key 按键}的{@link RecyclerView}适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class KeyViewAdapter extends RecyclerViewAdapter<KeyView<?, ?>> {
    private static final int VIEW_TYPE_CHAR_KEY = 0;
    private static final int VIEW_TYPE_CTRL_KEY = 1;
    private static final int VIEW_TYPE_NULL_KEY = 2;
    private static final int VIEW_TYPE_INPUT_WORD_KEY = 3;
    private static final int VIEW_TYPE_TOGGLE_INPUT_SPELL_KEY = 4;
    private static final int VIEW_TYPE_FILTER_INPUT_WORD_KEY = 5;
    private static final int VIEW_TYPE_SYMBOL_KEY = 6;
    private static final int VIEW_TYPE_MATH_OP_KEY = 7;
    private static final int VIEW_TYPE_XPAD_KEY = 8;

    private HexagonOrientation orientation;

    private List<Key<?>> keys = new ArrayList<>();
    private Integer themeResId;

    public KeyViewAdapter(HexagonOrientation orientation) {
        this.orientation = orientation;
    }

    protected static int getKeyViewType(Key<?> key) {
        if (key instanceof CtrlKey) {
            switch (((CtrlKey) key).getType()) {
                case Filter_PinyinInputCandidate_by_Stroke:
                    return VIEW_TYPE_FILTER_INPUT_WORD_KEY;
                case Toggle_PinyinInput_spell:
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
    private static KeyView<?, ?> createKeyView(Context context, ViewGroup root, int viewType) {
        if (viewType == VIEW_TYPE_CTRL_KEY) {
            return new CtrlKeyView(inflateItemView(context, root, R.layout.key_ctrl_view));
        } else if (viewType == VIEW_TYPE_FILTER_INPUT_WORD_KEY) {
            return new CtrlFilterInputWordKeyView(inflateItemView(context,
                                                                  root,
                                                                  R.layout.key_ctrl_filter_input_word_view));
        } else if (viewType == VIEW_TYPE_TOGGLE_INPUT_SPELL_KEY) {
            return new CtrlToggleInputSpellKeyView(inflateItemView(context,
                                                                   root,
                                                                   R.layout.key_ctrl_toggle_input_spell_view));
        } else if (viewType == VIEW_TYPE_INPUT_WORD_KEY) {
            return new InputWordKeyView(inflateItemView(context, root, R.layout.key_char_input_word_view));
        } else if (viewType == VIEW_TYPE_SYMBOL_KEY) {
            return new SymbolKeyView(inflateItemView(context, root, R.layout.key_char_view));
        } else if (viewType == VIEW_TYPE_MATH_OP_KEY) {
            return new MathOpKeyView(inflateItemView(context, root, R.layout.key_char_view));
        } else if (viewType == VIEW_TYPE_XPAD_KEY) {
            return new XPadKeyView(inflateItemView(context, root, R.layout.key_xpad_view));
        } else if (viewType == VIEW_TYPE_NULL_KEY) {
            return new NullKeyView(inflateItemView(context, root, R.layout.key_ctrl_view));
        } else {
            return new CharKeyView(inflateItemView(context, root, R.layout.key_char_view));
        }
    }

    private static void bindKeyView(KeyView<?, ?> view, Key<?> key, HexagonOrientation orientation) {
        if (key instanceof CtrlKey) {
            switch (((CtrlKey) key).getType()) {
                case Filter_PinyinInputCandidate_by_Stroke:
                    ((CtrlFilterInputWordKeyView) view).bind((CtrlKey) key, orientation);
                    break;
                case Toggle_PinyinInput_spell:
                    ((CtrlToggleInputSpellKeyView) view).bind((CtrlKey) key, orientation);
                    break;
                default:
                    ((CtrlKeyView) view).bind((CtrlKey) key, orientation);
            }
        } else if (key instanceof InputWordKey) {
            ((InputWordKeyView) view).bind((InputWordKey) key, orientation);
        } else if (key instanceof SymbolKey) {
            ((SymbolKeyView) view).bind((SymbolKey) key, orientation);
        } else if (key instanceof MathOpKey) {
            ((MathOpKeyView) view).bind((MathOpKey) key, orientation);
        } else if (key instanceof XPadKey) {
            ((XPadKeyView) view).bind((XPadKey) key);
        } else if (key == null) {
            ((NullKeyView) view).bind();
        } else {
            ((CharKeyView) view).bind((CharKey) key, orientation);
        }
    }

    public XPadKey getXPadKey() {
        for (Key<?> key : this.keys) {
            if (key instanceof XPadKey) {
                return (XPadKey) key;
            }
        }
        return null;
    }

    /** 更新按键表，并对发生变更的按键发送变更消息，以仅对变化的按键做渲染 */
    public void updateKeys(Key<?>[][] keys, Integer themeResId, HexagonOrientation orientation) {
        this.themeResId = themeResId;

        List<Key<?>> oldKeys = this.keys;
        this.keys = new ArrayList<>();

        for (Key<?>[] key : keys) {
            this.keys.addAll(Arrays.asList(key));
        }

        if (this.orientation != orientation) {
            this.orientation = orientation;

            // Note：若正六边形方向发生了变化，则始终更新视图
            updateItems(oldKeys, this.keys, (o, n) -> -1);
        } else {
            updateItems(oldKeys, this.keys);
        }
    }

    @Override
    public int getItemCount() {
        return this.keys.size();
    }

    @Override
    public void onBindViewHolder(@NonNull KeyView<?, ?> view, int position) {
        Key<?> key = this.keys.get(position);

        bindKeyView(view, key, this.orientation);
    }

    @Override
    public int getItemViewType(int position) {
        Key<?> key = this.keys.get(position);

        return getKeyViewType(key);
    }

    @NonNull
    @Override
    public KeyView<?, ?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        if (this.themeResId != null) {
            context = new ContextThemeWrapper(context, this.themeResId);
        }

        return createKeyView(context, parent, viewType);
    }
}
