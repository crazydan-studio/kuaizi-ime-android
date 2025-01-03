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

import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.pane.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.ui.view.xpad.XPadView;

/**
 * {@link XPadKey} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-04
 */
public class XPadKeyViewHolder extends KeyViewHolder<XPadView> {

    public XPadKeyViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public XPadView getXPad() {
        return this.fgView;
    }

    public void bind(XPadKey key) {
        super.bind(key, null);

        getXPad().updateZoneKeys(key.zone_0_key, key.zone_1_keys, key.zone_2_keys);
    }

    // <<<<<<<<<<<<<<<< Start 重载无效的功能接口
    @Override
    public void disable() {
    }

    @Override
    public void enable() {
    }

    @Override
    public void touchDown() {
    }

    @Override
    public void touchUp() {
    }
    // >>>>>>>>>>>>>>>>> End
}
