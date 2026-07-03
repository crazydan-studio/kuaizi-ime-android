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

package org.crazydan.studio.app.ime.kuaizi.device

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.crazydan.studio.app.ime.kuaizi.ui.feedback.HapticPlayer
import org.crazydan.studio.app.ime.kuaizi.ui.feedback.HapticType

/**
 * 基于 Android [Vibrator] 的触觉播放器实现。
 *
 * 在 [IMEService.onCreate] 中创建并注入 [KeyboardViewModel]。
 * 使用 [VibrationEffect.createOneShot] API 创建单次振动效果，
 * 不同 [HapticType] 对应不同的振动时长和强度。
 */
class DefaultHapticPlayer(context: Context) : HapticPlayer {
    // Vibrator 实例，根据 API 等级选择兼容的获取方式
    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    // 触觉类型到振动效果的映射，预设时长和振幅参数
    private val effects: Map<HapticType, VibrationEffect> =
        mapOf(
            HapticType.LightTap to VibrationEffect.createOneShot(20, 128),   // 轻触：20ms，50% 振幅
            HapticType.MediumTap to VibrationEffect.createOneShot(50, 180),  // 中触：50ms，70% 振幅
            HapticType.HeavyTap to VibrationEffect.createOneShot(100, 255),  // 重触：100ms，100% 振幅
        )

    // -------------------------------------------------------------

    /** 播放指定类型的触觉反馈。若振动效果未定义则静默跳过。 */
    override fun play(type: HapticType) {
        val effect = effects[type] ?: return

        vibrator.vibrate(effect)
    }
}
