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

package org.crazydan.studio.app.ime.kuaizi.common;

/**
 * 运动信息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-19
 */
public class Motion {
    public static final Motion none = new Motion(Direction.none, 0);

    /** 移动方向 */
    public final Direction direction;
    /** 移动距离 */
    public final float distance;

    public Motion(Motion motion) {
        this(motion.direction, motion.distance);
    }

    public Motion(Motion motion, float distance) {
        this(motion.direction, distance);
    }

    public Motion(Direction direction, float distance) {
        this.direction = direction;
        this.distance = distance;
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
