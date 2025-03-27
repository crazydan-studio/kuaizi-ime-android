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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.crazydan.studio.app.ime.kuaizi.IMEditorDict;
import org.crazydan.studio.app.ime.kuaizi.IMEditorDictBaseTest;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.EmojiWord;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.querySQLite;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDBHelper.getWordId;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputDataDBHelper.enableAllPrintableEmojis;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputDataDBHelper.getAllGroupedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputDataDBHelper.getEmoji;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputDataDBHelper.getEmojisByKeyword;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputDataDBHelper.getLatinsByStarts;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputDataDBHelper.saveUsedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputDataDBHelper.saveUsedLatins;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-28
 */
@RunWith(AndroidJUnit4.class)
public class UserInputDataDBHelperTest extends IMEditorDictBaseTest {
    private static final String LOG_TAG = UserInputDataDBHelperTest.class.getSimpleName();

    @Test
    public void test_query_grouped_emojis() {
        IMEditorDict dict = IMEditorDict.instance();
        SQLiteDatabase db = dict.getDB();

        int top = 10;
        Emojis emojis = getAllGroupedEmojis(db, top);
        emojis.groups.forEach((group, emojiList) -> {
            Log.i(LOG_TAG,
                  group + ": " + emojiList.stream()
                                          .map((emoji) -> emoji.value + ":" + emoji.id)
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
                "\uD83C\uDF83", // 🎃
                "\uD83C\uDF47", // 🍇
        };
        List<Integer> usedEmojiIdList = Arrays.stream(usedEmojis)
                                              .map((emoji) -> getEmoji(db, emoji).id)
                                              .collect(Collectors.toList());
        saveUsedEmojis(db, usedEmojiIdList, false);

        emojis = getAllGroupedEmojis(db, top);
        List<InputWord> generalEmojiList = emojis.groups.get(Emojis.GROUP_GENERAL);
        Assert.assertNotNull(generalEmojiList);

        // Note: 直接调用 LinkedHashSet#toArray 将报方法不存在
        Assert.assertArrayEquals(new LinkedHashSet<>(usedEmojiIdList).toArray(new Integer[0]),
                                 generalEmojiList.stream().map((w) -> w.id).toArray(Integer[]::new));
        Log.i(LOG_TAG,
              Emojis.GROUP_GENERAL + ": " + generalEmojiList.stream()
                                                            .map((w) -> w.value + ":" + w.id + ":" + w.weight)
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
        IMEditorDict dict = IMEditorDict.instance();
        SQLiteDatabase db = dict.getDB();

        List<Integer[]> keywordIdsList = Stream.of("地球", "笑脸")
                                               .map((keyword) -> keyword.chars()
                                                                        .mapToObj((w) -> getWordId(db,
                                                                                                   String.valueOf((char) w)))
                                                                        .toArray(Integer[]::new))
                                               .collect(Collectors.toList());
        List<EmojiWord> emojiList = getEmojisByKeyword(db, keywordIdsList, 10);

        Assert.assertNotEquals(0, emojiList.size());
        Log.i(LOG_TAG, emojiList.stream().map((w) -> w.value).collect(Collectors.joining(", ")));
    }

    @Test
    public void test_enable_all_printable_emojis() {
        IMEditorDict dict = IMEditorDict.instance();
        SQLiteDatabase db = dict.getDB();

        List<String> notPrintableEmojis = querySQLite(db, new DBUtils.SQLiteQueryParams<String>() {{
            this.table = "meta_emoji";
            this.columns = new String[] { "value_" };

            this.reader = (row) -> {
                String value = row.getString("value_");

                if (!CharUtils.isPrintable(value)) {
                    return value;
                }
                return null;
            };
        }});
        Assert.assertFalse(notPrintableEmojis.isEmpty());
        Log.i(LOG_TAG,
              "Not printable emojis (" + notPrintableEmojis.size() + "): " + String.join(", ", notPrintableEmojis));

        enableAllPrintableEmojis(db);

        Emojis emojis = getAllGroupedEmojis(db, 0);

        emojis.groups.forEach((group, list) -> {
            list.forEach(emoji -> {
                Assert.assertTrue(CharUtils.isPrintable(emoji.value));
            });
        });
    }

    @Test
    public void test_query_latins() {
        IMEditorDict dict = IMEditorDict.instance();
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
}
