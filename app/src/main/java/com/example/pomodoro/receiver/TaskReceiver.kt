package com.example.pomodoro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pomodoro.data.local.TaskDatabase
import com.example.pomodoro.data.model.TaskEntity
import com.example.pomodoro.service.TaskSpeakingService
import com.example.pomodoro.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val action = intent.getStringExtra("ACTION")

        if (taskId == -1) return

        val database = TaskDatabase.getInstance(context)
        val dao = database.taskDao
        val scheduler = TaskAlarmScheduler(context)
        val notificationHelper = NotificationHelper(context)

        CoroutineScope(Dispatchers.IO).launch {
            val task = dao.getTaskById(taskId) ?: return@launch

            when (action) {
                "FIRE_ALARM" -> {
                    notificationHelper.showTaskNotification(task)
                    if (task.isVoiceEnabled) {
                        val serviceIntent = Intent(context, TaskSpeakingService::class.java).apply {
                            putExtra("TASK_TITLE", task.title)
                            putExtra("TASK_SUBJECT", task.courseOrProject)
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                }
                "MARK_AS_DONE" -> {
                    val updatedTask = task.copy(isCompleted = true, completedAt = System.currentTimeMillis())
                    dao.updateTask(updatedTask)
                    scheduler.cancel(taskId)
                    // Dismiss notification
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    notificationManager.cancel(taskId)
                }
                "SNOOZE" -> {
                    val snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000
                    val updatedTask = task.copy(timestamp = snoozeTime)
                    dao.updateTask(updatedTask)
                    scheduler.schedule(updatedTask)
                    // Dismiss notification
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    notificationManager.cancel(taskId)
                }
            }
        }
    }
}
