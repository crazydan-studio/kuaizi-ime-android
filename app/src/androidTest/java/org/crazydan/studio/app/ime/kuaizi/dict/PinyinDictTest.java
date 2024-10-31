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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinDictHelper.getPinyinCharsIdList;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.HmmDBHelper.predictPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.HmmDBHelper.savePinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getPinyinInputWord;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getPinyinInputWords;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteRawQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.rawQuerySQLite;

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
    public void test_hmm_predict_phrase() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

        List<String> dbVersion = rawQuerySQLite(db, new SQLiteRawQueryParams<String>() {{
            this.sql = "SELECT sqlite_version() as version";
            this.reader = (row) -> row.getString("version");
        }});
        Log.i(LOG_TAG, "SQLite version: " + CollectionUtils.first(dbVersion));

        Map<String, String> sampleMap = new HashMap<String, String>() {{
            put("zhong,hua,ren,min,gong,he,guo,wan,sui",
                "中:zhōng,华:huá,人:rén,民:mín,共:gòng,和:hé,国:guó,万:wàn,岁:suì");
            put("shi,jie,ren,min,da,tuan,jie,wan,sui",
                "世:shì,界:jiè,人:rén,民:mín,大:dà,团:tuán,结:jié,万:wàn,岁:suì");
        }};

        sampleMap.forEach((pinyinCharsStr, expectWordStr) -> {
            List<String> pinyinCharsIdList = getPinyinCharsIdList(dict, pinyinCharsStr.split(","));

            List<String> phraseList = getTop5Phrases(db, pinyinCharsStr, pinyinCharsIdList);

            String bestPhrase = CollectionUtils.first(phraseList);
            if (bestPhrase.equals(expectWordStr)) {
                return;
            }

            // 预测得到的短语与预期的不符，则进行短语修正后再尝试预测
            Log.i(LOG_TAG, pinyinCharsStr + " hasn't expected phrase, try again");

            List<PinyinInputWord> phraseWordList = Arrays.stream(expectWordStr.split(",")).map((word) -> {
                String[] splits = word.split(":");
                return getPinyinInputWord(db, splits[0], splits[1]);
            }).collect(Collectors.toList());

            savePinyinPhrase(db, phraseWordList, false);

            phraseList = getTop5Phrases(db, pinyinCharsStr, pinyinCharsIdList);
            bestPhrase = CollectionUtils.first(phraseList);

            Assert.assertEquals(expectWordStr, bestPhrase);
        });
    }

    private List<String> getTop5Phrases(
            SQLiteDatabase db, String pinyinCharsStr, List<String> pinyinCharsIdList
    ) {
        List<String> phraseList = //
                predictPinyinPhrase(db, pinyinCharsIdList, 500, 5).stream().map((phrase) -> {
                    Map<String, PinyinInputWord> wordMap = getPinyinInputWords(db, Set.of(phrase));

                    return Arrays.stream(phrase)
                                 .map(wordMap::get)
                                 .map((word) -> word.getValue() + ":" + word.getSpell().value)
                                 .collect(Collectors.joining(","));
                }).collect(Collectors.toList());

        Assert.assertNotEquals(0, phraseList.size());
        phraseList.forEach((phrase) -> {
            Log.i(LOG_TAG, pinyinCharsStr + ": " + phrase);
        });

        return phraseList;
    }
}
