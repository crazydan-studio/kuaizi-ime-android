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

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-19
 */
public class Motion {
    /** 移动方向 */
    public final Direction direction;
    /** 移动距离 */
    public final float distance;
    public final long timestamp;

    public Motion() {
        this(Direction.none, 0, 0);
    }

    public Motion(Motion motion) {
        this(motion.direction, motion.distance, motion.timestamp);
    }

    public Motion(Direction direction, float distance, long timestamp) {
        this.direction = direction;
        this.distance = distance;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "{direction=" + this.direction + ", distance=" + this.distance + '}';
    }

    public enum Direction {
        up,
        down,
        right,
        left,
        none,
    }
}
