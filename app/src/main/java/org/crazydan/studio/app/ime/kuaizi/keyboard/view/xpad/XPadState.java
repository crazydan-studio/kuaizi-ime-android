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

package org.crazydan.studio.app.ime.kuaizi.keyboard.view.xpad;

import org.crazydan.studio.app.ime.kuaizi.keyboard.Key;

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
        public final Key<?> key;

        public KeyData(Key<?> key) {
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
