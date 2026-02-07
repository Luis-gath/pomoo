package com.example.pomodoro.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.pomodoro.data.model.TaskEntity

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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Fallback or just return? For now, we try to use setAndAllowWhileIdle if expected precision is loose, 
                // but for "Exact" requires permission. warning user is UI job. 
                // We'll use setAndAllowWhileIdle which behaves like exact on <12, but inexact on 12+ if no permission? 
                // Actually setExact throws. setAndAllowWhileIdle also requires permission??
                // No, setAndAllowWhileIdle acts like setExactAndAllowWhileIdle BUT might be throttled? 
                // Actually, if we don't have permission we should catch exception.
                // Let's just try-catch.
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.timestamp,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            // Fallback for when permission is revoked
             alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.timestamp,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
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
