/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core

// source from https://github.com/osfans/trime/blob/develop/app/src/main/java/com/osfans/trime/core/RimeProto.kt

data class CommitProto(
    val text: String?,
)

data class CandidateProto(
    /** 候选字/短语文本 */
    val text: String,
    /** 候选字拼音（带声调） */
    val comment: String?,
    /** 候选字标签（即其在候选字列表中的序号，从 1 开始） */
    val label: String,
)

data class CompositionProto(
    /**
     * Actually we can directly use [String.length] on [preedit], but
     * we add it here for the sake of completeness as it is semantically correct
     */
    val length: Int = 0,
    val cursorPos: Int = 0,
    val selStart: Int = 0,
    val selEnd: Int = 0,
    val preedit: String? = null,
    val commitTextPreview: String? = null,
) {
    internal constructor(text: String) : this(
        text.length,
        text.length,
        text.length,
        text.length,
        text,
    )
}

data class MenuProto(
    /** 候选字分页大小 */
    val pageSize: Int = 0,
    /** 候选字分页起始序号 */
    val pageNumber: Int = 0,
    /** 是否为最后一页 */
    val isLastPage: Boolean = false,
    /** 高亮候选字的序号 */
    val highlightedCandidateIndex: Int = 0,
    /** 候选字列表 */
    val candidates: Array<CandidateProto> = arrayOf(),
    val selectKeys: String? = null,
    /** 候选字序号，其为候选字列表 [candidates] 中候选字的 [CandidateProto.label] */
    val selectLabels: Array<String> = arrayOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MenuProto

        if (pageSize != other.pageSize) return false
        if (pageNumber != other.pageNumber) return false
        if (isLastPage != other.isLastPage) return false
        if (highlightedCandidateIndex != other.highlightedCandidateIndex) return false
        if (!candidates.contentEquals(other.candidates)) return false
        if (selectKeys != other.selectKeys) return false
        if (!selectLabels.contentEquals(other.selectLabels)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pageSize
        result = 31 * result + pageNumber
        result = 31 * result + isLastPage.hashCode()
        result = 31 * result + highlightedCandidateIndex
        result = 31 * result + candidates.contentHashCode()
        result = 31 * result + (selectKeys?.hashCode() ?: 0)
        result = 31 * result + selectLabels.contentHashCode()
        return result
    }
}

data class ContextProto(
    val composition: CompositionProto = CompositionProto(),
    val menu: MenuProto = MenuProto(),
    val input: String = "",
    val caretPos: Int = 0,
)

/** see `RimeGetStatus` in `librime/src/rime_api_impl.h` */
data class StatusProto(
    val schemaId: String = "",
    val schemaName: String = "",
    val isDisabled: Boolean = true,
    val isComposing: Boolean = false,
    /** 是否为英文输入模式：`true` - 英文；`false` - 中文 */
    val isAsciiMode: Boolean = true,
    /** 是否为全角字符输入模式：`true` - 全角；`false` - 半角 */
    val isFullShape: Boolean = false,
    /** 是否为简体字输出模式 */
    val isSimplified: Boolean = false,
    /** 是否为繁体字输出输出模式 */
    val isTraditional: Boolean = false,
    /** 是否为英文标点输入模式 */
    val isAsciiPunct: Boolean = true,
)
