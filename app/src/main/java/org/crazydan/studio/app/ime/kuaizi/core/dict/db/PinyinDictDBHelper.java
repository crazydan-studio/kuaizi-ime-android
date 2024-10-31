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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;

import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteRawQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteRow;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.rawQuerySQLite;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-29
 */
public class PinyinDictDBHelper {

    /** 根据字及其拼音获取其 {@link PinyinInputWord} 对象 */
    public static PinyinInputWord getPinyinInputWord(SQLiteDatabase db, String word, String pinyin) {
        List<PinyinInputWord> wordList = queryPinyinInputWords(db,
                                                               "py_.word_ = ? and py_.spell_ = ?",
                                                               new String[] { word, pinyin },
                                                               false);
        return CollectionUtils.first(wordList);
    }

    /** 根据拼音字 id 获取其 {@link PinyinInputWord} 对象 */
    public static Map<String, PinyinInputWord> getPinyinInputWords(SQLiteDatabase db, Set<String> pinyinWordIds) {
        List<PinyinInputWord> wordList = queryPinyinInputWords(db,
                                                               "py_.id_ in (" + pinyinWordIds.stream()
                                                                                             .map((id) -> "?")
                                                                                             .collect(Collectors.joining(
                                                                                                     ", ")) + ")",
                                                               pinyinWordIds.toArray(new String[0]),
                                                               false);

        return wordList.stream().collect(Collectors.toMap(PinyinInputWord::getUid, Function.identity()));
    }

    /**
     * 查询拼音字表 pinyin_word 以获得 {@link PinyinInputWord} 对象列表
     *
     * @param sort
     *         若为 <code>true</code>，则结果依次按使用频率、字形相似性、拼音字母顺序排序
     */
    public static List<PinyinInputWord> queryPinyinInputWords(
            SQLiteDatabase db, String queryWhere, String[] queryParams, boolean sort
    ) {
        return rawQuerySQLite(db, new SQLiteRawQueryParams<PinyinInputWord>() {{
            this.sql = "select distinct"
                       + "   py_.id_, py_.word_, py_.word_id_,"
                       + "   py_.spell_, py_.spell_id_, py_.spell_chars_id_,"
                       + "   py_.traditional_, py_.stroke_order_,"
                       + "   py_.radical_, py_.radical_stroke_count_"
                       + " from pinyin_word py_"
                       + (sort ? " left join phrase_word ph_ on ph_.word_id_ = py_.id_" : "")
                       + (" where " + queryWhere);
            if (sort) {
                this.sql += " order by"
                            // 短语中的常用字最靠前
                            + "   ( ifnull(ph_.weight_app_, 0) +" //
                            + "     ifnull(ph_.weight_user_, 0) +"
                            // Note: 低版本 SQLite 不支持 iif，需采用 case when
                            + "     (case when ifnull(ph_.weight_user_, 0) > 0 then ? else 0 end)"
                            // + "     iif(ifnull(ph_.weight_user_, 0) > 0, ?, 0)"
                            + "   ) desc,"
                            // 再按拼音字的使用频率（weight_）、拼音字的字形相似性（glyph_weight_）、拼音字母顺序（spell_id_）排序
                            + "   py_.weight_ desc, py_.glyph_weight_ desc, py_.spell_id_ asc";
            }

            this.params = queryParams;

            this.reader = PinyinDictDBHelper::createPinyinInputWord;
        }});
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

        boolean traditional = row.getInt("traditional_") > 0;
        String strokeOrder = row.getString("stroke_order_");

        String radicalValue = row.getString("radical_");
        int radicalStrokeCount = row.getInt("radical_stroke_count_");
        PinyinInputWord.Radical radical = new PinyinInputWord.Radical(radicalValue, radicalStrokeCount);

        return new PinyinInputWord(uid, wordValue, wordId, spell, radical, traditional, strokeOrder);
    }
}
