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

package org.crazydan.studio.app.ime.kuaizi.ui.view.xpad;

import org.crazydan.studio.app.ime.kuaizi.core.Key;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-13
 */
public class XPadState {
    public final Type type;
    public final Data data;

    public XPadState(Type type) {
        this(type, null);
    }

    public XPadState(Type type, Data data) {
        this.type = type;
        this.data = data;
    }

    public enum Type {
        Init,

        InputChars_Input_Waiting,
        InputChars_Input_Doing,
    }

    interface Data {}

    public static class KeyData implements Data {
        public final Key key;

        public KeyData(Key key) {
            this.key = key;
        }
    }

    public static class BlockData implements Data {
        private final int totalBlocks;
        private int startBlock = -1;
        private int currentBlock = -1;
        private int blockDiff = 0;

        public BlockData(int totalBlocks) {
            this.totalBlocks = totalBlocks;
        }

        public void reset() {
            this.startBlock = -1;
            this.currentBlock = -1;
            this.blockDiff = 0;
        }

        public int getBlockDiff() {
            return this.blockDiff;
        }

        public int getStartBlock() {
            return this.startBlock;
        }

        public void updateCurrentBlock(int block) {
            if (this.currentBlock >= 0) {
                int diff = block - this.currentBlock;
                // Note：跨起止序号时，差值需取反，且绝对值为 1
                if (Math.abs(diff) == this.totalBlocks - 1) {
                    diff = diff > 0 ? -1 : 1;
                }

                this.blockDiff += diff;
            } else {
                this.startBlock = block;
                this.blockDiff = 0;
            }

            // Note：若已环绕一圈，则重置差值
            if (Math.abs(this.blockDiff) == this.totalBlocks + 1) {
                this.blockDiff = this.blockDiff > 0 ? 1 : -1;
            }
            this.currentBlock = block;
        }
    }
}
