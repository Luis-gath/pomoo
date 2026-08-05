package com.example.pomodoro.features.areas.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pomodoro.features.areas.domain.DeliverableReminders

private const val TAG = "DeliverableAlarm"

/**
 * Programa los avisos de una entrega.
 *
 * Cada material genera varias alarmas (una por adelanto), así que siempre se cancelan
 * todas antes de volver a programar: si no, cambiar la fecha dejaría vivo el aviso viejo.
 */
class DeliverableAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(item: Item) {
        cancel(item.id)

        val dueAt = item.dueAt ?: return
        if (!item.isPendingDeliverable) return

        DeliverableReminders.instantsFor(dueAt, System.currentTimeMillis()).forEach { instant ->
            val pending = pendingIntent(item.id, instant.offsetIndex)
            try {
                // En Android 12+ la alarma exacta exige permiso; degradamos antes que
                // perder el aviso de una entrega.
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    instant.triggerAt,
                    pending
                )
            } catch (e: SecurityException) {
                Log.w(TAG, "Sin permiso de alarma exacta para el material ${item.id}", e)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    instant.triggerAt,
                    pending
                )
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo programar el aviso del material ${item.id}", e)
            }
        }
    }

    fun cancel(itemId: Int) {
        DeliverableReminders.OFFSETS_MILLIS.indices.forEach { index ->
            alarmManager.cancel(pendingIntent(itemId, index))
        }
    }

    private fun pendingIntent(itemId: Int, offsetIndex: Int): PendingIntent {
        val intent = Intent(context, DeliverableReceiver::class.java).apply {
            putExtra("ITEM_ID", itemId)
            putExtra("ACTION", "FIRE_DUE_REMINDER")
        }
        return PendingIntent.getBroadcast(
            context,
            DeliverableReminders.requestCode(itemId, offsetIndex),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
