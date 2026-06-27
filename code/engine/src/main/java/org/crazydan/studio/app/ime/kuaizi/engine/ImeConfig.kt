package org.crazydan.studio.app.ime.kuaizi.engine

import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardHandMode
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardInputMode
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardThemeType

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
