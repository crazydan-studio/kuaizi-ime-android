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

package org.crazydan.studio.app.ime.kuaizi.internal.key;

import java.util.Arrays;
import java.util.Objects;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;

/**
 * 将 X 型输入键盘作为普通按键，以便于与其他普通按键进行统一布局
 * <p/>
 * Note：不重载 {@link #equals} 以避免重建视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-04
 */
public class XPadKey extends BaseKey<XPadKey> {
    public final Key<?> zone_0_key;
    public final Key<?>[] zone_1_keys;
    public final Key<?>[][][] zone_2_keys;

    public XPadKey() {
        this(null, null, null);
    }

    public XPadKey(Key<?> zone_0_key, Key<?>[] zone_1_keys, Key<?>[][][] zone_2_keys) {
        this.zone_0_key = zone_0_key;
        this.zone_1_keys = zone_1_keys;
        this.zone_2_keys = zone_2_keys;
    }

    /**
     * 确保仅在按键布局发生变化时才触发
     * RecyclerViewGestureDetector#hasChangedViewData
     * 中的事件视图的重置
     */
    @Override
    public boolean isSameWith(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        XPadKey that = (XPadKey) o;
        return Objects.equals(this.zone_0_key, that.zone_0_key)
               && Arrays.equals(this.zone_1_keys, that.zone_1_keys)
               && Arrays.deepEquals(this.zone_2_keys, that.zone_2_keys);
    }

    @NonNull
    @Override
    public String toString() {
        return "XPadKey";
    }
}
