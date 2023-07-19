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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin.state;

import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin.State;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;

/**
 * {@link State.Type#SelectingInputText}的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-19
 */
public class SelectingInputTextData implements State.Data {
    private Motion anchor1 = new Motion();
    private Motion anchor2 = new Motion();

    public Motion getAnchor1() {
        return this.anchor1;
    }

    public Motion getAnchor2() {
        return this.anchor2;
    }

    public void updateAnchor1(Motion motion) {
        this.anchor1 = LocatingInputCursorData.createAnchor(motion);
    }

    public void updateAnchor2(Motion motion) {
        this.anchor2 = LocatingInputCursorData.createAnchor(motion);
    }

    public void resetAnchor1() {
        this.anchor1 = new Motion();
    }

    public void resetAnchor2() {
        this.anchor2 = new Motion();
    }
}
