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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.EmojiWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.dict.Emojis;

import static org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils.isBlank;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils.subList;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.SQLiteQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.SQLiteRawQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.SQLiteRawUpsertParams;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.querySQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.rawQuerySQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.upsertSQLite;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-27
 */
public class UserInputDataDBHelper {

    /** 根据表情符号获取其 {@link EmojiWord 表情对象} */
    public static EmojiWord getEmoji(SQLiteDatabase db, String emoji) {
        List<EmojiWord> emojiList = querySQLite(db, new SQLiteQueryParams<EmojiWord>() {{
            this.table = "meta_emoji";
            this.columns = new String[] { "id_", "value_", "weight_user_ as weight_" };
            this.where = "value_ = ?";
            this.params = new String[] { emoji };

            this.reader = UserInputDataDBHelper::createEmojiWord;
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
            this.clause = "select id_, value_, weight_, group_" //
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
            this.columns = new String[] {
                    "id_", "value_", "weight_user_ as weight_", "keyword_ids_list_"
            };
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
        List<Object[]> argsList = statsWeightArgsList(emojiIds);

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
        List<Object[]> argsList = statsWeightArgsList(latins);

        if (!reverse) {
            upsertSQLite(db, new SQLiteRawUpsertParams() {{
                // Note: 确保更新和新增的参数位置相同
                this.updateClause = "update meta_latin set weight_user_ = weight_user_ + ? where value_ = ?";
                this.insertClause = "insert into meta_latin(weight_user_, value_) values(?, ?)";

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

    private static EmojiWord createEmojiWord(DBUtils.SQLiteRow row) {
        Integer id = row.getInt("id_");
        String value = row.getString("value_");
        int weight = row.getInt("weight_");

        return EmojiWord.build((b) -> b.id(id).value(value).weight(weight));
    }

    /** 统计字符串列表中的字符串权重，并返回 SQLite 参数列表：<code>[[weight, source], [...], ...]</code> */
    private static List<Object[]> statsWeightArgsList(Collection<?> list) {
        Map<Object, Integer> argsMap = new HashMap<>(list.size());
        list.forEach((source) -> {
            argsMap.compute(source, (k, v) -> (v == null ? 0 : v) + 1);
        });

        List<Object[]> argsList = new ArrayList<>(argsMap.size());
        argsMap.forEach((source, weight) -> {
            argsList.add(new Object[] { weight, source });
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
