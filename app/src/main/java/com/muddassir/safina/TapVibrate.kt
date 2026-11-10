package com.muddassir.safina

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Real vibration for taps, so the buzz happens even when the system
 * "touch feedback" setting is off. Requires android.permission.VIBRATE.
 */
class TapVibrate(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private val canVibrate = vibrator?.hasVibrator() == true

    /** Light tick for a normal tap. */
    fun light() = buzz(14L, 120)

    /** Stronger pulse every 25 counts. */
    fun strong() = buzz(45L, 200)

    private fun buzz(millis: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (!canVibrate) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(millis, amplitude))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(millis)
        }
    }
}
