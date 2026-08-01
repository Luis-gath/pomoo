package com.example.pomodoro.features.tasks.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
private const val TAG = "TaskAlarmScheduler"

class TaskAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(task: TaskEntity) {
        if (!task.isNotificationEnabled) return

        val intent = Intent(context, TaskReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("ACTION", "FIRE_ALARM")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // En Android 12+ la alarma exacta requiere permiso; si no está concedido
        // se degrada a una alarma inexacta en lugar de perder el recordatorio.
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.timestamp,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Sin permiso de alarma exacta para la tarea ${task.id}; se usa alarma inexacta", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.timestamp,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo programar la alarma de la tarea ${task.id}", e)
        }
    }

    fun cancel(taskId: Int) {
        val intent = Intent(context, TaskReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
