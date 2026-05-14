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

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.common.Async;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputFavorite;

import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputFavoriteDBHelper.clearAllInputFavorites;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputFavoriteDBHelper.existSameTextInputFavorite;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputFavoriteDBHelper.getAllInputFavorites;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputFavoriteDBHelper.removeInputFavorites;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputFavoriteDBHelper.saveInputFavorite;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputFavoriteDBHelper.updateInputFavoriteUsage;

/**
 * {@link InputFavorite 用户收藏}字典
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-26
 */
public class UserInputFavoriteDict extends BaseDBDict {

    public UserInputFavoriteDict(SQLiteDatabase db, Async async) {
        super(db, async);
    }

    /** 新增 {@link InputFavorite} */
    public CompletableFuture<InputFavorite> save(InputFavorite favorite) {
        return this.async.future(() -> saveInputFavorite(this.db, favorite));
    }

    /** 更新 {@link InputFavorite} 的使用情况 */
    public CompletableFuture<InputFavorite> updateUsage(InputFavorite favorite) {
        return this.async.future(() -> updateInputFavoriteUsage(this.db, favorite));
    }

    public CompletableFuture<List<InputFavorite>> getAll() {
        return this.async.future(() -> getAllInputFavorites(this.db));
    }

    public CompletableFuture<Void> remove(List<Integer> ids) {
        return this.async.future(() -> removeInputFavorites(this.db, ids));
    }

    public CompletableFuture<Void> clearAll() {
        return this.async.future(() -> clearAllInputFavorites(this.db));
    }

    /** 是否存在相同文本的 {@link InputFavorite} */
    public CompletableFuture<Boolean> exist(String text) {
        return this.async.future(() -> existSameTextInputFavorite(this.db, text));
    }
}
