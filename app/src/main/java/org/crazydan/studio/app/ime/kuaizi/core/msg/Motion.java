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

    public enum Direction {
        up,
        down,
        right,
        left,
        none,
    }
}
