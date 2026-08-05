package com.example.pomodoro.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pomodoro.MainActivity
import com.example.pomodoro.R
import com.example.pomodoro.features.areas.data.DeliverableReceiver
import com.example.pomodoro.features.areas.data.Item
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Avisos de entregas próximas.
 *
 * Canal propio y no el de tareas para que el usuario pueda silenciar los recordatorios de
 * rutina sin perderse una fecha de entrega.
 */
class DeliverableNotifier(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "entregas"
        const val CHANNEL_NAME = "Entregas"

        /**
         * Identificadores de notificación. Este espacio es independiente del de los
         * códigos de PendingIntent, pero se separa igualmente del de las tareas, que
         * usan el propio identificador de tarea.
         */
        private const val NOTIFICATION_BASE = 2_000_000

        /** Códigos de los PendingIntent de la acción «Entregado», en su propio tramo. */
        private const val ACTION_BASE = 3_000_000
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Avisos de fechas de entrega" }
            manager().createNotificationChannel(channel)
        }
    }

    fun show(item: Item, areaName: String) {
        val dueAt = item.dueAt ?: return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "area_detail/${item.areaId}")
        }
        val openPending = PendingIntent.getActivity(
            context,
            NOTIFICATION_BASE + item.id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, DeliverableReceiver::class.java).apply {
            putExtra("ITEM_ID", item.id)
            putExtra("ACTION", "MARK_DONE")
        }
        val donePending = PendingIntent.getBroadcast(
            context,
            ACTION_BASE + item.id,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(item.title)
            .setContentText("$areaName · entrega ${formatDue(dueAt)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, "Entregado", donePending)
            .build()

        manager().notify(NOTIFICATION_BASE + item.id, notification)
    }

    /** Retira el aviso. Vive aquí para que el identificador no se repita a mano fuera. */
    fun cancel(itemId: Int) {
        manager().cancel(NOTIFICATION_BASE + itemId)
    }

    private fun manager() =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun formatDue(millis: Long): String =
        SimpleDateFormat("d MMM 'a las' HH:mm", Locale.getDefault()).format(Date(millis))
}
