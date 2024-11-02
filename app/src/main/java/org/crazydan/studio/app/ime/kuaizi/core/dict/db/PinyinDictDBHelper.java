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
import org.crazydan.studio.app.ime.kuaizi.core.input.EmojiInputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;

import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteRawQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteRow;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.querySQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.rawQuerySQLite;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-29
 */
public class PinyinDictDBHelper {

    /** 根据字及其拼音获取其{@link PinyinInputWord 拼音字对象} */
    public static PinyinInputWord getPinyinInputWord(SQLiteDatabase db, String word, String pinyin) {
        List<PinyinInputWord> wordList = queryPinyinInputWords(db,
                                                               "py_.word_ = ? and py_.spell_ = ?",
                                                               new String[] { word, pinyin },
                                                               false);
        return CollectionUtils.first(wordList);
    }

    /** 根据拼音字 id 获取其 {@link PinyinInputWord 拼音字对象} */
    public static Map<String, PinyinInputWord> getPinyinInputWords(SQLiteDatabase db, Set<String> pinyinWordIds) {
        String placeholder = pinyinWordIds.stream().map((id) -> "?").collect(Collectors.joining(", "));

        List<PinyinInputWord> wordList = queryPinyinInputWords(db,
                                                               "py_.id_ in (" + placeholder + ")",
                                                               pinyinWordIds.toArray(new String[0]),
                                                               false);

        return wordList.stream().collect(Collectors.toMap(PinyinInputWord::getUid, Function.identity()));
    }

    /**
     * 根据拼音字母组合 id 获取其对应的全部{@link PinyinInputWord 拼音字对象}
     * <p/>
     * 返回结果已按使用和字形权重等排序
     */
    public static List<PinyinInputWord> getAllPinyinInputWords(
            SQLiteDatabase db, String pinyinCharsId, int userPhraseBaseWeight
    ) {
        return queryPinyinInputWords(db, "py_.spell_chars_id_ = ?", new String[] {
                // Note: 注意占位参数的位置
                userPhraseBaseWeight + "", pinyinCharsId
        }, true);
    }

    /**
     * 查询拼音字表 pinyin_word 以获得{@link PinyinInputWord 拼音字对象}列表
     *
     * @param sort
     *         若为 <code>true</code>，则结果依次按使用权重、拼音字母顺序、字形相似性排序
     */
    public static List<PinyinInputWord> queryPinyinInputWords(
            SQLiteDatabase db, String queryWhere, String[] queryParams, boolean sort
    ) {
        return rawQuerySQLite(db, new SQLiteRawQueryParams<PinyinInputWord>() {{
            this.sql = "select distinct"
                       + "   py_.id_, py_.word_, py_.word_id_,"
                       + "   py_.spell_, py_.spell_id_, py_.spell_chars_id_,"
                       + "   py_.traditional_, py_.stroke_order_,"
                       + "   py_.radical_, py_.radical_stroke_count_";
            if (sort) {
                this.sql += ",   ( ifnull(ph_.weight_app_, 0) +" //
                            + "     ifnull(ph_.weight_user_, 0) +"
                            // 补充用户输入的基础权重
                            // Note: 低版本 SQLite 不支持 iif，需采用 case when
                            + "     (case when ifnull(ph_.weight_user_, 0) > 0 then ? else 0 end)"
                            // + "     iif(ifnull(ph_.weight_user_, 0) > 0, ?, 0)"
                            + "   ) used_weight_";
            } else { // 确保列取值正常
                this.sql += ",   0 as used_weight_";
            }

            this.sql += " from pinyin_word py_";
            if (sort) {
                this.sql += " left join phrase_word ph_ on ph_.word_id_ = py_.id_";
            }

            this.sql += " where " + queryWhere;
            if (sort) {
                this.sql += " order by"
                            // 短语中的常用字最靠前
                            + "   used_weight_ desc,"
                            // 再按拼音字的拼音字母顺序（spell_id_）、字形相似性（glyph_weight_）排序
                            + "   py_.spell_id_ asc, py_.glyph_weight_ desc";
            }

            this.params = queryParams;

            this.reader = PinyinDictDBHelper::createPinyinInputWord;
        }});
    }

    /** 根据表情符号获取其 {@link EmojiInputWord 表情对象} */
    public static EmojiInputWord getEmoji(SQLiteDatabase db, String emoji) {
        List<EmojiInputWord> emojiList = querySQLite(db, new SQLiteQueryParams<EmojiInputWord>() {{
            this.table = "meta_emoji";
            this.columns = new String[] { "id_", "value_", "weight_user_ as weight_" };
            this.where = "value_ = ?";
            this.params = new String[] { emoji };

            this.reader = PinyinDictDBHelper::createEmojiInputWord;
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
                EmojiInputWord emoji = createEmojiInputWord(row);

                if (emoji != null) {
                    groups.computeIfAbsent(group, (k) -> new ArrayList<>(500)).add(emoji);
                }
                return null;
            };
        }});

        return new Emojis(groups);
    }

    /** 根据关键字的字 id 获取表情 */
    public static List<EmojiInputWord> getEmojisByKeyword(SQLiteDatabase db, List<String[]> keywordIdsList, int top) {
        List<String> args = new ArrayList<>(keywordIdsList.size() * 4);
        keywordIdsList.forEach((keywordIds) -> {
            String ids = String.join(",", keywordIds);
            args.add("%[" + ids + "]%");
            args.add("%," + ids + "]%");
            args.add("%[" + ids + ",%");
            args.add("%," + ids + ",%");
        });

        return querySQLite(db, new SQLiteQueryParams<EmojiInputWord>() {{
            this.table = "meta_emoji";
            this.columns = new String[] { "id_", "value_", "weight_user_ as weight_" };
            this.where = args.stream().map((arg) -> "keyword_ids_list_ like ?").collect(Collectors.joining(" or "));
            this.params = args.toArray(new String[0]);
            this.orderBy = "weight_ desc, id_ asc";
            this.limit = top + "";

            this.reader = PinyinDictDBHelper::createEmojiInputWord;
        }});
    }

    /**
     * 更新使用的表情
     *
     * @param reverse
     *         是否反向更新，即，减掉对表情的使用权重
     */
    public static void saveUsedEmojis(SQLiteDatabase db, Collection<String> emojiIds, boolean reverse) {
        Map<String, Integer> argsMap = new HashMap<>(emojiIds.size());
        emojiIds.forEach((emojiId) -> {
            argsMap.compute(emojiId, (k, v) -> (v == null ? 0 : v) + 1);
        });

        List<String[]> argsList = new ArrayList<>(argsMap.size());
        argsMap.forEach((emojiId, weight) -> {
            argsList.add(new String[] { weight + "", emojiId });
        });

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

    /** 向{@link PinyinInputWord 拼音字}附加其繁/简变体 */
    public static void attachVariantToPinyinInputWord(SQLiteDatabase db, Collection<PinyinInputWord> pinyinWordList) {
        Map<String, List<PinyinInputWord>> pinyinWordMap = pinyinWordList.stream()
                                                                         .collect(Collectors.groupingBy(PinyinInputWord::getWordId,
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
                List<PinyinInputWord> sourceWords = pinyinWordMap.get(sourceWordId);
                assert sourceWords != null;

                String targetWordId = row.getString("target_id_");
                String targetWordValue = row.getString("target_value_");
                List<PinyinInputWord> targetWords = pinyinWordMap.get(targetWordId);

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

    private static PinyinInputWord createPinyinInputWord(SQLiteRow row) {
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
        PinyinInputWord.Spell spell = new PinyinInputWord.Spell(spellId, spellValue, spellCharsId);

        int usedWeight = row.getInt("used_weight_");

        boolean traditional = row.getInt("traditional_") > 0;
        String strokeOrder = row.getString("stroke_order_");

        String radicalValue = row.getString("radical_");
        int radicalStrokeCount = row.getInt("radical_stroke_count_");
        PinyinInputWord.Radical radical = new PinyinInputWord.Radical(radicalValue, radicalStrokeCount);

        PinyinInputWord word = new PinyinInputWord(uid, wordValue, wordId, spell, radical, traditional, strokeOrder);
        word.setWeight(usedWeight);

        return word;
    }

    private static EmojiInputWord createEmojiInputWord(SQLiteRow row) {
        String uid = row.getString("id_");
        String value = row.getString("value_");
        int weight = row.getInt("weight_");

        EmojiInputWord word = null;
        if (CharUtils.isPrintable(value)) {
            word = new EmojiInputWord(uid, value);
            word.setWeight(weight);
        }
        return word;
    }
}
