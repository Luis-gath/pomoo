package com.example.pomodoro.core.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

private const val TAG = "SessionFeedback"

/** Patrón de vibración al terminar un intervalo: espera, vibra, pausa, vibra. */
private val INTERVAL_PATTERN = longArrayOf(0, 400, 200, 400)

/** Patrón más largo al completar una tarea entera. */
private val TASK_DONE_PATTERN = longArrayOf(0, 250, 150, 250, 150, 600)

/**
 * Aviso háptico y sonoro al terminar un intervalo o una tarea.
 *
 * Se dispara explícitamente desde el código en vez de delegarlo en el canal de
 * notificación, por dos razones: así los interruptores de Ajustes ("Vibración" y
 * "Sonido") mandan de verdad — la configuración de un canal no se puede cambiar una vez
 * creado — y así el aviso funciona aunque el usuario haya denegado las notificaciones.
 */
class SessionFeedback(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo obtener el servicio de vibración", e)
            null
        }
    }

    fun onIntervalFinished(vibrationEnabled: Boolean, soundEnabled: Boolean) {
        if (vibrationEnabled) vibrate(INTERVAL_PATTERN)
        if (soundEnabled) playAlertSound()
    }

    fun onTaskCompleted(vibrationEnabled: Boolean, soundEnabled: Boolean) {
        if (vibrationEnabled) vibrate(TASK_DONE_PATTERN)
        if (soundEnabled) playAlertSound()
    }

    private fun vibrate(pattern: LongArray) {
        val device = vibrator ?: return
        if (!device.hasVibrator()) return

        try {
            // -1 = sin repetición
            device.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo vibrar", e)
        }
    }

    private fun playAlertSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
            RingtoneManager.getRingtone(context, uri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo reproducir el aviso sonoro", e)
        }
    }
}
