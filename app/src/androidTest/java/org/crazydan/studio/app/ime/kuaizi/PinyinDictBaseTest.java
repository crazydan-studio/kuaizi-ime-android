/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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
