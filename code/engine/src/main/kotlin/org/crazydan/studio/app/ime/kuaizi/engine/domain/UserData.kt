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

package org.crazydan.studio.app.ime.kuaizi.engine.domain

import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig
import kotlinx.serialization.Serializable
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogLevel

/** 导入策略 */
enum class ImportStrategy {
    /** 替换现有数据 */
    Replace,
    /** 与现有数据合并 */
    Merge,
}

/** 导出结果的密封类型 */
sealed class ExportResult {
    /** 导出成功 */
    data class Success(val itemCount: Int) : ExportResult()
    /** 导出失败 */
    data class Failure(val message: String) : ExportResult()
}

/** 导入结果的密封类型 */
sealed class ImportResult {
    /** 导入成功 */
    data class Success(
        val importedCount: Int,
        val skippedCount: Int,
        val conflictCount: Int,
    ) : ImportResult()

    /** 导入失败 */
    data class Failure(val message: String) : ImportResult()
}

/** 用户数据备份文件根结构 */
@Serializable
data class UserBackup(
    /** 数据格式版本号 */
    val version: Int,
    /** 导出时的应用版本 */
    val appVersion: String,
    /** 导出时间戳 */
    val exportedAt: String,
    /** 备份数据 */
    val data: BackupData,
)

/** 备份数据类型 */
@Serializable
data class BackupData(
    /** 用户输入历史数据 */
    val userInput: List<UserInputBackupEntry>,
    /** 收藏数据 */
    val favorites: List<FavoriteBackupEntry>,
    /** 配置数据（可选） */
    val config: ConfigBackupEntry? = null,
)

/** 用户输入备份条目 */
@Serializable
data class UserInputBackupEntry(
    val text: String,
    val type: String,
    val freq: Int,
    val last_used: Long,
)

/** 收藏备份条目 */
@Serializable
data class FavoriteBackupEntry(
    val text: String,
    val type: String? = null,
    val usage_count: Int,
    val created_at: Long,
)

/** 配置备份条目 */
@Serializable
data class ConfigBackupEntry(
    val engine: EngineConfigBackupEntry? = null,
    val ui: UiConfigBackupEntry? = null,
) {
    /**
     * 从备份恢复配置
     * @param current 当前配置
     * @return 恢复后的配置
     */
    fun restoreFromBackup(current: ImeConfig): ImeConfig {
        return current.copy(
            engine = engine?.restoreFromBackup(current.engine) ?: current.engine,
            ui = ui?.restoreFromBackup(current.ui) ?: current.ui,
        )
    }
}

/** 引擎配置备份条目 */
@Serializable
data class EngineConfigBackupEntry(
    val log_level: String? = null,
    val log_storage_path: String? = null,
    val input_prediction_enabled: Boolean? = null,
    val user_data_persist_enabled: Boolean? = null,
    val favorite_input_enabled: Boolean? = null,
    val favorite_clip_enabled: Boolean? = null,
    val favorite_sync_to_user_dict_enabled: Boolean? = null,
    val candidate_variant_first_enabled: Boolean? = null,
) {
    /**
     * 从备份恢复引擎配置
     * @param current 当前引擎配置
     * @return 恢复后的引擎配置
     */
    fun restoreFromBackup(current: ImeConfig.Engine): ImeConfig.Engine {
        return current.copy(
            logLevel = log_level?.let { LogLevel.valueOf(it) } ?: current.logLevel,
            logStoragePath = log_storage_path ?: current.logStoragePath,
            inputPredictionEnabled = input_prediction_enabled ?: current.inputPredictionEnabled,
            userDataPersistEnabled = user_data_persist_enabled ?: current.userDataPersistEnabled,
            favoriteInputEnabled = favorite_input_enabled ?: current.favoriteInputEnabled,
            favoriteClipEnabled = favorite_clip_enabled ?: current.favoriteClipEnabled,
            favoriteSyncToUserDictEnabled = favorite_sync_to_user_dict_enabled ?: current.favoriteSyncToUserDictEnabled,
            candidateVariantFirstEnabled = candidate_variant_first_enabled ?: current.candidateVariantFirstEnabled,
        )
    }
}

/** UI 配置备份条目 */
@Serializable
data class UiConfigBackupEntry(
    val keyboard_hand_mode: String? = null,
    val keyboard_theme_type: String? = null,
    val key_popup_tips_enabled: Boolean? = null,
    val audio_feedback_enabled: Boolean? = null,
    val haptic_feedback_enabled: Boolean? = null,
    val key_animation_enabled: Boolean? = null,
    val gesture_slipping_trail_enabled: Boolean? = null,
    val clip_popup_tips_enabled: Boolean? = null,
    val clip_popup_tips_timeout: Int? = null,
    val adapt_desktop_swipe_up_gesture: Boolean? = null,
    val candidates_paging_audio_enabled: Boolean? = null,
) {
    /**
     * 从备份恢复 UI 配置
     * @param current 当前 UI 配置
     * @return 恢复后的 UI 配置
     */
    fun restoreFromBackup(current: ImeConfig.Ui): ImeConfig.Ui {
        return current.copy(
            keyboardHandMode = keyboard_hand_mode?.let { org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardHandMode.valueOf(it) } ?: current.keyboardHandMode,
            keyboardThemeType = keyboard_theme_type?.let { org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardThemeType.valueOf(it) } ?: current.keyboardThemeType,
            keyPopupTipsEnabled = key_popup_tips_enabled ?: current.keyPopupTipsEnabled,
            audioFeedbackEnabled = audio_feedback_enabled ?: current.audioFeedbackEnabled,
            hapticFeedbackEnabled = haptic_feedback_enabled ?: current.hapticFeedbackEnabled,
            keyAnimationEnabled = key_animation_enabled ?: current.keyAnimationEnabled,
            gestureSlippingTrailEnabled = gesture_slipping_trail_enabled ?: current.gestureSlippingTrailEnabled,
            clipPopupTipsEnabled = clip_popup_tips_enabled ?: current.clipPopupTipsEnabled,
            clipPopupTipsTimeout = clip_popup_tips_timeout ?: current.clipPopupTipsTimeout,
            adaptDesktopSwipeUpGesture = adapt_desktop_swipe_up_gesture ?: current.adaptDesktopSwipeUpGesture,
            candidatesPagingAudioEnabled = candidates_paging_audio_enabled ?: current.candidatesPagingAudioEnabled,
        )
    }
}
