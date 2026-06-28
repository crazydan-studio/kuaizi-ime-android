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

package org.crazydan.studio.app.ime.kuaizi.engine

import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardHandMode
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardInputMode
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardThemeType
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogLevel

data class ImeConfig(
    val engine: EngineConfig = EngineConfig(),
    val ui: UiConfig = UiConfig(),
    val runtime: RuntimeConfig = RuntimeConfig(),
) {
    data class EngineConfig(
        val logLevel: LogLevel = LogLevel.WARN,
        val logStoragePath: String? = null,
        val inputPredictionEnabled: Boolean = true,
        val userDataPersistEnabled: Boolean = true,
        val favoriteInputEnabled: Boolean = true,
        val favoriteClipEnabled: Boolean = true,
        val favoriteSyncToUserDictEnabled: Boolean = false,
        val candidateVariantFirstEnabled: Boolean = false,
    )

    data class UiConfig(
        val keyboardInputMode: KeyboardInputMode = KeyboardInputMode.RectGrid,
        val keyboardHandMode: KeyboardHandMode = KeyboardHandMode.Right,
        val keyboardThemeType: KeyboardThemeType = KeyboardThemeType.FollowSystem,
        val keyPopupTipsEnabled: Boolean = true,
        val audioFeedbackEnabled: Boolean = true,
        val hapticFeedbackEnabled: Boolean = true,
        val keyAnimationEnabled: Boolean = true,
        val gestureSlippingTrailEnabled: Boolean = true,
        val clipPopupTipsEnabled: Boolean = true,
        val clipPastePopupTipsEnabled: Boolean = true,
        val clipPopupTipsTimeout: Int = 15,
        val adaptDesktopSwipeUpGesture: Boolean = false,
        val candidatesPagingAudioEnabled: Boolean = true,
        val practicePlaybackSpeed: Float = 1.0f,
        val practiceShowFingerOverlay: Boolean = true,
        val practiceShowSwipeTrail: Boolean = true,
    )

    data class RuntimeConfig(
        val screenOrientation: ScreenOrientation = ScreenOrientation.Landscape,
        val editorInputType: EditorInputType = EditorInputType.Text,
        val keyPopupTipsEnabled: Boolean? = null,
        val toolSettingsEnabled: Boolean = true,
        val toolSwitchIMEEnabled: Boolean = true,
        val toolCloseKeyboardEnabled: Boolean = true,
    )
}

data class StartupConfig(
    val imeSubtype: IMESubtype,
    val screenOrientation: ScreenOrientation,
    val editorInputType: EditorInputType?,
)

enum class ScreenOrientation { Landscape, Portrait }
enum class EditorInputType { Filter, Number, Datetime, Phone, Password, Email, URI, Text }
enum class IMESubtype { Latin, Hans }
