/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
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

package org.crazydan.studio.app.ime.kuaizi.dict.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputFavorite
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputTextType

/** 拼音字数据库实体：存储单个汉字的拼音、频率、变体和声调信息。 */
@Entity(tableName = "pinyin_word")
data class PinyinWordEntity(
    @PrimaryKey val id: Long,
    val spell: String,           // 拼音拼写，如 "zhong"
    val text: String,            // 汉字文本，如 "中"
    val freq: Int,               // 字典频率
    val variant: String? = null, // 繁体/异体变体文本
    val tone: Int? = null,       // 声调值：1-4 对应一声到四声，0 或 null 为轻声
)

/** 拼音词组数据库实体：存储多字词组的拼音序列和频率。 */
@Entity(tableName = "pinyin_phrase")
data class PinyinPhraseEntity(
    @PrimaryKey val id: Long,
    val spells: String,  // 拼音序列，逗号分隔，如 "zhong,guo"
    val text: String,    // 词组文本，如 "中国"
    val freq: Int,       // 字典频率
)

/** 用户输入记录数据库实体：记录用户输入历史和使用频率。 */
@Entity(tableName = "user_input_data")
data class UserInputEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,                               // 输入的文本内容
    val type: String,                               // 输入类型：pinyin / latin / phrase
    val freq: Int = 0,                              // 用户使用频率
    val lastUsed: Long = System.currentTimeMillis(), // 最后使用时间戳
)

/**
 * 用户收藏数据库实体：存储用户收藏的文本条目。
 * @param id 主键
 * @param text 收藏文本
 * @param type 文本类型
 * @param usageCount 使用次数
 * @param createdAt 创建时间戳
 */
@Entity(tableName = "user_input_favorite")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,                               // 收藏的文本
    val type: String? = null,                        // 文本类型，复用 InputTextType 的值
    val usageCount: Int = 0,                         // 使用次数
    val createdAt: Long = System.currentTimeMillis(), // 创建时间
) {
    /** 转换为领域模型 [InputFavorite] */
    fun toDomain(): InputFavorite = InputFavorite(
        text = text,
        type = type?.let { InputTextType.valueOf(it) },
        usageCount = usageCount,
        createdAt = createdAt,
    )
}

/** HMM 状态转移概率数据库实体：存储拼音间的转移概率。 */
@Entity(tableName = "hmm_transition")
data class HmmTransitionEntity(
    @PrimaryKey val id: Long,
    val fromState: String, // 起始拼音
    val toState: String,   // 目标拼音
    val probability: Double, // 转移概率
)
