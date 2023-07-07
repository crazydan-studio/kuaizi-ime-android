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

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewAdapter;
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

    private final HexagonOrientation orientation;
    private final List<Key> keys = new ArrayList<>();

    public KeyViewAdapter(HexagonOrientation orientation) {
        this.orientation = orientation;
    }

    public void setKeys(Key[][] keys) {
        this.keys.clear();

        for (Key[] key : keys) {
            this.keys.addAll(Arrays.asList(key));
        }
    }

    @Override
    public int getItemCount() {
        return this.keys.size();
    }

    @Override
    public void onBindViewHolder(@NonNull KeyView<?, ?> view, int position) {
        Key key = this.keys.get(position);

        if (key instanceof CtrlKey) {
            ((CtrlKeyView) view).bind((CtrlKey) key, this.orientation);
        } else {
            ((CharKeyView) view).bind((CharKey) key, this.orientation);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Key key = this.keys.get(position);

        if (key instanceof CtrlKey) {
            return VIEW_TYPE_CTRL_KEY;
        } else {
            return VIEW_TYPE_CHAR_KEY;
        }
    }

    @NonNull
    @Override
    public KeyView<?, ?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CTRL_KEY) {
            return new CtrlKeyView(inflateHolderView(parent, R.layout.ctrl_key_view));
        } else {
            return new CharKeyView(inflateHolderView(parent, R.layout.char_key_view));
        }
    }
}
