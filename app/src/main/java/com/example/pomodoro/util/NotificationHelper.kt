package com.example.pomodoro.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pomodoro.MainActivity
import com.example.pomodoro.R
import com.example.pomodoro.data.model.TaskEntity
import com.example.pomodoro.receiver.TaskReceiver

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "recordatorios_tareas"
        const val CHANNEL_NAME = "Recordatorios de Tareas"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para recordatorios de tareas"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showTaskNotification(task: TaskEntity) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "task_detail/${task.id}")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, task.id, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, TaskReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("ACTION", "MARK_AS_DONE")
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context, task.id + 10000, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, TaskReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("ACTION", "SNOOZE")
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, task.id + 20000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use appropriate icon
            .setContentTitle(task.title)
            .setContentText("${task.courseOrProject} - ${formatTime(task.timestamp)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Hecho", donePendingIntent)
            .addAction(0, "Posponer 10 min", snoozePendingIntent)
            .build()

        manager.notify(task.id, notification)
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
