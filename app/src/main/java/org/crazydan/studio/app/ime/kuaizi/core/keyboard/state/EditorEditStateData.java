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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard.state;

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.Motion;
import org.crazydan.studio.app.ime.kuaizi.common.Point;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.State;

/**
 * {@link State.Type#Editor_Edit_Doing} 的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-02-01
 */
public class EditorEditStateData implements State.Data {
    /** 操作的目标 */
    public final Target target;

    /** 起点坐标 */
    private Point from;
    /** 终点坐标 */
    private Point to;

    public EditorEditStateData(Target target, Point from) {
        this.target = target;
        startAt(from);
    }

    public void startAt(Point from) {
        this.from = from;
    }

    public void moveTo(Point to) {
        this.to = to;
    }

    public Motion getMotion() {
        return this.from.motion(this.to);
    }

    public enum Target {
        cursor(R.string.text_tip_editor_operation_move_cursor),
        selection(R.string.text_tip_editor_operation_select_range),
        ;

        public final int tipResId;

        Target(int tipResId) {
            this.tipResId = tipResId;
        }
    }
}
