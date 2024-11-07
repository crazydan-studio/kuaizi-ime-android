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

package org.crazydan.studio.app.ime.kuaizi.core.dict.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.dict.Emojis;
import org.crazydan.studio.app.ime.kuaizi.core.input.EmojiWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.DBUtils;

import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteRawQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteRow;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.querySQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.rawQuerySQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.upsertSQLite;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-29
 */
public class PinyinDictDBHelper {

    /** 根据字及其拼音获取其{@link PinyinWord 拼音字对象} */
    public static PinyinWord getPinyinWord(SQLiteDatabase db, String word, String pinyin) {
        List<PinyinWord> wordList = queryPinyinWords(db,
                                                     "py_.word_ = ? and py_.spell_ = ?",
                                                     new String[] { word, pinyin });
        return CollectionUtils.first(wordList);
    }

    /** 根据拼音字 id 获取其 {@link PinyinWord 拼音字对象} */
    public static Map<String, PinyinWord> getPinyinWordsByWordId(SQLiteDatabase db, Set<String> pinyinWordIds) {
        String placeholder = pinyinWordIds.stream().map((id) -> "?").collect(Collectors.joining(", "));

        List<PinyinWord> wordList = queryPinyinWords(db,
                                                     "py_.id_ in (" + placeholder + ")",
                                                     pinyinWordIds.toArray(new String[0]));

        return wordList.stream().collect(Collectors.toMap(PinyinWord::getUid, Function.identity()));
    }

    /**
     * 根据拼音字母组合 id 获取其对应的全部{@link PinyinWord 拼音字对象}
     * <p/>
     * 返回结果已按拼音声调、字形权重排序
     */
    public static List<PinyinWord> getAllPinyinWordsByCharsId(SQLiteDatabase db, String pinyinCharsId) {
        return queryPinyinWords(db, "py_.spell_chars_id_ = ?", new String[] { pinyinCharsId });
    }

    /**
     * 根据拼音字母组合 id 获取其对应的前 <code>top</code> 个拼音字 id
     *
     * @return 结果拼音字的权重均大于 0
     */
    public static List<String> getTopBestPinyinWordIds(
            SQLiteDatabase db, String pinyinCharsId, int userPhraseBaseWeight, int top
    ) {
        return rawQuerySQLite(db, new SQLiteRawQueryParams<String>() {{
            this.sql = "select distinct"
                       + "   word_id_,"
                       + "   ( ifnull(weight_app_, 0) +"
                       + "     ifnull(weight_user_, 0) +"
                       // 补充用户输入的基础权重
                       // Note: SQLite 3.32.0 版本才支持 iif
                       // https://sqlite.org/forum/info/97a66708939d518e
                       + "     (case when ifnull(weight_user_, 0) > 0 then ? else 0 end)"
                       // + "     iif(ifnull(weight_user_, 0) > 0, ?, 0)"
                       + "   ) used_weight_"
                       + " from phrase_word"
                       + " where used_weight_ > 0 and spell_chars_id_ = ?"
                       + " order by used_weight_ desc"
                       + " limit ?";

            this.params = new String[] { userPhraseBaseWeight + "", pinyinCharsId, top + "" };

            this.reader = (row) -> row.getString("word_id_");
        }});
    }

    /**
     * 查询拼音字表 pinyin_word 以获得{@link PinyinWord 拼音字对象}列表
     */
    private static List<PinyinWord> queryPinyinWords(
            SQLiteDatabase db, String queryWhere, String[] queryParams
    ) {
        return rawQuerySQLite(db, new SQLiteRawQueryParams<PinyinWord>() {
            {
                this.sql = "select distinct"
                           + "   py_.id_, py_.word_, py_.word_id_,"
                           + "   py_.spell_, py_.spell_id_, py_.spell_chars_id_,"
                           + "   py_.traditional_,"
                           + "   py_.radical_, py_.radical_stroke_count_"
                           + " from pinyin_word py_"
                           + (" where " + queryWhere)
                           + " order by"
                           // 按拼音字的拼音字母顺序（spell_id_）、字形相似性（glyph_weight_）排序
                           + "   py_.spell_id_ asc, py_.glyph_weight_ desc";
                this.params = queryParams;

                this.reader = PinyinDictDBHelper::createPinyinWord;
            }
        });
    }

    /** 根据表情符号获取其 {@link EmojiWord 表情对象} */
    public static EmojiWord getEmoji(SQLiteDatabase db, String emoji) {
        List<EmojiWord> emojiList = querySQLite(db, new SQLiteQueryParams<EmojiWord>() {{
            this.table = "meta_emoji";
            this.columns = new String[] { "id_", "value_", "weight_user_ as weight_" };
            this.where = "value_ = ?";
            this.params = new String[] { emoji };

            this.reader = PinyinDictDBHelper::createEmojiWord;
        }});

        return CollectionUtils.first(emojiList);
    }

    /**
     * 获取各分组下的所有表情
     *
     * @param groupGeneralCount
     *         {@link Emojis#GROUP_GENERAL} 分组中的表情数量
     */
    public static Emojis getAllGroupedEmojis(SQLiteDatabase db, int groupGeneralCount) {
        Map<String, List<InputWord>> groups = new LinkedHashMap<>();
        // Note: 确保常用始终在第一的位置
        groups.put(Emojis.GROUP_GENERAL, new ArrayList<>());

        rawQuerySQLite(db, new SQLiteRawQueryParams<Void>() {{
            // Note: 确保 常用 分组的结果在最前面
            this.sql = "select * from (" //
                       + "  select id_, value_, weight_, ? as group_" //
                       + "  from emoji" //
                       + "  where weight_ > 0" //
                       + "  order by weight_ desc, id_ asc" //
                       + "  limit ?" //
                       + ")" //
                       + "union" //
                       + "  select id_, value_, weight_, group_" //
                       + "  from emoji" //
                       + "  order by group_ asc, id_ asc";
            this.params = new String[] { Emojis.GROUP_GENERAL, groupGeneralCount + "" };

            this.reader = (row) -> {
                String group = row.getString("group_");
                EmojiWord emoji = createEmojiWord(row);

                if (emoji != null) {
                    groups.computeIfAbsent(group, (k) -> new ArrayList<>(500)).add(emoji);
                }
                return null;
            };
        }});

        return new Emojis(groups);
    }

    /** 根据关键字的字 id 获取表情 */
    public static List<EmojiWord> getEmojisByKeyword(SQLiteDatabase db, List<String[]> keywordIdsList, int top) {
        List<String> args = new ArrayList<>(keywordIdsList.size() * 4);
        keywordIdsList.forEach((keywordIds) -> {
            String ids = String.join(",", keywordIds);
            args.add("%[" + ids + "]%");
            args.add("%," + ids + "]%");
            args.add("%[" + ids + ",%");
            args.add("%," + ids + ",%");
        });

        return querySQLite(db, new SQLiteQueryParams<EmojiWord>() {{
            this.table = "meta_emoji";
            this.columns = new String[] { "id_", "value_", "weight_user_ as weight_" };
            this.where = args.stream().map((arg) -> "keyword_ids_list_ like ?").collect(Collectors.joining(" or "));
            this.params = args.toArray(new String[0]);
            this.orderBy = "weight_ desc, id_ asc";
            this.limit = top + "";

            this.reader = PinyinDictDBHelper::createEmojiWord;
        }});
    }

    /**
     * 更新表情的使用信息
     *
     * @param reverse
     *         是否反向更新，即，减掉对表情的使用权重
     */
    public static void saveUsedEmojis(SQLiteDatabase db, Collection<String> emojiIds, boolean reverse) {
        List<String[]> argsList = statsWeightArgsList(emojiIds);

        if (!reverse) {
            execSQLite(db, "update meta_emoji" //
                           + " set weight_user_ = weight_user_ + ?" //
                           + " where id_ = ?", argsList);
        } else {
            execSQLite(db, "update meta_emoji" //
                           + " set weight_user_ = max(weight_user_ - ?, 0)" //
                           + " where id_ = ?", argsList);
        }
    }

    /** 获取以指定字符开头的拉丁文，并按使用权重降序排序返回 */
    public static List<String> getLatinsByStarts(SQLiteDatabase db, String text, int top) {
        return querySQLite(db, new SQLiteQueryParams<String>() {{
            this.table = "meta_latin";
            this.columns = new String[] { "value_" };
            // like 为大小写不敏感的匹配，glob 为大小写敏感匹配
            // https://www.sqlitetutorial.net/sqlite-glob/
            this.where = "weight_user_ > 0 and value_ glob ?";
            this.params = new String[] { text + "*" };
            this.orderBy = "weight_user_ desc, id_ asc";
            this.limit = top + "";

            this.reader = (row) -> row.getString("value_");
        }});
    }

    /**
     * 更新拉丁文的使用信息
     *
     * @param reverse
     *         是否反向更新，即，减掉对拉丁文的使用权重
     */
    public static void saveUsedLatins(SQLiteDatabase db, Collection<String> latins, boolean reverse) {
        List<String[]> argsList = statsWeightArgsList(latins);

        if (!reverse) {
            upsertSQLite(db, new DBUtils.SQLiteRawUpsertParams() {{
                // Note: 确保更新和新增的参数位置相同
                this.updateSQL = "update meta_latin set weight_user_ = weight_user_ + ? where value_ = ?";
                this.insertSql = "insert into meta_latin(weight_user_, value_) values(?, ?)";

                this.updateParamsList = this.insertParamsList = argsList;
            }});
        } else {
            execSQLite(db, "update meta_latin" //
                           + " set weight_user_ = max(weight_user_ - ?, 0)" //
                           + " where value_ = ?", argsList);
        }

        if (reverse) {
            // 清理无用数据
            execSQLite(db, new String[] {
                    "delete from meta_latin where weight_user_ = 0",
                    });
        }
    }

    /** 向{@link PinyinWord 拼音字}附加其繁/简变体 */
    public static void attachVariantToPinyinWord(SQLiteDatabase db, Collection<PinyinWord> pinyinWordList) {
        Map<String, List<PinyinWord>> pinyinWordMap = pinyinWordList.stream()
                                                                    .collect(Collectors.groupingBy(PinyinWord::getWordId,
                                                                                                   HashMap::new,
                                                                                                   Collectors.toCollection(
                                                                                                           ArrayList::new)));

        rawQuerySQLite(db, new SQLiteRawQueryParams<Void>() {{
            String placeholder = pinyinWordMap.keySet().stream().map((k) -> "?").collect(Collectors.joining(", "));

            this.params = pinyinWordMap.keySet().toArray(new String[0]);
            this.sql = "select t_.* from (" //
                       + "   select source_id_, target_id_, target_value_" //
                       + "   from simple_word" //
                       + "   union" //
                       + "   select source_id_, target_id_, target_value_" //
                       + "   from traditional_word" //
                       + " ) t_" //
                       + " where t_.source_id_ in (" + placeholder + ")";

            this.reader = (row) -> {
                String sourceWordId = row.getString("source_id_");
                List<PinyinWord> sourceWords = pinyinWordMap.get(sourceWordId);
                assert sourceWords != null;

                String targetWordId = row.getString("target_id_");
                String targetWordValue = row.getString("target_value_");
                List<PinyinWord> targetWords = pinyinWordMap.get(targetWordId);

                sourceWords.forEach((sourceWord) -> {
                    if (sourceWord.getVariant() != null) {
                        return;
                    }

                    // 适用于为某个拼音下的所有候选字附加变体
                    if (!CollectionUtils.isEmpty(targetWords)) {
                        // 繁/简字的拼音需一致
                        targetWords.forEach((targetWord) -> {
                            if (targetWord.getNotation().equals(sourceWord.getNotation())) {
                                sourceWord.setVariant(targetWordValue);
                            }
                        });
                    }
                    // 适用于为不同拼音的字附加各自的变体
                    else {
                        sourceWord.setVariant(targetWordValue);
                    }
                });

                return null;
            };
        }});
    }

    /** 获取指定汉字的字 id */
    public static String getWordId(SQLiteDatabase db, String word) {
        List<String> wordIdList = querySQLite(db, new SQLiteQueryParams<String>() {{
            this.table = "meta_word";
            this.columns = new String[] { "id_" };
            this.where = "value_ = ?";
            this.params = new String[] { word };

            this.reader = (row) -> row.getString("id_");
        }});

        return CollectionUtils.first(wordIdList);
    }

    private static PinyinWord createPinyinWord(SQLiteRow row) {
        // 拼音字 id
        String uid = row.getString("id_");
        // 字 id
        String wordId = row.getString("word_id_");
        // 字
        String wordValue = row.getString("word_");

        // 拼音 id
        int spellId = row.getInt("spell_id_");
        // 拼音
        String spellValue = row.getString("spell_");
        // 拼音字母组合 id
        String spellCharsId = row.getString("spell_chars_id_");
        PinyinWord.Spell spell = new PinyinWord.Spell(spellId, spellValue, spellCharsId);

        boolean traditional = row.getInt("traditional_") > 0;

        String radicalValue = row.getString("radical_");
        int radicalStrokeCount = row.getInt("radical_stroke_count_");
        PinyinWord.Radical radical = new PinyinWord.Radical(radicalValue, radicalStrokeCount);

        return new PinyinWord(uid, wordValue, wordId, spell, radical, traditional);
    }

    private static EmojiWord createEmojiWord(SQLiteRow row) {
        String uid = row.getString("id_");
        String value = row.getString("value_");
        int weight = row.getInt("weight_");

        EmojiWord word = null;
        if (CharUtils.isPrintable(value)) {
            word = new EmojiWord(uid, value);
            word.setWeight(weight);
        }
        return word;
    }

    /** 统计字符串列表中的字符串权重，并返回 SQLite 参数列表：<code>[[weight, str], [...], ...]</code> */
    private static List<String[]> statsWeightArgsList(Collection<String> list) {
        Map<String, Integer> argsMap = new HashMap<>(list.size());
        list.forEach((str) -> {
            argsMap.compute(str, (k, v) -> (v == null ? 0 : v) + 1);
        });

        List<String[]> argsList = new ArrayList<>(argsMap.size());
        argsMap.forEach((str, weight) -> {
            argsList.add(new String[] { weight + "", str });
        });

        return argsList;
    }
}
