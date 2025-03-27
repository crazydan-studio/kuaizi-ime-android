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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper;

import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.SQLiteQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.querySQLite;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.HmmDBHelper.predictPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getAllPinyinWordsByCharsId;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getEmojisByKeyword;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getFirstBestPinyinWord;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getPinyinWordsByWordId;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getTopBestPinyinWordIds;

/**
 * 拼音字典
 * <p/>
 * 应用内置的拼音字典数据库的表结构和数据生成见
 * <a href="https://github.com/crazydan-studio/kuaizi-ime/blob/master/tools/pinyin-dict/src/generate/sqlite/ime/index.mjs">kuaizi-ime/tools/pinyin-dict</a>
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-27
 */
public class PinyinDict extends BaseDBDict {
    /** 用户词组数据的基础权重，以确保用户输入权重大于应用词组数据 */
    private final int userPhraseBaseWeight = 500;

    private final PinyinCharsTree pinyinCharsTree;

    public PinyinDict(SQLiteDatabase db, ThreadPoolExecutor executor, PinyinCharsTree pinyinCharsTree) {
        super(db, executor);
        this.pinyinCharsTree = pinyinCharsTree;
    }

    public PinyinCharsTree getPinyinCharsTree() {
        return this.pinyinCharsTree;
    }

    /** 通过字及其读音获取 {@link PinyinWord} 对象 */
    public PinyinWord getWord(String word, String pinyin) {
        return PinyinDictDBHelper.getPinyinWord(this.db, word, pinyin);
    }

    /** 获取指定拼音的候选拼音字列表：已按权重等排序 */
    public Map<Integer, InputWord> getCandidates(CharInput input) {
        Integer pinyinCharsId = getPinyinCharsTree().getCharsId(input);

        return getAllPinyinWordsByCharsId(this.db, pinyinCharsId).stream()
                                                                 .collect(Collectors.toMap((w) -> w.id,
                                                                                           Function.identity(),
                                                                                           (a, b) -> a,
                                                                                           // 保持候选字的顺序不变
                                                                                           LinkedHashMap::new));
    }

    /**
     * 获取指定拼音的第一个最佳候选字
     * <p/>
     * 优先选择使用权重最高的，否则，选择候选字列表中的第一个
     */
    public PinyinWord getFirstBestCandidate(Integer pinyinCharsId) {
        return getFirstBestPinyinWord(this.db, pinyinCharsId, this.userPhraseBaseWeight);
    }

    /** 获取指定拼音的前 <code>top</code> 个高权重的候选拼音字 id */
    public List<Integer> getTopBestCandidateIds(CharInput input, int top) {
        Integer pinyinCharsId = getPinyinCharsTree().getCharsId(input);

        return getTopBestPinyinWordIds(this.db, pinyinCharsId, this.userPhraseBaseWeight, top);
    }

    /**
     * 根据输入的拼音，查找最靠前的 <code>top</code> 个拼音短语
     *
     * @param currentInput
     *         当前输入。若不为 null，则在该输入之前的输入候选字均视为已确认，
     *         不会被预测结果替换，而在其之后的输入，仅已确认的候选字才不会被替换
     */
    public List<List<InputWord>> findTopBestMatchedPhrase(List<CharInput> inputs, CharInput currentInput, int top) {
        int total = inputs.size();
        if (total < 2) {
            return List.of();
        }

        int lastAutoConfirmedUntilIndex = inputs.indexOf(currentInput);
        Map<Integer, Integer> pinyinCharsPlaceholderMap = new HashMap<>(total);

        List<Integer> pinyinCharsIdList = new ArrayList<>(total);
        Map<Integer, Integer> confirmedPhraseWords = new HashMap<>(total);
        for (int i = 0; i < total; i++) {
            CharInput input = inputs.get(i);
            // Note: 英文字符也可能组成有效拼音，故而，需仅针对拼音键盘的输入
            if (!CharInput.isPinyin(input)) {
                continue;
            }

            String chars = input.getJoinedKeyChars();
            Integer charsId = getPinyinCharsTree().getCharsId(chars);
            if (charsId == null) {
                continue;
            }

            int charsIndex = pinyinCharsIdList.size();
            if (i < lastAutoConfirmedUntilIndex || input.isWordConfirmed()) {
                confirmedPhraseWords.put(charsIndex, input.getWord().id);
            }

            pinyinCharsPlaceholderMap.put(i, charsIndex);
            pinyinCharsIdList.add(charsId);
        }

        List<Integer[]> phraseWordsList = predictPinyinPhrase(this.db,
                                                              pinyinCharsIdList,
                                                              confirmedPhraseWords,
                                                              this.userPhraseBaseWeight,
                                                              top);
        if (phraseWordsList.isEmpty()) {
            return List.of();
        }

        Set<Integer> pinyinWordIds = new HashSet<>();
        phraseWordsList.forEach(wordIds -> pinyinWordIds.addAll(List.of(wordIds)));

        Map<Integer, PinyinWord> pinyinWordMap = getPinyinWordsByWordId(this.db, pinyinWordIds);

        BiFunction<Integer[], Integer, InputWord> getWord = (wordIds, inputIndex) -> {
            Integer pinyinCharsIndex = pinyinCharsPlaceholderMap.get(inputIndex);
            if (pinyinCharsIndex == null) {
                return null;
            }

            Integer wordId = wordIds[pinyinCharsIndex];
            return pinyinWordMap.get(wordId);
        };

        return phraseWordsList.stream().map((wordIds) -> {
            List<InputWord> list = new ArrayList<>(inputs.size());

            // 按拼音所在的位置填充拼音字
            for (int i = 0; i < inputs.size(); i++) {
                InputWord word = getWord.apply(wordIds, i);
                list.add(word);
            }
            return list;
        }).collect(Collectors.toList());
    }

    /** 根据拼音输入短语的后 4 个字作为关键字查询得到最靠前的 <code>top</code> 个表情 */
    public List<InputWord> findTopBestEmojisMatchedPhrase(List<PinyinWord> phraseWords, int top) {
        if (phraseWords.isEmpty()) {
            return List.of();
        }

        List<Integer> wordGlyphIdList = phraseWords.stream().map((w) -> w.glyphId).collect(Collectors.toList());

        int tries = 4;
        int total = wordGlyphIdList.size();
        List<Integer[]> keywordIdsList = new ArrayList<>(tries);
        for (int i = total - 1; i >= 0 && i >= total - tries; i--) {
            Integer[] keywordIds = wordGlyphIdList.subList(i, total).toArray(new Integer[0]);
            keywordIdsList.add(keywordIds);
        }

        return getEmojisByKeyword(this.db, keywordIdsList, top).stream()
                                                               .map((word) -> (InputWord) word)
                                                               .collect(Collectors.toList());
    }

    public static PinyinCharsTree createPinyinCharsTree(SQLiteDatabase db) {
        Map<String, Integer> pinyinCharsAndIdMap = new HashMap<>(600);

        querySQLite(db, new SQLiteQueryParams<Void>() {{
            this.table = "meta_pinyin_chars";
            this.columns = new String[] { "id_", "value_" };

            this.voidReader = (row) -> {
                pinyinCharsAndIdMap.put(row.getString("value_"), row.getInt("id_"));
            };
        }});

        return PinyinCharsTree.create(pinyinCharsAndIdMap);
    }
}
