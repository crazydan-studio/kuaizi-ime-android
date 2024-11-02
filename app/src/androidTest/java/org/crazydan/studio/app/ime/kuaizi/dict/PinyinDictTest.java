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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.dict.Emojis;
import org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.core.input.EmojiInputWord;
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
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.attachVariantToPinyinInputWord;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getAllGroupedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getAllPinyinInputWords;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getEmoji;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getEmojisByKeyword;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getPinyinInputWord;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getPinyinInputWords;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getWordId;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.saveUsedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteRawQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.rawQuerySQLite;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-28
 */
@RunWith(AndroidJUnit4.class)
public class PinyinDictTest {
    private static final String LOG_TAG = PinyinDictTest.class.getSimpleName();

    private static final int userPhraseBaseWeight = 500;

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

    @Test
    public void test_top_candidate_words() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

        String[] samples = new String[] { "zhong", "guo" };
        for (String pinyinChars : samples) {
            String pinyinCharsId = dict.getPinyinTree().getPinyinCharsId(pinyinChars);
            Assert.assertNotNull(pinyinCharsId);

            List<PinyinInputWord> wordList = getAllPinyinInputWords(db, pinyinCharsId, userPhraseBaseWeight);
            Assert.assertNotEquals(0, wordList.size());

            String result = wordList.stream()
                                    .map((word) -> String.format("%s:%s:%d",
                                                                 word.getValue(),
                                                                 word.getSpell().value,
                                                                 word.getWeight()))
                                    .collect(Collectors.joining(", "));

            Log.i(LOG_TAG, pinyinChars + " => " + result);
        }
    }

    @Test
    public void test_word_variant() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

        String pinyinChars = "guo";
        String pinyinCharsId = dict.getPinyinTree().getPinyinCharsId(pinyinChars);
        List<PinyinInputWord> wordList = getAllPinyinInputWords(db, pinyinCharsId, userPhraseBaseWeight);

        attachVariantToPinyinInputWord(db, wordList);

        wordList = wordList.stream().filter((word) -> word.getVariant() != null).collect(Collectors.toList());
        Assert.assertNotEquals(0, wordList.size());

        Log.i(LOG_TAG,
              pinyinChars + " => " + wordList.stream()
                                             .map((word) -> word.getValue()
                                                            + ":"
                                                            + word.getSpell().value
                                                            + ":"
                                                            + word.getVariant())
                                             .collect(Collectors.joining(", ")));
    }

    @Test
    public void test_query_grouped_emojis() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

        int top = 10;
        Emojis emojis = getAllGroupedEmojis(db, top);
        emojis.groups.forEach((group, emojiList) -> {
            Log.i(LOG_TAG,
                  group + ": " + emojiList.stream()
                                          .map((emoji) -> emoji.getValue() + ":" + emoji.getUid())
                                          .limit(top)
                                          .collect(Collectors.joining(", ")));
        });

        // <<<<<<<<<<<<<<<<<<<<< 更新使用权重
        String[] usedEmojis = new String[] {
                "\uD83D\uDE00", // 😀
                "\uD83D\uDE00", // 😀
                "\uD83D\uDE00", // 😀
                "\uD83D\uDC4B", // 👋
                "\uD83D\uDC4B", // 👋
                "\uD83D\uDC35", // 🐵
                "\uD83C\uDF47", // 🍇
                "\uD83C\uDF83", // 🎃
        };
        List<String> usedEmojiIdList = Arrays.stream(usedEmojis)
                                             .map((emoji) -> getEmoji(db, emoji).getUid())
                                             .collect(Collectors.toList());
        saveUsedEmojis(db, usedEmojiIdList, false);

        emojis = getAllGroupedEmojis(db, top);
        List<InputWord> generalEmojiList = emojis.groups.get(Emojis.GROUP_GENERAL);
        Assert.assertNotNull(generalEmojiList);

        // Note: 直接调用 LinkedHashSet#toArray 将报方法不存在
        Assert.assertArrayEquals(new LinkedHashSet<>(usedEmojiIdList).stream().toArray(String[]::new),
                                 generalEmojiList.stream().map(InputWord::getUid).toArray(String[]::new));
        Log.i(LOG_TAG,
              Emojis.GROUP_GENERAL + ": " + generalEmojiList.stream()
                                                            .map((emoji) -> emoji.getValue()
                                                                            + ":"
                                                                            + emoji.getUid()
                                                                            + ":"
                                                                            + emoji.getWeight())
                                                            .collect(Collectors.joining(", ")));
        // >>>>>>>>>>>>>>>>>>>>>>>

        // <<<<<<<<<<<<<< 撤销使用
        saveUsedEmojis(db, usedEmojiIdList, true);

        emojis = getAllGroupedEmojis(db, top);
        Assert.assertNull(emojis.groups.get(Emojis.GROUP_GENERAL));
        // >>>>>>>>>>>>>>
    }

    @Test
    public void test_query_emojis_by_keyword() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

        List<String[]> keywordIdsList = Stream.of("地球", "笑脸")
                                              .map((keyword) -> keyword.chars()
                                                                       .mapToObj((word) -> getWordId(db,
                                                                                                     String.valueOf((char) word)))
                                                                       .toArray(String[]::new))
                                              .collect(Collectors.toList());
        List<EmojiInputWord> emojiList = getEmojisByKeyword(db, keywordIdsList, 10);

        Assert.assertNotEquals(0, emojiList.size());
        Log.i(LOG_TAG, emojiList.stream().map(EmojiInputWord::getValue).collect(Collectors.joining(", ")));
    }

    private List<String> getTop5Phrases(
            SQLiteDatabase db, String pinyinCharsStr, List<String> pinyinCharsIdList
    ) {
        List<String> phraseList = //
                predictPinyinPhrase(db, pinyinCharsIdList, userPhraseBaseWeight, 5).stream().map((phrase) -> {
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
