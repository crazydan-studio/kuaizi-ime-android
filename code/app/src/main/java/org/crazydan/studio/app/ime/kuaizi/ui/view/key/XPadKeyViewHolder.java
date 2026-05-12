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

import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.core.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.ui.view.xpad.XPadView;

/**
 * {@link XPadKey} 视图的 {@link RecyclerView.ViewHolder}
 * <p/>
 * XPad 输入涉及键盘切换和输入同时进行的情况，因此，不能因为 XPad 的按键布局变化而重新构造视图，
 * 只能保持 XPad 视图不变，并在其按键变化时调用 {@link XPadView#updateZoneKeys}
 * 重新绘制其内部视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-04
 */
public class XPadKeyViewHolder extends KeyViewHolder<XPadView> {

    public XPadKeyViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(XPadKey key) {
        super.bind(key, null);

        getXPad().updateZoneKeys(key.zone_0_key, key.zone_1_keys, key.zone_2_keys);
    }

    public XPadView getXPad() {
        return this.fgView;
    }

    // =================== Start: 重载无效的功能接口 ===================

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

    // =================== End: 重载无效的功能接口 ===================
}
