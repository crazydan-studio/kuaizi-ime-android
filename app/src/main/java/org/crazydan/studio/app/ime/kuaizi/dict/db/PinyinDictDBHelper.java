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

package org.crazydan.studio.app.ime.kuaizi.dict.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils;
import org.crazydan.studio.app.ime.kuaizi.dict.Emojis;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.input.word.EmojiWord;
import org.crazydan.studio.app.ime.kuaizi.pane.input.word.PinyinWord;

import static org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils.isBlank;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils.subList;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.SQLiteQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.SQLiteRawQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.SQLiteRow;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.querySQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.rawQuerySQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.upsertSQLite;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-29
 */
public class PinyinDictDBHelper {

    /** 根据字及其拼音获取其{@link PinyinWord 拼音字对象} */
    public static PinyinWord getPinyinWord(SQLiteDatabase db, String word, String pinyin) {
        List<PinyinWord> wordList = queryPinyinWords(db,
                                                     "py_.word_ = ? and py_.spell_ = ?",
                                                     new String[] { word, pinyin },
                                                     null);
        return CollectionUtils.first(wordList);
    }

    /** 根据拼音字 id 获取其 {@link PinyinWord 拼音字对象} */
    public static Map<Integer, PinyinWord> getPinyinWordsByWordId(SQLiteDatabase db, Set<Integer> pinyinWordIds) {
        String placeholder = pinyinWordIds.stream().map((id) -> "?").collect(Collectors.joining(", "));

        List<PinyinWord> wordList = queryPinyinWords(db,
                                                     "py_.id_ in (" + placeholder + ")",
                                                     pinyinWordIds.stream()
                                                                  .map(Objects::toString)
                                                                  .toArray(String[]::new),
                                                     null);

        return wordList.stream().collect(Collectors.toMap((w) -> w.id, Function.identity()));
    }

    /**
     * 根据拼音字母组合 id 获取其对应的全部{@link PinyinWord 拼音字对象}
     * <p/>
     * 返回结果已按拼音声调、字形权重排序
     */
    public static List<PinyinWord> getAllPinyinWordsByCharsId(SQLiteDatabase db, Integer pinyinCharsId) {
        return queryPinyinWords(db, "py_.spell_chars_id_ = ?", new String[] { pinyinCharsId + "" }, null);
    }

    /**
     * 根据拼音字母组合 id 获取其对应的前 <code>top</code> 个拼音字 id
     *
     * @return 结果拼音字的权重均大于 0
     */
    public static List<Integer> getTopBestPinyinWordIds(
            SQLiteDatabase db, Integer pinyinCharsId, int userPhraseBaseWeight, int top
    ) {
        return rawQuerySQLite(db, new SQLiteRawQueryParams<Integer>() {{
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

            this.params = new String[] { userPhraseBaseWeight + "", pinyinCharsId + "", top + "" };

            this.reader = (row) -> row.getInt("word_id_");
        }});
    }

    /**
     * 根据拼音字母组合 id 获取其第一个最佳拼音字 id
     * <p/>
     * 首先从词典库中选择使用权重最高的拼音字，若不存在，则从拼音的候选字列表中选择第一个
     */
    public static PinyinWord getFirstBestPinyinWord(
            SQLiteDatabase db, Integer pinyinCharsId, int userPhraseBaseWeight
    ) {
        List<Integer> wordIds = getTopBestPinyinWordIds(db, pinyinCharsId, userPhraseBaseWeight, 1);
        Integer wordId = CollectionUtils.first(wordIds);

        Collection<PinyinWord> words;
        if (wordId != null) {
            words = getPinyinWordsByWordId(db, Set.of(wordId)).values();
        } else {
            words = queryPinyinWords(db, "py_.spell_chars_id_ = ?", new String[] { pinyinCharsId + "" }, 1);
        }

        return CollectionUtils.first(words);
    }

    /**
     * 查询拼音字表 pinyin_word 以获得{@link PinyinWord 拼音字对象}列表
     * <p/>
     * 注：拼音字对象已包含其繁/简体
     */
    private static List<PinyinWord> queryPinyinWords(
            SQLiteDatabase db, String queryWhere, String[] queryParams, Integer limit
    ) {
        return rawQuerySQLite(db, new SQLiteRawQueryParams<PinyinWord>() {
            {
                this.sql = "select distinct"
                           + "   py_.id_, py_.word_, py_.word_id_,"
                           + "   py_.spell_, py_.spell_id_, py_.spell_chars_id_,"
                           + "   py_.traditional_,"
                           + "   py_.radical_, py_.radical_stroke_count_,"
                           + "   py_.variant_"
                           + " from pinyin_word py_"
                           + (" where " + queryWhere)
                           + " order by"
                           // 按拼音字的使用权重（used_weight_）、字形相似性（glyph_weight_）排序
                           + "   py_.used_weight_ desc, py_.glyph_weight_ desc"
                           + (limit != null ? " limit " + limit : "");

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
        int listCapacity = 500;
        Map<String, List<InputWord>> groups = new LinkedHashMap<>();
        // Note: 确保常用始终在第一的位置
        List<InputWord> general = new ArrayList<>(listCapacity);
        groups.put(Emojis.GROUP_GENERAL, general);

        rawQuerySQLite(db, new SQLiteRawQueryParams<Void>() {{
            // 非常用分组的表情保持其位置不变，以便于快速翻阅
            this.sql = "select id_, value_, weight_, group_" //
                       + " from emoji" //
                       + " where enabled_ = 1" //
                       + " order by group_ asc, id_ asc";

            this.voidReader = (row) -> {
                String group = row.getString("group_");
                EmojiWord emoji = createEmojiWord(row);

                if (emoji.weight > 0) {
                    general.add(emoji);
                }
                groups.computeIfAbsent(group, (k) -> new ArrayList<>(listCapacity)).add(emoji);
            };
        }});

        // 按使用权重收集常用表情
        general.sort((a, b) -> b.weight - a.weight);
        groups.put(Emojis.GROUP_GENERAL, subList(general, 0, groupGeneralCount));

        return new Emojis(groups);
    }

    /**
     * 根据关键字的字 id 获取表情
     *
     * @param keywordIdsList
     *         关键字的{@link PinyinWord#glyphId 字形} id 列表
     */
    public static List<EmojiWord> getEmojisByKeyword(SQLiteDatabase db, List<Integer[]> keywordIdsList, int top) {
        // 直接查出全部表情，再对其做关键字过滤，以避免模糊查询存在性能问题
        List<KeywordEmoji> emojiWordList = querySQLite(db, new SQLiteQueryParams<KeywordEmoji>() {{
            this.table = "meta_emoji";
            this.columns = new String[] { "id_", "value_", "weight_user_ as weight_", "keyword_ids_list_" };
            this.where = "enabled_ = 1";
            this.orderBy = "weight_ desc, id_ asc";

            this.reader = (row) -> {
                String keywords = row.getString("keyword_ids_list_");
                EmojiWord emoji = createEmojiWord(row);

                return !isBlank(keywords) ? new KeywordEmoji(emoji, keywords) : null;
            };
        }});

        List<String> keywordList = keywordIdsList.stream()
                                                 .map(ids -> CharUtils.join(",", (Object[]) ids))
                                                 .collect(Collectors.toList());
        return emojiWordList.stream().filter(emoji -> {
            for (String keyword : keywordList) {
                if (emoji.matched(keyword)) {
                    return true;
                }
            }
            return false;
        }).map(KeywordEmoji::getEmoji).limit(top).collect(Collectors.toList());
    }

    /**
     * 更新表情的使用信息
     *
     * @param reverse
     *         是否反向更新，即，减掉对表情的使用权重
     */
    public static void saveUsedEmojis(SQLiteDatabase db, Collection<Integer> emojiIds, boolean reverse) {
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

    /** 启用所有系统支持的可显示的表情 */
    public static void enableAllPrintableEmojis(SQLiteDatabase db) {
        List<String> enabledIds = new ArrayList<>();
        List<String> disabledIds = new ArrayList<>();

        querySQLite(db, new SQLiteQueryParams<Void>() {{
            this.table = "meta_emoji";
            this.columns = new String[] { "id_", "value_", "enabled_" };

            this.voidReader = (row) -> {
                String id = row.getString("id_");
                String value = row.getString("value_");
                boolean enabled = row.getInt("enabled_") > 0;

                if (CharUtils.isPrintable(value)) {
                    if (!enabled) {
                        enabledIds.add(id);
                    }
                } else if (enabled) {
                    disabledIds.add(id);
                }
            };
        }});

        List<String>[] idsArray = new List[] { disabledIds, enabledIds };
        for (int i = 0; i < idsArray.length; i++) {
            List<String> ids = idsArray[i];
            if (ids.isEmpty()) {
                continue;
            }

            execSQLite(db, "update meta_emoji set enabled_ = " + i //
                           + " where id_ in (" + String.join(", ", ids) + ")");
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
            execSQLite(db, "delete from meta_latin where weight_user_ = 0");
        }
    }

    /** 获取指定汉字的字 id */
    public static Integer getWordId(SQLiteDatabase db, String word) {
        List<Integer> wordIdList = querySQLite(db, new SQLiteQueryParams<Integer>() {{
            this.table = "pinyin_word";
            this.columns = new String[] { "word_id_" };
            this.where = "word_ = ?";
            this.params = new String[] { word };

            this.reader = (row) -> row.getInt("word_id_");
        }});

        return CollectionUtils.first(wordIdList);
    }

    private static PinyinWord createPinyinWord(SQLiteRow row) {
        // 拼音字 id
        Integer id = row.getInt("id_");
        // 字
        String value = row.getString("word_");
        // 字形 id
        Integer glyphId = row.getInt("word_id_");

        // 拼音 id
        Integer spellId = row.getInt("spell_id_");
        // 拼音
        String spellValue = row.getString("spell_");
        // 拼音字母组合 id
        Integer spellCharsId = row.getInt("spell_chars_id_");
        PinyinWord.Spell spell = new PinyinWord.Spell(spellValue, spellId, spellCharsId);

        boolean traditional = row.getInt("traditional_") > 0;
        String variant = row.getString("variant_");

        String radicalValue = row.getString("radical_");
        int radicalStrokeCount = row.getInt("radical_stroke_count_");
        PinyinWord.Radical radical = new PinyinWord.Radical(radicalValue, radicalStrokeCount);

        return PinyinWord.build((b) -> //
                                        b.id(id)
                                         .value(value)
                                         .spell(spell)
                                         .glyphId(glyphId)
                                         .radical(radical)
                                         .traditional(traditional)
                                         .variant(variant) //
        );
    }

    private static EmojiWord createEmojiWord(SQLiteRow row) {
        Integer id = row.getInt("id_");
        String value = row.getString("value_");
        int weight = row.getInt("weight_");

        return EmojiWord.build((b) -> b.id(id).value(value).weight(weight));
    }

    /** 统计字符串列表中的字符串权重，并返回 SQLite 参数列表：<code>[[weight, source], [...], ...]</code> */
    private static List<String[]> statsWeightArgsList(Collection<?> list) {
        Map<Object, Integer> argsMap = new HashMap<>(list.size());
        list.forEach((source) -> {
            argsMap.compute(source, (k, v) -> (v == null ? 0 : v) + 1);
        });

        List<String[]> argsList = new ArrayList<>(argsMap.size());
        argsMap.forEach((source, weight) -> {
            argsList.add(new String[] { weight + "", Objects.toString(source) });
        });

        return argsList;
    }

    private static class KeywordEmoji {
        private final EmojiWord emoji;
        private final String keywords;

        private KeywordEmoji(EmojiWord emoji, String keywords) {
            this.emoji = emoji;
            this.keywords = keywords;
        }

        public EmojiWord getEmoji() {
            return this.emoji;
        }

        public boolean matched(String keyword) {
            return this.keywords.contains("[" + keyword + "]")
                   || this.keywords.contains("," + keyword + "]")
                   || this.keywords.contains("[" + keyword + ",")
                   || this.keywords.contains("," + keyword + ",");
        }
    }
}
