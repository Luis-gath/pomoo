package com.example.pomodoro.shared.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import java.util.Calendar
import java.util.TimeZone

/**
 * Selectores de fecha y hora compartidos por toda la app.
 *
 * Antes había dos implementaciones distintas: la hoja "Nueva Tarea" usaba las de
 * Material 3 y el editor de tareas las del sistema (`android.app.DatePickerDialog`),
 * con lo que el mismo gesto se veía diferente según la pantalla.
 */

/**
 * El DatePicker de Material 3 trabaja en UTC: devuelve la medianoche UTC del día
 * seleccionado. Combinar eso con una hora local sin convertir desplaza la fecha un día
 * en cualquier zona con desfase negativo (en UTC−5, el 5 de agosto a medianoche UTC son
 * las 19:00 del día 4). Estas dos funciones hacen la conversión en ambos sentidos.
 */
object PickerDateUtils {

    /** Instante local -> medianoche UTC de ese mismo día, que es lo que espera el DatePicker. */
    fun toPickerUtcMillis(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH)
            )
        }.timeInMillis
    }

    /** Día elegido en el DatePicker (medianoche UTC) + hora local -> instante local. */
    fun combineDateAndTime(pickerUtcDateMillis: Long, hour: Int, minute: Int): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = pickerUtcDateMillis
        }
        return Calendar.getInstance().apply {
            clear()
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                hour,
                minute,
                0
            )
        }.timeInMillis
    }

    /** Hora local (0-23) de un instante. */
    fun hourOf(localMillis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = localMillis }.get(Calendar.HOUR_OF_DAY)

    /** Minuto local de un instante. */
    fun minuteOf(localMillis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = localMillis }.get(Calendar.MINUTE)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroDatePickerDialog(
    initialDateMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (utcDateMillis: Long) -> Unit,
    confirmText: String = "Aceptar"
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis?.let(PickerDateUtils::toPickerUtcMillis)
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { datePickerState.selectedDateMillis?.let(onConfirm) }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        text = { TimePicker(state = timePickerState) }
    )
}
