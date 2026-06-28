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
 * If not, see <see href="https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text"/>.
 */

package org.crazydan.studio.app.ime.kuaizi.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.crazydan.studio.app.ime.kuaizi.engine.*
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogLevel

/** Context 扩展属性，提供 IME 配置的 DataStore 实例。 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("ime_config")

/**
 * 基于 DataStore 的配置持久化实现。
 *
 * 将 [ImeConfig] 中的 [EngineConfig] 和 [UiConfig] 持久化到 Preferences DataStore。
 * 支持增量更新：仅将变化的部分写入 DataStore，避免全量写入。
 *
 * [RuntimeConfig] 不做持久化，应用重启时根据 [StartupConfig] 重新初始化。
 */
class ConfigDataStore(private val context: Context) {
    /**
     * 配置的响应式 Flow。
     *
     * 从 DataStore 读取原始 Preferences，映射为 [ImeConfig] 实例。
     * UI 层通过订阅此 Flow 实时响应配置变更。
     */
    val config: Flow<ImeConfig> = context.dataStore.data.map { prefs ->
        ImeConfig(
            engine = readEngineConfig(prefs),
            ui = readUiConfig(prefs),
        )
    }

    /**
     * 增量更新配置。
     *
     * 通过比较新旧配置的差异，仅将发生变化的字段写入 DataStore，
     * 避免每次 updateConfig 都全量写入所有 key。
     * 适用于高频配置切换场景（主题、音效开关的快速连续切换）。
     */
    suspend fun updateConfig(transform: (ImeConfig) -> ImeConfig) {
        context.dataStore.edit { prefs ->
            val current = config.first()
            val new = transform(current)
            // 分别写入引擎和 UI 配置中变化的部分
            writeChangedEngineConfig(prefs, current.engine, new.engine)
            writeChangedUiConfig(prefs, current.ui, new.ui)
        }
    }

    /** 从 Preferences 读取引擎配置。缺失的 key 使用对应默认值。 */
    private fun readEngineConfig(prefs: Preferences): ImeConfig.Engine {
        return ImeConfig.Engine(
            inputPredictionEnabled = prefs[booleanPreferencesKey("engine_input_prediction_enabled")] ?: true,
            userDataPersistEnabled = prefs[booleanPreferencesKey("engine_user_data_persist_enabled")] ?: true,
            favoriteInputEnabled = prefs[booleanPreferencesKey("engine_favorite_input_enabled")] ?: true,
            favoriteClipEnabled = prefs[booleanPreferencesKey("engine_favorite_clip_enabled")] ?: true,
            favoriteSyncToUserDictEnabled = prefs[booleanPreferencesKey("engine_favorite_sync_to_user_dict_enabled")] ?: false,
            candidateVariantFirstEnabled = prefs[booleanPreferencesKey("engine_candidate_variant_first_enabled")] ?: false,
            logLevel = LogLevel.fromPriority(prefs[intPreferencesKey("engine_log_level")] ?: LogLevel.WARN.priority),
            logStoragePath = prefs[stringPreferencesKey("engine_log_storage_path")],
        )
    }

    /** 从 Preferences 读取 UI 配置。枚举类型通过 name 序列化/反序列化。 */
    private fun readUiConfig(prefs: Preferences): ImeConfig.Ui {
        return ImeConfig.Ui(
            keyboardInputMode = try {
                KeyboardInputMode.valueOf(prefs[stringPreferencesKey("ui_keyboard_input_mode")] ?: "RectGrid")
            } catch (_: Exception) { KeyboardInputMode.RectGrid },
            keyboardHandMode = try {
                KeyboardHandMode.valueOf(prefs[stringPreferencesKey("ui_keyboard_hand_mode")] ?: "Right")
            } catch (_: Exception) { KeyboardHandMode.Right },
            keyboardThemeType = try {
                KeyboardThemeType.valueOf(prefs[stringPreferencesKey("ui_keyboard_theme_type")] ?: "FollowSystem")
            } catch (_: Exception) { KeyboardThemeType.FollowSystem },
            audioFeedbackEnabled = prefs[booleanPreferencesKey("ui_audio_feedback_enabled")] ?: true,
            hapticFeedbackEnabled = prefs[booleanPreferencesKey("ui_haptic_feedback_enabled")] ?: true,
            keyAnimationEnabled = prefs[booleanPreferencesKey("ui_key_animation_enabled")] ?: true,
            keyPopupTipsEnabled = prefs[booleanPreferencesKey("ui_key_popup_tips_enabled")] ?: true,
            gestureSlippingTrailEnabled = prefs[booleanPreferencesKey("ui_gesture_slipping_trail_enabled")] ?: true,
            clipPopupTipsEnabled = prefs[booleanPreferencesKey("ui_clip_popup_tips_enabled")] ?: true,
            clipPastePopupTipsEnabled = prefs[booleanPreferencesKey("ui_clip_paste_popup_tips_enabled")] ?: true,
            clipPopupTipsTimeout = prefs[intPreferencesKey("ui_clip_popup_tips_timeout")] ?: 15,
            adaptDesktopSwipeUpGesture = prefs[booleanPreferencesKey("ui_adapt_desktop_swipe_up_gesture")] ?: false,
            candidatesPagingAudioEnabled = prefs[booleanPreferencesKey("ui_candidates_paging_audio_enabled")] ?: true,
            practicePlaybackSpeed = prefs[floatPreferencesKey("ui_practice_playback_speed")] ?: 1.0f,
            practiceShowFingerOverlay = prefs[booleanPreferencesKey("ui_practice_show_finger_overlay")] ?: true,
            practiceShowSwipeTrail = prefs[booleanPreferencesKey("ui_practice_show_swipe_trail")] ?: true,
        )
    }

    /** 增量写入引擎配置：仅将变化的字段写入 Preferences。 */
    private fun writeChangedEngineConfig(
        prefs: MutablePreferences,
        old: ImeConfig.Engine,
        new: ImeConfig.Engine,
    ) {
        if (old.inputPredictionEnabled != new.inputPredictionEnabled)
            prefs[booleanPreferencesKey("engine_input_prediction_enabled")] = new.inputPredictionEnabled
        if (old.userDataPersistEnabled != new.userDataPersistEnabled)
            prefs[booleanPreferencesKey("engine_user_data_persist_enabled")] = new.userDataPersistEnabled
        if (old.favoriteInputEnabled != new.favoriteInputEnabled)
            prefs[booleanPreferencesKey("engine_favorite_input_enabled")] = new.favoriteInputEnabled
        if (old.favoriteClipEnabled != new.favoriteClipEnabled)
            prefs[booleanPreferencesKey("engine_favorite_clip_enabled")] = new.favoriteClipEnabled
        if (old.favoriteSyncToUserDictEnabled != new.favoriteSyncToUserDictEnabled)
            prefs[booleanPreferencesKey("engine_favorite_sync_to_user_dict_enabled")] = new.favoriteSyncToUserDictEnabled
        if (old.candidateVariantFirstEnabled != new.candidateVariantFirstEnabled)
            prefs[booleanPreferencesKey("engine_candidate_variant_first_enabled")] = new.candidateVariantFirstEnabled
        if (old.logLevel != new.logLevel)
            prefs[intPreferencesKey("engine_log_level")] = new.logLevel.priority
        if (old.logStoragePath != new.logStoragePath)
            prefs[stringPreferencesKey("engine_log_storage_path")] = new.logStoragePath
    }

    /** 增量写入 UI 配置：仅将变化的字段写入 Preferences。 */
    private fun writeChangedUiConfig(
        prefs: MutablePreferences,
        old: ImeConfig.Ui,
        new: ImeConfig.Ui,
    ) {
        if (old.keyboardInputMode != new.keyboardInputMode)
            prefs[stringPreferencesKey("ui_keyboard_input_mode")] = new.keyboardInputMode.name
        if (old.keyboardHandMode != new.keyboardHandMode)
            prefs[stringPreferencesKey("ui_keyboard_hand_mode")] = new.keyboardHandMode.name
        if (old.keyboardThemeType != new.keyboardThemeType)
            prefs[stringPreferencesKey("ui_keyboard_theme_type")] = new.keyboardThemeType.name
        if (old.audioFeedbackEnabled != new.audioFeedbackEnabled)
            prefs[booleanPreferencesKey("ui_audio_feedback_enabled")] = new.audioFeedbackEnabled
        if (old.hapticFeedbackEnabled != new.hapticFeedbackEnabled)
            prefs[booleanPreferencesKey("ui_haptic_feedback_enabled")] = new.hapticFeedbackEnabled
        if (old.keyAnimationEnabled != new.keyAnimationEnabled)
            prefs[booleanPreferencesKey("ui_key_animation_enabled")] = new.keyAnimationEnabled
        if (old.keyPopupTipsEnabled != new.keyPopupTipsEnabled)
            prefs[booleanPreferencesKey("ui_key_popup_tips_enabled")] = new.keyPopupTipsEnabled
        if (old.gestureSlippingTrailEnabled != new.gestureSlippingTrailEnabled)
            prefs[booleanPreferencesKey("ui_gesture_slipping_trail_enabled")] = new.gestureSlippingTrailEnabled
        if (old.clipPopupTipsEnabled != new.clipPopupTipsEnabled)
            prefs[booleanPreferencesKey("ui_clip_popup_tips_enabled")] = new.clipPopupTipsEnabled
        if (old.clipPastePopupTipsEnabled != new.clipPastePopupTipsEnabled)
            prefs[booleanPreferencesKey("ui_clip_paste_popup_tips_enabled")] = new.clipPastePopupTipsEnabled
        if (old.clipPopupTipsTimeout != new.clipPopupTipsTimeout)
            prefs[intPreferencesKey("ui_clip_popup_tips_timeout")] = new.clipPopupTipsTimeout
        if (old.adaptDesktopSwipeUpGesture != new.adaptDesktopSwipeUpGesture)
            prefs[booleanPreferencesKey("ui_adapt_desktop_swipe_up_gesture")] = new.adaptDesktopSwipeUpGesture
        if (old.candidatesPagingAudioEnabled != new.candidatesPagingAudioEnabled)
            prefs[booleanPreferencesKey("ui_candidates_paging_audio_enabled")] = new.candidatesPagingAudioEnabled
        if (old.practicePlaybackSpeed != new.practicePlaybackSpeed)
            prefs[floatPreferencesKey("ui_practice_playback_speed")] = new.practicePlaybackSpeed
        if (old.practiceShowFingerOverlay != new.practiceShowFingerOverlay)
            prefs[booleanPreferencesKey("ui_practice_show_finger_overlay")] = new.practiceShowFingerOverlay
        if (old.practiceShowSwipeTrail != new.practiceShowSwipeTrail)
            prefs[booleanPreferencesKey("ui_practice_show_swipe_trail")] = new.practiceShowSwipeTrail
    }
}
