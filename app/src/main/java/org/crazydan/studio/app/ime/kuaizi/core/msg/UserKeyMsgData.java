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

package org.crazydan.studio.app.ime.kuaizi.core.msg;

import org.crazydan.studio.app.ime.kuaizi.common.Point;
import org.crazydan.studio.app.ime.kuaizi.core.Key;

/**
 * {@link UserKeyMsg} 所携带的数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class UserKeyMsgData {
    /** 消息目标按键 */
    public final Key key;
    /** 消息触发的位置坐标 */
    public final Point at;

    public UserKeyMsgData(Key key, Point at) {
        this.key = key;
        this.at = at;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + "key=" + this.key + '}';
    }
}
