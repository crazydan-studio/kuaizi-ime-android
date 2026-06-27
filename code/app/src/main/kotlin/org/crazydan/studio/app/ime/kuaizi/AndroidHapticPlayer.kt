package org.crazydan.studio.app.ime.kuaizi

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.crazydan.studio.app.ime.kuaizi.engine.HapticType
import org.crazydan.studio.app.ime.kuaizi.ui.HapticPlayer

class AndroidHapticPlayer(context: Context) : HapticPlayer {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val effects: Map<HapticType, VibrationEffect> = mapOf(
        HapticType.LightTap to VibrationEffect.createOneShot(20, 128),
        HapticType.MediumTap to VibrationEffect.createOneShot(50, 180),
        HapticType.HeavyTap to VibrationEffect.createOneShot(100, 255),
    )

    override fun play(type: HapticType) {
        val effect = effects[type] ?: return
        vibrator.vibrate(effect)
    }
}
