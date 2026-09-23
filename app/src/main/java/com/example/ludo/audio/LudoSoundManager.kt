package com.example.ludo.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LudoSoundManager(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    fun playDiceRoll() {
        vibrate(35)
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
            } catch (_: Exception) {}
        }
    }

    fun playTokenMove() {
        vibrate(20)
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 40)
            } catch (_: Exception) {}
        }
    }

    fun playCapture() {
        vibratePattern(longArrayOf(0, 50, 40, 90))
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 120)
            } catch (_: Exception) {}
        }
    }

    fun playError() {
        vibrate(30)
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 60)
            } catch (_: Exception) {}
        }
    }

    fun playSixRolled() {
        vibratePattern(longArrayOf(0, 30, 40, 60))
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
            } catch (_: Exception) {}
        }
    }

    fun playHomeReached() {
        vibratePattern(longArrayOf(0, 60, 50, 80))
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
            } catch (_: Exception) {}
        }
    }

    fun playWin() {
        vibratePattern(longArrayOf(0, 100, 80, 100, 80, 200))
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK, 400)
            } catch (_: Exception) {}
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun vibratePattern(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null
    }
}
