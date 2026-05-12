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

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-27
 */
public enum DictDBType {
    /** 用户库，即存放用户数据的库，应用运行期的数据均存放在该库中 */
    user("ime_user_dict.db"),

    /** 应用安装包内的字典库 */
    app_word("pinyin_word_dict.app.db"),
    /** 应用安装包内的词典库 */
    app_phrase("pinyin_phrase_dict.app.db");

    public final String filename;

    DictDBType(String filename) {
        this.filename = filename;
    }
}
