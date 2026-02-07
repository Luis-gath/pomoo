package com.example.pomodoro.notification

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
import com.example.pomodoro.domain.model.PomodoroMode
import com.example.pomodoro.service.PomodoroForegroundService

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
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Placeholder
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(toggleIcon, toggleActionTitle, togglePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Siguiente", nextPendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1))
            .build()
    }

    fun showCompleteNotification(mode: PomodoroMode) {
        val channel = NotificationChannel(
            CHANNEL_HIGH_PRIORITY_ID,
            "Pomodoro Alertas",
            NotificationManager.IMPORTANCE_HIGH
        )
         notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_HIGH_PRIORITY_ID)
            .setContentTitle("¡Tiempo terminado!")
            .setContentText("${mode.title} ha finalizado.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_COMPLETE_ID, notification)
    }

    fun showTaskCompleteNotification(taskTitle: String) {
        val channel = NotificationChannel(
            CHANNEL_HIGH_PRIORITY_ID,
            "Pomodoro Alertas",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_HIGH_PRIORITY_ID)
            .setContentTitle("🎉 ¡Tarea completada!")
            .setContentText("Has completado todos los pomodoros de: $taskTitle")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_TASK_COMPLETE_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "pomodoro_timer_channel"
        const val CHANNEL_HIGH_PRIORITY_ID = "pomodoro_alert_channel"
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_COMPLETE_ID = 2
        const val NOTIFICATION_TASK_COMPLETE_ID = 3
    }
}
