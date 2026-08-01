package com.example.pomodoro.features.tasks.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pomodoro.features.tasks.service.TaskSpeakingService
import com.example.pomodoro.core.notification.TaskReminderNotifier
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "TaskReceiver"

/**
 * Dependencias que necesita el receiver.
 *
 * Se usa un [EntryPoint] en lugar de `@AndroidEntryPoint` porque ese último exige llamar a
 * `super.onReceive(...)`, que en Kotlin no compila al ser un miembro abstracto de
 * BroadcastReceiver. El objetivo se cumple igual: el grafo lo resuelve Hilt, no el receiver.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TaskReceiverEntryPoint {
    fun taskRepository(): TaskRepository
    fun alarmScheduler(): TaskAlarmScheduler
    fun reminderNotifier(): TaskReminderNotifier
}

class TaskReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val action = intent.getStringExtra("ACTION")
        if (taskId == -1) return

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            TaskReceiverEntryPoint::class.java
        )
        val taskRepository = entryPoint.taskRepository()
        val alarmScheduler = entryPoint.alarmScheduler()
        val reminderNotifier = entryPoint.reminderNotifier()

        // goAsync() evita que el sistema mate el proceso antes de terminar la escritura en BD.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val task = taskRepository.getTaskById(taskId) ?: return@launch

                when (action) {
                    "FIRE_ALARM" -> {
                        reminderNotifier.showTaskNotification(task)
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
                        // markTaskDone deja status = DONE, que es lo que consultan las listas.
                        taskRepository.markTaskDone(taskId)
                        cancelNotification(context, taskId)
                    }

                    "SNOOZE" -> {
                        val snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000
                        val updatedTask = task.copy(timestamp = snoozeTime)
                        taskRepository.insertOrUpdateTask(updatedTask)
                        alarmScheduler.schedule(updatedTask)
                        cancelNotification(context, taskId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando la acción '$action' de la tarea $taskId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun cancelNotification(context: Context, taskId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(taskId)
    }
}
