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

package org.crazydan.studio.app.ime.kuaizi.dict;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.crazydan.studio.app.ime.kuaizi.IMEditorDict;
import org.crazydan.studio.app.ime.kuaizi.IMEditorDictBaseTest;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.crazydan.studio.app.ime.kuaizi.dict.db.HmmDBHelper.predictPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.HmmDBHelper.saveUsedPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDBHelper.getAllPinyinWordsByCharsId;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDBHelper.getFirstBestPinyinWord;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDBHelper.getPinyinWord;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDBHelper.getPinyinWordsByWordId;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDBHelper.getTopBestPinyinWordIds;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-28
 */
@RunWith(AndroidJUnit4.class)
public class PinyinDBHelperTest extends IMEditorDictBaseTest {
    private static final String LOG_TAG = PinyinDBHelperTest.class.getSimpleName();

    private static final int userPhraseBaseWeight = 500;

    @Test
    public void test_hmm_predict_phrase() {
        PinyinDict dict = IMEditorDict.instance().usePinyinDict();
        SQLiteDatabase db = IMEditorDict.instance().getDB();

        Map<String, String> sampleMap = new HashMap<String, String>() {{
            put("zhong,hua,ren,min,gong,he,guo,wan,sui",
                "中:zhōng,华:huá,人:rén,民:mín,共:gòng,和:hé,国:guó,万:wàn,岁:suì");
            put("shi,jie,ren,min,da,tuan,jie,wan,sui",
                "世:shì,界:jiè,人:rén,民:mín,大:dà,团:tuán,结:jié,万:wàn,岁:suì");
        }};

        sampleMap.forEach((pinyinCharsStr, expectWordStr) -> {
            List<Integer> pinyinCharsIdList = getPinyinCharsIdList(dict, pinyinCharsStr.split(","));

            List<String> phraseList = getTop5Phrases(db, pinyinCharsStr, pinyinCharsIdList);

            String bestPhrase = CollectionUtils.first(phraseList);
            Assert.assertNotNull(bestPhrase);

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
        PinyinDict dict = IMEditorDict.instance().usePinyinDict();
        SQLiteDatabase db = IMEditorDict.instance().getDB();

        String pinyinCharsStr = "wo,ai,kuai,zi,shu,ru,fa";
        String usedPhrase = "筷:kuài,字:zì,输:shū,入:rù,法:fǎ";
        String expectedPhrase = "我:wǒ,爱:ài," + usedPhrase;
        List<Integer> pinyinCharsIdList = getPinyinCharsIdList(dict, pinyinCharsStr.split(","));

        List<String> phraseList = getTopPhrases(db, pinyinCharsStr, pinyinCharsIdList, 1);
        String bestPhrase = CollectionUtils.first(phraseList);
        Assert.assertNotEquals(expectedPhrase, bestPhrase);

        List<PinyinWord> phraseWordList = Arrays.stream(usedPhrase.split(",")).map((word) -> {
            String[] splits = word.split(":");
            return getPinyinWord(db, splits[0], splits[1]);
        }).collect(Collectors.toList());
        saveUsedPinyinPhrase(db, phraseWordList, false);

        phraseList = getTopPhrases(db, pinyinCharsStr, pinyinCharsIdList, 1);
        bestPhrase = CollectionUtils.first(phraseList);
        Assert.assertEquals(expectedPhrase, bestPhrase);
    }

    @Test
    public void test_predict_phrase_with_confirmed_word() {
        PinyinDict dict = IMEditorDict.instance().usePinyinDict();
        SQLiteDatabase db = IMEditorDict.instance().getDB();

        String pinyinCharsStr = "shi,jie,da,yu,zhou";
        String expectedPhrase = "世:shì,界:jiè,大:dà,宇:yǔ,宙:zhòu";
        List<Integer> pinyinCharsIdList = getPinyinCharsIdList(dict, pinyinCharsStr.split(","));

        List<String> phraseList = getTopPhrases(db, pinyinCharsStr, pinyinCharsIdList, 1);
        String bestPhrase = CollectionUtils.first(phraseList);
        Assert.assertNotEquals(expectedPhrase, bestPhrase);

        PinyinWord shi = getPinyinWord(db, "世", "shì");
        PinyinWord da = getPinyinWord(db, "大", "dà");
        PinyinWord yu = getPinyinWord(db, "宇", "yǔ");
        Map<Integer, Integer> confirmedPhraseWords = new HashMap<Integer, Integer>() {{
            put(0, shi.id);
            put(2, da.id);
            put(3, yu.id);
        }};

        phraseList = getTopPhrases(db, pinyinCharsStr, pinyinCharsIdList, 1, confirmedPhraseWords);
        bestPhrase = CollectionUtils.first(phraseList);
        Assert.assertEquals(expectedPhrase, bestPhrase);
    }

    @Test
    public void test_predict_phrase_with_not_record_pinyin() {
        PinyinDict dict = IMEditorDict.instance().usePinyinDict();
        SQLiteDatabase db = IMEditorDict.instance().getDB();

        // 在词典表中未收录的拼音不影响词组预测，相应位置置空
        String pinyinCharsStr = "zi,m,zhong,guo";
        String expectedPhrase = "子:zǐ,,中:zhōng,国:guó";
        List<Integer> pinyinCharsIdList = getPinyinCharsIdList(dict, pinyinCharsStr.split(","));

        List<String> phraseList = getTopPhrases(db, pinyinCharsStr, pinyinCharsIdList, 1);
        String bestPhrase = CollectionUtils.first(phraseList);
        Assert.assertEquals(expectedPhrase, bestPhrase);
    }

    @Test
    public void test_top_candidate_words() {
        PinyinDict dict = IMEditorDict.instance().usePinyinDict();
        SQLiteDatabase db = IMEditorDict.instance().getDB();

        String[] samples = new String[] { "zhong", "guo" };
        for (String pinyinChars : samples) {
            Integer pinyinCharsId = dict.getPinyinCharsTree().getCharsId(pinyinChars);
            Assert.assertNotNull(pinyinCharsId);

            Map<Integer, PinyinWord> wordMap = //
                    getAllPinyinWordsByCharsId(db, pinyinCharsId).stream()
                                                                 .collect(Collectors.toMap((w) -> w.id,
                                                                                           Function.identity(),
                                                                                           (a, b) -> a,
                                                                                           HashMap::new));

            int top = 10;
            List<PinyinWord> wordList = //
                    getTopBestPinyinWordIds(db, pinyinCharsId, userPhraseBaseWeight, top).stream()
                                                                                         .map(wordMap::get)
                                                                                         .collect(Collectors.toList());
            Assert.assertTrue(wordList.size() <= top && !wordList.isEmpty());

            String result = wordList.stream()
                                    .map((w) -> w.value + ":" + w.spell.value)
                                    .collect(Collectors.joining(", "));

            Log.i(LOG_TAG, pinyinChars + " => " + result);

            PinyinWord first = getFirstBestPinyinWord(db, pinyinCharsId, userPhraseBaseWeight);
            Assert.assertNotNull(first);
            Log.i(LOG_TAG, pinyinChars + " 的最佳拼音字：" + first.value + ":" + first.spell.value);
        }
    }

    @Test
    public void test_word_variant() {
        PinyinDict dict = IMEditorDict.instance().usePinyinDict();
        SQLiteDatabase db = IMEditorDict.instance().getDB();

        String pinyinChars = "guo";
        Integer pinyinCharsId = dict.getPinyinCharsTree().getCharsId(pinyinChars);
        List<PinyinWord> wordList = getAllPinyinWordsByCharsId(db, pinyinCharsId);

        wordList = wordList.stream().filter((w) -> w.variant != null).collect(Collectors.toList());
        Assert.assertNotEquals(0, wordList.size());

        Log.i(LOG_TAG,
              pinyinChars + " => " + wordList.stream()
                                             .map((w) -> w.value + ":" + w.spell.value + ":" + w.variant)
                                             .collect(Collectors.joining(", ")));
    }

    private List<String> getTop5Phrases(
            SQLiteDatabase db, String pinyinCharsStr, List<Integer> pinyinCharsIdList
    ) {
        return getTopPhrases(db, pinyinCharsStr, pinyinCharsIdList, 5, null);
    }

    private List<String> getTopPhrases(
            SQLiteDatabase db, String pinyinCharsStr, List<Integer> pinyinCharsIdList, int top
    ) {
        return getTopPhrases(db, pinyinCharsStr, pinyinCharsIdList, top, null);
    }

    private List<String> getTopPhrases(
            SQLiteDatabase db, String pinyinCharsStr, List<Integer> pinyinCharsIdList, int top,
            Map<Integer, Integer> confirmedPhraseWords
    ) {
        List<String> phraseList = //
                predictPinyinPhrase( //
                                     db, pinyinCharsIdList, confirmedPhraseWords, //
                                     userPhraseBaseWeight, top //
                ).stream().map((phrase) -> {
                    Map<Integer, PinyinWord> wordMap = getPinyinWordsByWordId(db, new HashSet<>(List.of(phrase)));

                    return Arrays.stream(phrase)
                                 .map(wordMap::get)
                                 .map((w) -> w != null ? w.value + ":" + w.spell.value : "")
                                 .collect(Collectors.joining(","));
                }).collect(Collectors.toList());

        Assert.assertNotEquals(0, phraseList.size());
        phraseList.forEach((phrase) -> {
            Log.i(LOG_TAG, pinyinCharsStr + ": " + phrase);
        });

        return phraseList;
    }

    /** 获取拼音字母组合的 id 列表 */
    public static List<Integer> getPinyinCharsIdList(PinyinDict dict, String... pinyinCharsArray) {
        return getPinyinCharsIdList(dict, List.of(pinyinCharsArray));
    }

    /** 获取拼音字母组合的 id 列表 */
    public static List<Integer> getPinyinCharsIdList(PinyinDict dict, List<String> pinyinCharsList) {
        return pinyinCharsList.stream()
                              .map(chars -> dict.getPinyinCharsTree().getCharsId(chars))
                              .collect(Collectors.toList());
    }
}
