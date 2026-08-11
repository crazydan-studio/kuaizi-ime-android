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

package org.crazydan.studio.app.ime.kuaizi.engine.input

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent

/**
 * 候选列表，管理拼音输入的候选词数据。
 * 采用不可变 data class 设计，包含候选词列表、分页控制、过滤状态和加载标记。
 * 采用轮播分页模式：翻到末页后继续翻页回到首页，翻到首页前继续翻页跳到末页。
 *
 * @param candidates 候选词列表
 * @param pageIndex 当前页码
 * @param pageSize 每页大小
 * @param hasMore 字典中是否还有更多候选词可供加载
 * @param totalApprox 查询总结果数的近似值
 * @param filter 应用的过滤条件
 */
data class CandidateList(
    val candidates: List<InputWord> = emptyList(),
    val pageIndex: Int = 0,
    val pageSize: Int = 20,
    val hasMore: Boolean = false,
    val totalApprox: Int = 0,
    val filter: PinyinWordFilter? = null,
) {
    /** 候选列表是否为空 */
    val isEmpty: Boolean get() = candidates.isEmpty()

    /** 总页数 */
    val totalPages: Int
        get() = if (candidates.isEmpty()) 0 else (candidates.size + pageSize - 1) / pageSize

    /** 当前页的候选词子列表 */
    val currentPage: List<InputWord>
        get() {
            val fromIndex = pageIndex * pageSize
            val toIndex = minOf(fromIndex + pageSize, candidates.size)
            return if (fromIndex >= candidates.size) emptyList()
            else candidates.subList(fromIndex, toIndex)
        }

    /** 翻到下一页（轮播模式） */
    fun nextPage(): CandidateList {
        if (candidates.isEmpty()) return this
        val next = (pageIndex + 1) % totalPages
        return copy(pageIndex = next)
    }

    /** 翻到上一页（轮播模式） */
    fun prevPage(): CandidateList {
        if (candidates.isEmpty()) return this
        val prev = if (pageIndex == 0) totalPages - 1 else pageIndex - 1
        return copy(pageIndex = prev)
    }

    /**
     * 使用新的候选词列表替换当前列表，重置页码为 0
     * @param candidates 新的候选词列表
     */
    fun withCandidates(candidates: List<InputWord>): CandidateList =
        copy(candidates = candidates, pageIndex = 0, hasMore = candidates.size >= pageSize)

    /**
     * 应用拼音过滤器过滤候选词
     * @param filter 过滤条件
     * @return 过滤后的候选列表
     */
    fun withFilter(filter: PinyinWordFilter): CandidateList {
        if (filter.isEmpty) return copy(filter = filter, pageIndex = 0)
        val filtered = candidates.filter { filter.matched(it) }
        return copy(filter = filter, pageIndex = 0, candidates = filtered)
    }
}

/**
 * 候选词的密封类型层级，包含四种候选词类型。
 * 所有子类共享 text 和 frequency 字段，各自携带类型特有的语义信息。
 */
sealed class InputWord {
    /** 候选词文本 */
    abstract val value: String

    /** 候选词使用频率 */
    abstract val frequency: Int

    /**
     * 汉字
     * @param value 汉字文本
     * @param frequency 使用频率
     * @param spell 拼音拼写信息
     * @param variant 变体文本。若当前为简体，则该变体为繁体，若当前为繁体，则该变体为简体
     * @param radical 部首信息
     * @param tone 声调信息
     */
    data class Hanzi(
        override val value: String,
        override val frequency: Int = 0,
        val type: Type = Type.Simplified,
        val spell: Spell,
        val variant: String? = null,
        val radical: Radical? = null,
        val tone: Tone? = null,
    ) : InputWord() {

        /** 汉字类型：简体 or 繁体 */
        enum class Type {
            /** 简体字 */
            Simplified,

            /** 繁体字 */
            Traditional,
        }
    }

    /**
     * 拼音词组候选
     * @param value 词组文本
     * @param frequency 使用频率
     * @param spells 各字的拼音拼写列表
     */
    data class PinyinPhrase(
        override val value: String,
        override val frequency: Int,
        val spells: List<String>,
    ) : InputWord()

    /**
     * Emoji 候选
     * @param value Emoji 文本
     * @param frequency 使用频率
     * @param name Emoji 名称
     * @param group Emoji 分组
     */
    data class Emoji(
        override val value: String,
        override val frequency: Int = 0,
        val name: String,
        val group: String,
    ) : InputWord()

    /**
     * 拉丁词候选
     * @param value 拉丁词文本
     * @param frequency 使用频率
     */
    data class Latin(
        override val value: String,
        override val frequency: Int,
    ) : InputWord()

    /**
     * 提交选项，针对已确认输入的后续操作
     * @param value 选项文本
     * @param frequency 使用频率
     * @param action 点击后触发的 ImeIntent
     * @param spell 关联的拼写信息
     * @param variant 关联的变体信息
     */
    data class CommitOption(
        override val value: String,
        override val frequency: Int = 0,
        val action: ImeIntent? = null,
        val spell: Spell? = null,
        val variant: Variant? = null,
    ) : InputWord()

    /** 读音使用模式：跟随或替换 [InputWord] */
    enum class SpellUseMode {
        /** 跟随 */
        Follow,

        /** 替换 */
        Replace,
    }
}

/**
 * 汉字/英文读音
 * @param id 读音唯一标识，用于去重和索引
 * @param value 读音的显示值
 */
data class Spell(val id: String, val value: String)

/**
 * 变体信息（繁体/异体）
 * @param text 变体文本
 * @param type 变体类型
 */
data class Variant(val text: String, val type: VariantType)

/**
 * 部首信息
 * @param text 部首文本
 * @param strokeCount 笔画数
 */
data class Radical(val text: String, val strokeCount: Int)

/** 声调枚举 */
enum class Tone {
    /** 第一声（阴平） */
    Tone1,

    /** 第二声（阳平） */
    Tone2,

    /** 第三声（上声） */
    Tone3,

    /** 第四声（去声） */
    Tone4,

    /** 轻声 */
    Neutral,
}

/** 变体类型 */
enum class VariantType {
    /** 繁体字 */
    Traditional,

    /** 异体字 */
    Variant,
}

/**
 * 拼音过滤器，支持按声调和拼写两个维度筛选拼音候选词。
 * 两个维度的过滤条件是 AND 关系。
 *
 * @param tones 选中的声调集合
 * @param spells 选中的拼写集合
 */
data class PinyinWordFilter(
    val tones: Set<Tone> = emptySet(),
    val spells: Set<Spell> = emptySet(),
) {
    /** 过滤条件是否为空 */
    val isEmpty: Boolean get() = tones.isEmpty() && spells.isEmpty()

    /**
     * 判断一个候选词是否满足过滤条件
     * @param word 候选词
     * @return 是否匹配
     */
    fun matched(word: InputWord): Boolean {
        if (word !is InputWord.Hanzi) return false
        if (tones.isNotEmpty() && word.tone !in tones) return false
        if (spells.isNotEmpty() && word.spell !in spells) return false
        return true
    }

    /** 添加声调过滤条件 */
    fun withTone(tone: Tone): PinyinWordFilter =
        copy(tones = tones + tone)

    /** 移除声调过滤条件 */
    fun withoutTone(tone: Tone): PinyinWordFilter =
        copy(tones = tones - tone)

    /** 添加拼写过滤条件 */
    fun withSpell(spell: Spell): PinyinWordFilter =
        copy(spells = spells + spell)

    /** 移除拼写过滤条件 */
    fun withoutSpell(spell: Spell): PinyinWordFilter =
        copy(spells = spells - spell)

    /** 清除所有过滤条件 */
    fun clear(): PinyinWordFilter = PinyinWordFilter()
}
