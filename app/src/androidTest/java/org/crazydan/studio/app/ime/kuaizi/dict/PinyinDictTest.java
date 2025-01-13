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
import java.util.List;
import java.util.stream.Collectors;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.crazydan.studio.app.ime.kuaizi.PinyinDictBaseTest;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.crazydan.studio.app.ime.kuaizi.dict.db.HmmDBHelper.saveUsedPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getPinyinWord;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-11-14
 */
@RunWith(AndroidJUnit4.class)
public class PinyinDictTest extends PinyinDictBaseTest {
    private static final String LOG_TAG = PinyinDictTest.class.getSimpleName();

    @Test
    public void test_findTopBestMatchedPhrase() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

        // 预备词库
        String usedPhrase = "这:zhè,是:shì,输:shū,入:rù,法:fǎ";
        List<PinyinWord> phraseWordList = Arrays.stream(usedPhrase.split(",")).map((word) -> {
            String[] splits = word.split(":");
            return getPinyinWord(db, splits[0], splits[1]);
        }).collect(Collectors.toList());
        saveUsedPinyinPhrase(db, phraseWordList, false);

        // 中英文混合的词组预测：英文被忽略掉
        String[] inputCharsArray = new String[] { "zhe", "shi", "Android", "shu", "ru", "fa" };
        List<CharInput> inputs = parseCharInputs(dict, inputCharsArray);

        List<List<InputWord>> phrases = dict.findTopBestMatchedPhrase(inputs, null, 1);
        Assert.assertEquals(1, phrases.size());

        List<InputWord> phrase = phrases.get(0);
        Assert.assertEquals(inputs.size(), phrase.size());

        for (int i = 0; i < inputs.size(); i++) {
            CharInput input = inputs.get(i);
            InputWord word = phrase.get(i);
            input.setWord(word);
        }

        String phraseText = inputs.stream()
                                  .map((input) -> input.hasWord()
                                                  ? input.getWord().toString()
                                                  : input.getJoinedKeyChars())
                                  .collect(Collectors.joining(" "));
        Log.i(LOG_TAG, String.join(",", inputCharsArray) + ": " + phraseText);
        Assert.assertEquals("这(zhè) 是(shì) Android 输(shū) 入(rù) 法(fǎ)", phraseText);
    }

    private List<CharInput> parseCharInputs(PinyinDict dict, String[] texts) {
        return Arrays.stream(texts).map((text) -> {
            CharInput input = CharInput.from(CharKey.from(text));

            if (dict.getPinyinCharsTree().isPinyinCharsInput(input)) {
                // Note: 这里仅用于标记输入为拼音输入
                input.setWord(PinyinWord.build(PinyinWord.Builder.noop));
            }

            return input;
        }).collect(Collectors.toList());
    }
}
