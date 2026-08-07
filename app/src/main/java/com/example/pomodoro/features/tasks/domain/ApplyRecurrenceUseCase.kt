package com.example.pomodoro.features.tasks.domain

import com.example.pomodoro.features.tasks.data.RepeatType
import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskRepository
import java.util.UUID
import javax.inject.Inject

data class RecurrenceApplied(
    val created: Int,
    val lastOccurrenceMillis: Long?,
    /** La primera de la serie: es la que la pantalla debe abrir o iniciar. */
    val first: TaskEntity?
)

/**
 * Guarda una tarea y, si se repite, también sus siguientes ocurrencias.
 *
 * Sigue el mismo camino que las rutinas semanales: no hay tabla ni proceso nuevos, cada
 * repetición es una tarea normal que comparte [TaskEntity.scheduleSeriesId] con el resto
 * de la serie. Así aparecen en el calendario, disparan sus recordatorios, y el
 * replanificador ya distingue «editar solo esta» de «editar todas».
 *
 * Recibe la entidad ya construida en lugar de una lista de parámetros para no perder por
 * el camino campos como la nota de voz o el audio: la repetición debe ser idéntica a la
 * original salvo en la fecha.
 *
 * Lo que **no** hace: reponerse sola al agotarse. Cuando se acaben las ocurrencias hay que
 * volver a crear la tarea, igual que ocurre con las rutinas.
 */
class ApplyRecurrenceUseCase @Inject constructor(
    private val repository: TaskRepository
) {

    suspend operator fun invoke(base: TaskEntity, repeatCount: Int): RecurrenceApplied {
        val startMillis = base.dueDateTimeMillis ?: base.dueDateTime

        // Sin fecha no hay nada que repetir: no se sabe desde cuándo contar.
        val type = if (startMillis == null) RepeatType.NONE else base.repeatType

        if (type == RepeatType.NONE) {
            val id = repository.insertOrUpdateTask(base.copy(repeatType = RepeatType.NONE))
            return RecurrenceApplied(
                created = 1,
                lastOccurrenceMillis = startMillis,
                first = repository.getTaskById(id.toInt())
            )
        }

        val instants = RecurrenceGenerator.occurrences(startMillis!!, type, repeatCount)
        val seriesId = base.scheduleSeriesId ?: UUID.randomUUID().toString()

        // Si la base ya está guardada (viene del alta rápida) solo se le pone el
        // identificador de serie y se crean las siguientes; si no lo está (viene del
        // editor), la primera ocurrencia la crea este mismo bucle.
        val baseAlreadySaved = base.id != 0
        var firstId: Int? = base.id.takeIf { baseAlreadySaved }

        if (baseAlreadySaved) {
            repository.insertOrUpdateTask(base.copy(scheduleSeriesId = seriesId))
        }

        val pending = if (baseAlreadySaved) instants.drop(1) else instants
        pending.forEach { instant ->
            val end = TaskDurationCalculator.calculateEndTime(instant, base.computedDurationMinutes)
            val id = repository.insertOrUpdateTask(
                base.copy(
                    // id 0 fuerza un alta nueva: si se reutilizara el id, cada ocurrencia
                    // sobrescribiría a la anterior en lugar de añadirse.
                    id = 0,
                    dueDateTime = instant,
                    dueDateTimeMillis = instant,
                    startDateTimeMillis = instant,
                    endDateTimeMillis = end,
                    timestamp = instant,
                    scheduleSeriesId = seriesId
                )
            )
            if (firstId == null) firstId = id.toInt()
        }

        return RecurrenceApplied(
            created = instants.size,
            lastOccurrenceMillis = instants.lastOrNull(),
            first = firstId?.let { repository.getTaskById(it) }
        )
    }
}
