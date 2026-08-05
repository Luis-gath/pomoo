package com.example.pomodoro.features.areas.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pomodoro.core.notification.DeliverableNotifier
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "DeliverableReceiver"

/**
 * Se usa un [EntryPoint] y no `@AndroidEntryPoint` por el mismo motivo que en
 * TaskReceiver: ese último exige llamar a `super.onReceive`, que no compila en Kotlin.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DeliverableReceiverEntryPoint {
    fun areaRepository(): AreaRepository
    fun deliverableNotifier(): DeliverableNotifier
    fun deliverableAlarmScheduler(): DeliverableAlarmScheduler
}

class DeliverableReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getIntExtra("ITEM_ID", -1)
        val action = intent.getStringExtra("ACTION")
        if (itemId == -1) return

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            DeliverableReceiverEntryPoint::class.java
        )
        val repository = entryPoint.areaRepository()
        val notifier = entryPoint.deliverableNotifier()
        val scheduler = entryPoint.deliverableAlarmScheduler()

        // goAsync evita que el proceso muera antes de terminar la lectura o la escritura.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val item = repository.getItemById(itemId) ?: return@launch

                when (action) {
                    "FIRE_DUE_REMINDER" -> {
                        // Una entrega ya hecha no debe avisar aunque quedara una alarma viva.
                        if (!item.isPendingDeliverable) return@launch
                        val areaName = repository.getAreaById(item.areaId)?.name.orEmpty()
                        notifier.show(item, areaName)
                    }

                    "MARK_DONE" -> {
                        repository.setDeliverableCompleted(itemId, System.currentTimeMillis())
                        scheduler.cancel(itemId)
                        notifier.cancel(itemId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando '$action' del material $itemId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
