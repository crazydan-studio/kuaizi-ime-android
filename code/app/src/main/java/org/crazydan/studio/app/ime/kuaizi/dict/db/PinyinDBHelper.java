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

package org.crazydan.studio.app.ime.kuaizi.dict.db;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;

import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.SQLiteQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.SQLiteRawQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.SQLiteRow;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.createSQLiteArgHolders;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.querySQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.rawQuerySQLite;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-29
 */
public class PinyinDBHelper {

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
        String argHolders = createSQLiteArgHolders(pinyinWordIds);

        List<PinyinWord> wordList = queryPinyinWords(db,
                                                     "py_.id_ in (" + argHolders + ")",
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
            this.clause = "select distinct"
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
                this.clause = "select distinct"
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

                this.reader = PinyinDBHelper::createPinyinWord;
            }
        });
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
}
