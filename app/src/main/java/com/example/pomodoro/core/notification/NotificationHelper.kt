package com.example.pomodoro.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pomodoro.MainActivity
import com.example.pomodoro.R
import com.example.pomodoro.features.timer.domain.PomodoroMode
import com.example.pomodoro.features.timer.service.PomodoroForegroundService

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pomodoro Timer",
            NotificationManager.IMPORTANCE_LOW 
        ).apply {
            description = "Muestra el estado del temporizador"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildTimerNotification(
        mode: PomodoroMode,
        timeLeftFormatted: String,
        isRunning: Boolean
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action Intents
        val toggleIntent = Intent(context, PomodoroForegroundService::class.java).apply {
            action = if (isRunning) PomodoroForegroundService.ACTION_PAUSE else PomodoroForegroundService.ACTION_RESUME
        }
        val togglePendingIntent = PendingIntent.getService(
            context, 1, toggleIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = Intent(context, PomodoroForegroundService::class.java).apply {
            action = PomodoroForegroundService.ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            context, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleActionTitle = if (isRunning) "Pausar" else "Continuar"
        val toggleIcon = if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Pomodoro: ${mode.title}")
            .setContentText(timeLeftFormatted)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(toggleIcon, toggleActionTitle, togglePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Siguiente", nextPendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1))
            .build()
    }

    /**
     * Canal de alertas sin sonido ni vibración propios: de eso se encarga
     * [com.example.pomodoro.core.feedback.SessionFeedback], que sí respeta los
     * interruptores de Ajustes. Si el canal también avisara, sonaría y vibraría por
     * duplicado y el usuario no podría desactivarlo (la config de un canal es inmutable).
     */
    private fun createAlertChannel() {
        val channel = NotificationChannel(
            CHANNEL_HIGH_PRIORITY_ID,
            "Pomodoro Alertas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de fin de intervalo y de tarea completada"
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showCompleteNotification(mode: PomodoroMode) {
        createAlertChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_HIGH_PRIORITY_ID)
            .setContentTitle("¡Tiempo terminado!")
            .setContentText("${mode.title} ha finalizado.")
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_COMPLETE_ID, notification)
    }

    fun showTaskCompleteNotification(taskTitle: String) {
        createAlertChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_HIGH_PRIORITY_ID)
            .setContentTitle("🎉 ¡Tarea completada!")
            .setContentText("Has completado todos los pomodoros de: $taskTitle")
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_TASK_COMPLETE_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "pomodoro_timer_channel"

        // v2: el canal original se creó con sonido y vibración del sistema, y esa
        // configuración ya no se puede cambiar. Se usa un id nuevo para poder silenciarlo
        // y que manden los ajustes de la app.
        const val CHANNEL_HIGH_PRIORITY_ID = "pomodoro_alert_channel_v2"
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_COMPLETE_ID = 2
        const val NOTIFICATION_TASK_COMPLETE_ID = 3
    }
}
