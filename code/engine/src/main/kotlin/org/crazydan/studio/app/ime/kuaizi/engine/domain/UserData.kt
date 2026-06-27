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
import java.time.Clock
import kotlinx.serialization.Serializable

enum class ImportStrategy {
    Replace,
    Merge,
}

sealed class ExportResult {
    data class Success(val itemCount: Int) : ExportResult()
    data class Failure(val message: String) : ExportResult()
}

sealed class ImportResult {
    data class Success(
        val importedCount: Int,
        val skippedCount: Int,
        val conflictCount: Int,
    ) : ImportResult()

    data class Failure(val message: String) : ImportResult()
}

@Serializable
data class UserBackup(
    val version: Int,
    val appVersion: String,
    val exportedAt: String,
    val data: BackupData,
)

@Serializable
data class BackupData(
    val userInput: List<UserInputBackupEntry>,
    val favorites: List<FavoriteBackupEntry>,
    val config: ConfigBackupEntry? = null,
)

@Serializable
data class UserInputBackupEntry(
    val text: String,
    val type: String,
    val freq: Int,
    val last_used: Long,
)

@Serializable
data class FavoriteBackupEntry(
    val text: String,
    val type: String? = null,
    val usage_count: Int,
    val created_at: Long,
)

@Serializable
data class ConfigBackupEntry(
    val engine: EngineConfigBackupEntry? = null,
    val ui: UiConfigBackupEntry? = null,
) {
    fun restoreFromBackup(current: ImeConfig): ImeConfig {
        return current.copy(
            engine = engine?.restoreFromBackup(current.engine) ?: current.engine,
            ui = ui?.restoreFromBackup(current.ui) ?: current.ui,
        )
    }
}

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
    fun restoreFromBackup(current: ImeConfig.EngineConfig): ImeConfig.EngineConfig {
        return current.copy(
            logLevel = log_level?.let { org.crazydan.studio.app.ime.kuaizi.engine.LogLevel.valueOf(it) } ?: current.logLevel,
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
    val practice_playback_speed: Float? = null,
    val practice_show_finger_overlay: Boolean? = null,
    val practice_show_swipe_trail: Boolean? = null,
) {
    fun restoreFromBackup(current: ImeConfig.UiConfig): ImeConfig.UiConfig {
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
            practicePlaybackSpeed = practice_playback_speed ?: current.practicePlaybackSpeed,
            practiceShowFingerOverlay = practice_show_finger_overlay ?: current.practiceShowFingerOverlay,
            practiceShowSwipeTrail = practice_show_swipe_trail ?: current.practiceShowSwipeTrail,
        )
    }
}
