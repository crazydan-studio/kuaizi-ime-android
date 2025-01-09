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

package org.crazydan.studio.app.ime.kuaizi;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-11-05
 */
public abstract class PinyinDictBaseTest {

    @BeforeClass
    public static void before() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        PinyinDict.instance().open(context, new PinyinDict.Listener.Noop());
    }

    @AfterClass
    public static void after() {
        PinyinDict.instance().close();
    }
}
