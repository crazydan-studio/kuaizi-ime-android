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

package org.crazydan.studio.app.ime.kuaizi.dict;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinDict;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.crazydan.studio.app.ime.kuaizi.core.dict.hmm.HmmDBHelper.predictPhrase;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-28
 */
@RunWith(AndroidJUnit4.class)
public class PinyinDictTest {
    private static final String LOG_TAG = PinyinDictTest.class.getSimpleName();

    @BeforeClass
    public static void before() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        PinyinDict dict = PinyinDict.instance();
        dict.init(context);
        dict.open(context);
    }

    @AfterClass
    public static void after() {
        PinyinDict.instance().close();
    }

    @Test
    public void test_hmm_phrase() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

        String[][] samples = new String[][] {
                // 中华人民共和国万岁
                new String[] { "zhong", "hua", "ren", "min", "gong", "he", "guo", "wan", "sui" },
                // 世界人民大团结万岁
                new String[] { "shi", "jie", "ren", "min", "da", "tuan", "jie", "wan", "sui" },
                };
        for (String[] sample : samples) {
            List<String> pinyinCharsList = Arrays.stream(sample)
                                                 .map(s -> dict.getPinyinTree().getPinyinCharsId(s))
                                                 .collect(Collectors.toList());
            List<String[]> phraseList = predictPhrase(db, pinyinCharsList, 500, 5).stream().map((phrase) -> {
                Map<String, InputWord> wordMap = dict.getPinyinWords(List.of(phrase));

                return Arrays.stream(phrase).map(wordMap::get).map(InputWord::getValue).toArray(String[]::new);
            }).collect(Collectors.toList());

            phraseList.forEach((phrase) -> {
                Log.i(LOG_TAG, String.join(" ", sample) + ": " + String.join("", phrase));
            });
        }
    }
}
