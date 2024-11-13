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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.crazydan.studio.app.ime.kuaizi.PinyinDictBaseTest;
import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.dict.Emojis;
import org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.core.input.EmojiWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinDictHelper.getPinyinCharsIdList;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.HmmDBHelper.predictPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.HmmDBHelper.saveUsedPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.attachVariantToPinyinWord;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getAllGroupedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getAllPinyinWordsByCharsId;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getEmoji;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getEmojisByKeyword;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getLatinsByStarts;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getPinyinWord;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getPinyinWordsByWordId;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getTopBestPinyinWordIds;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getWordId;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.saveUsedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.saveUsedLatins;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-28
 */
@RunWith(AndroidJUnit4.class)
public class PinyinDictTest extends PinyinDictBaseTest {
    private static final String LOG_TAG = PinyinDictTest.class.getSimpleName();

    private static final int userPhraseBaseWeight = 500;

    @Test
    public void test_hmm_predict_phrase() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

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
            if (!bestPhrase.equals(expectWordStr)) {
                // 预测得到的短语与预期的不符
                Log.i(LOG_TAG, pinyinCharsStr + " hasn't expected phrase, try again");
            }

            List<PinyinWord> phraseWordList = Arrays.stream(expectWordStr.split(",")).map((word) -> {
                String[] splits = word.split(":");
                return getPinyinWord(db, splits[0], splits[1]);
            }).collect(Collectors.toList());

            saveUsedPinyinPhrase(db, phraseWordList, false);

            phraseList = getTop5Phrases(db, pinyinCharsStr, pinyinCharsIdList);
            bestPhrase = CollectionUtils.first(phraseList);

            Assert.assertEquals(expectWordStr, bestPhrase);
        });
    }

    @Test
    public void test_predict_new_phrase_after_used() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

        List<PinyinWord> phraseWordList = Arrays.stream("筷:kuài,字:zì,输:shū,入:rù,法:fǎ".split(",")).map((word) -> {
            String[] splits = word.split(":");
            return getPinyinWord(db, splits[0], splits[1]);
        }).collect(Collectors.toList());
        saveUsedPinyinPhrase(db, phraseWordList, false);

        String pinyinCharsStr = "wo,ai,kuai,zi,shu,ru,fa";
        List<String> pinyinCharsIdList = getPinyinCharsIdList(dict, pinyinCharsStr.split(","));
        List<String> phraseList = getTop5Phrases(db, pinyinCharsStr, pinyinCharsIdList);

        String bestPhrase = CollectionUtils.first(phraseList);
        Assert.assertEquals("我:wǒ,爱:ài,筷:kuài,字:zì,输:shū,入:rù,法:fǎ", bestPhrase);
    }

    @Test
    public void test_top_candidate_words() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

        String[] samples = new String[] { "zhong", "guo" };
        for (String pinyinChars : samples) {
            String pinyinCharsId = dict.getPinyinTree().getPinyinCharsId(pinyinChars);
            Assert.assertNotNull(pinyinCharsId);

            Map<String, PinyinWord> wordMap = getAllPinyinWordsByCharsId(db, pinyinCharsId).stream()
                                                                                           .collect(Collectors.toMap(
                                                                                                   InputWord::getId,
                                                                                                   Function.identity(),
                                                                                                   (a, b) -> a,
                                                                                                   HashMap::new));

            int top = 10;
            List<PinyinWord> wordList = getTopBestPinyinWordIds(db, pinyinCharsId, userPhraseBaseWeight, top).stream()
                                                                                                             .map(wordMap::get)
                                                                                                             .collect(
                                                                                                                     Collectors.toList());
            Assert.assertTrue(wordList.size() <= top && !wordList.isEmpty());

            String result = wordList.stream()
                                    .map((word) -> word.getValue() + ":" + word.getSpell().value)
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
        List<PinyinWord> wordList = getAllPinyinWordsByCharsId(db, pinyinCharsId);

        attachVariantToPinyinWord(db, wordList);

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
                                          .map((emoji) -> emoji.getValue() + ":" + emoji.getId())
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
                                             .map((emoji) -> getEmoji(db, emoji).getId())
                                             .collect(Collectors.toList());
        saveUsedEmojis(db, usedEmojiIdList, false);

        emojis = getAllGroupedEmojis(db, top);
        List<InputWord> generalEmojiList = emojis.groups.get(Emojis.GROUP_GENERAL);
        Assert.assertNotNull(generalEmojiList);

        // Note: 直接调用 LinkedHashSet#toArray 将报方法不存在
        Assert.assertArrayEquals(new LinkedHashSet<>(usedEmojiIdList).stream().toArray(String[]::new),
                                 generalEmojiList.stream().map(InputWord::getId).toArray(String[]::new));
        Log.i(LOG_TAG,
              Emojis.GROUP_GENERAL + ": " + generalEmojiList.stream()
                                                            .map((emoji) -> emoji.getValue()
                                                                            + ":"
                                                                            + emoji.getId()
                                                                            + ":"
                                                                            + emoji.getWeight())
                                                            .collect(Collectors.joining(", ")));
        // >>>>>>>>>>>>>>>>>>>>>>>

        // <<<<<<<<<<<<<< 撤销使用
        saveUsedEmojis(db, usedEmojiIdList, true);

        emojis = getAllGroupedEmojis(db, top);
        Assert.assertNotNull(emojis.groups.get(Emojis.GROUP_GENERAL));
        Assert.assertTrue(emojis.groups.get(Emojis.GROUP_GENERAL).isEmpty());
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
        List<EmojiWord> emojiList = getEmojisByKeyword(db, keywordIdsList, 10);

        Assert.assertNotEquals(0, emojiList.size());
        Log.i(LOG_TAG, emojiList.stream().map(EmojiWord::getValue).collect(Collectors.joining(", ")));
    }

    @Test
    public void test_query_latins() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

        List<String> samples = List.of("I love China", "I love earth", "I love you");
        for (String sample : samples) {
            List<String> latins = List.of(sample.split("\\s+"));

            saveUsedLatins(db, latins, false);
        }

        List<String> latins = getLatinsByStarts(db, "lov", 5);
        Assert.assertEquals(1, latins.size());
        Assert.assertEquals("love", latins.get(0));

        latins = getLatinsByStarts(db, "Ch", 5);
        Assert.assertEquals(1, latins.size());
        Assert.assertEquals("China", latins.get(0));
    }

    private List<String> getTop5Phrases(
            SQLiteDatabase db, String pinyinCharsStr, List<String> pinyinCharsIdList
    ) {
        List<String> phraseList = //
                predictPinyinPhrase(db, pinyinCharsIdList, userPhraseBaseWeight, 5).stream().map((phrase) -> {
                    Map<String, PinyinWord> wordMap = getPinyinWordsByWordId(db, new HashSet<>(List.of(phrase)));

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
