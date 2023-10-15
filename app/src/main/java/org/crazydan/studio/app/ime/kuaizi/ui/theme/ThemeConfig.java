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

package org.crazydan.studio.app.ime.kuaizi.ui.theme;

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.ViewData;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-15
 */
public class ThemeConfig implements ViewData {
    private final Keyboard.ThemeType type;

    private List<CharInput> samples;
    private boolean selected;
    private boolean desktopSwipeUpGestureAdapted;
    private Keyboard.HandMode handMode;

    public ThemeConfig(Keyboard.ThemeType type) {
        this.type = type;
    }

    public Keyboard.ThemeType getType() {
        return this.type;
    }

    public List<CharInput> getSamples() {
        return this.samples;
    }

    public void setSamples(List<CharInput> samples) {
        this.samples = samples;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isDesktopSwipeUpGestureAdapted() {
        return this.desktopSwipeUpGestureAdapted;
    }

    public void setDesktopSwipeUpGestureAdapted(boolean desktopSwipeUpGestureAdapted) {
        this.desktopSwipeUpGestureAdapted = desktopSwipeUpGestureAdapted;
    }

    public Keyboard.HandMode getHandMode() {
        return this.handMode;
    }

    public void setHandMode(Keyboard.HandMode handMode) {
        this.handMode = handMode;
    }

    @Override
    public boolean isSameWith(Object o) {
        return false;
    }
}
