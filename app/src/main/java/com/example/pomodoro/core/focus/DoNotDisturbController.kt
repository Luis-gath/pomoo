package com.example.pomodoro.core.focus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

private const val TAG = "DoNotDisturb"

/**
 * Silencia el móvil entero durante los bloques de enfoque.
 *
 * Bloquear las notificaciones de **otras** aplicaciones solo es posible activando el modo
 * "No molestar" del sistema, y para eso Android exige un permiso especial que el usuario
 * concede a mano en una pantalla de ajustes: no se puede pedir con un diálogo normal.
 *
 * Si el permiso no está concedido, esta clase no hace nada y el modo enfoque sigue
 * funcionando igual, solo que sin aislar de avisos ajenos.
 */
class DoNotDisturbController(private val context: Context) {

    private val notificationManager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    /** Filtro que había antes de empezar, para dejarlo como estaba al terminar. */
    private var previousFilter: Int? = null

    fun hasAccess(): Boolean =
        notificationManager?.isNotificationPolicyAccessGranted == true

    /** Pantalla del sistema donde se concede el acceso. */
    fun accessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun startBlocking() {
        val manager = notificationManager ?: return
        if (!hasAccess() || previousFilter != null) return

        try {
            previousFilter = manager.currentInterruptionFilter
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo activar No molestar", e)
            previousFilter = null
        }
    }

    fun stopBlocking() {
        val manager = notificationManager ?: return
        val restore = previousFilter ?: return

        try {
            manager.setInterruptionFilter(restore)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo restaurar el estado anterior de No molestar", e)
        } finally {
            previousFilter = null
        }
    }
}
