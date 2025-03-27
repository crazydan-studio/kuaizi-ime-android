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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;

import static org.crazydan.studio.app.ime.kuaizi.dict.db.HmmDBHelper.saveUsedPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputDataDBHelper.getAllGroupedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputDataDBHelper.getLatinsByStarts;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputDataDBHelper.saveUsedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputDataDBHelper.saveUsedLatins;

/**
 * {@link UserInputData 用户输入数据}字典
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-27
 */
public class UserInputDataDict extends BaseDBDict {

    public UserInputDataDict(SQLiteDatabase db, ThreadPoolExecutor executor) {
        super(db, executor);
    }

    /** 保存使用数据信息，含短语、单字、表情符号等：异步处理 */
    public CompletableFuture<Void> save(UserInputData data) {
        return doSaveUserInputData(data, false);
    }

    /** 对 {@link #save} 的撤销处理（异步） */
    public CompletableFuture<Void> revokeSave(UserInputData data) {
        return doSaveUserInputData(data, true);
    }

    /**
     * 获取各分组中的所有表情
     *
     * @param groupGeneralCount
     *         {@link Emojis#GROUP_GENERAL} 分组中的表情数量
     */
    public Emojis getAllEmojis(int groupGeneralCount) {
        return getAllGroupedEmojis(this.db, groupGeneralCount);
    }

    /** 查找以指定参数开头的最靠前的 <code>top</code> 个拉丁文 */
    public List<String> findTopBestMatchedLatins(String text, int top) {
        if (text == null || text.length() < 2) {
            return List.of();
        }

        return getLatinsByStarts(this.db, text, top);
    }

    /** 保存使用数据信息，含短语、单字、表情符号等：异步处理 */
    private CompletableFuture<Void> doSaveUserInputData(UserInputData data, boolean reverse) {
        if (data.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return promise(() -> {
            data.phrases.forEach((phrase) -> doSaveUsedPhrase(phrase, reverse));

            doSaveUsedEmojis(data.emojis, reverse);
            doSaveUsedLatins(data.latins, reverse);
        });
    }

    private void doSaveUsedPhrase(List<PinyinWord> phrase, boolean reverse) {
        saveUsedPinyinPhrase(this.db, phrase, reverse);
    }

    /** 保存表情的使用频率等信息 */
    private void doSaveUsedEmojis(List<InputWord> emojis, boolean reverse) {
        saveUsedEmojis(this.db, emojis.stream().map((w) -> w.id).collect(Collectors.toList()), reverse);
    }

    /** 保存拉丁文的使用频率等信息 */
    private void doSaveUsedLatins(List<String> latins, boolean reverse) {
        // 仅针对长单词
        saveUsedLatins(this.db,
                       latins.stream().filter((latin) -> latin.length() > 3).collect(Collectors.toList()),
                       reverse);
    }
}
