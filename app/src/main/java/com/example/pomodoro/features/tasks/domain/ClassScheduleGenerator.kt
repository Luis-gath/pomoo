package com.example.pomodoro.features.tasks.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * El horario de un curso: qué días y a qué hora, y durante cuántas semanas.
 *
 * Se modela como «los mismos días cada semana» porque así son los horarios de una
 * universidad o un colegio, que es el caso que hay que resolver. Un horario con semanas
 * alternas o clases sueltas se añade después editando las sesiones.
 */
data class ClassSchedulePlan(
    val courseName: String,
    val days: Set<DayOfWeek>,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val weeks: Int,
    val areaId: Int? = null,
    val withReminders: Boolean = true
) {
    val durationMinutes: Int
        get() = Duration.between(startTime, endTime).toMinutes().toInt()

    val totalSessions: Int get() = days.size * weeks

    val minutesPerWeek: Int get() = days.size * durationMinutes

    val isValid: Boolean get() = days.isNotEmpty() && durationMinutes > 0
}

/** Una clase concreta en el calendario. */
data class ClassSession(
    val startMillis: Long,
    val durationMinutes: Int
)

/**
 * Convierte el horario de un curso en sesiones concretas.
 *
 * Genera por adelantado, igual que las rutinas y la repetición de tareas: cada clase es
 * una tarea normal, así que aparece en el calendario y se le puede arrancar el Pomodoro.
 *
 * Es lógica pura y sin Android para poder comprobarla con tests, que es donde se ven los
 * casos molestos: empezar a mitad de semana, o pedir un día que ya pasó.
 */
object ClassScheduleGenerator {

    /**
     * Tope de semanas. Un ciclo académico rara vez pasa de veinte, y cada sesión es una
     * fila y una alarma.
     */
    const val MAX_WEEKS = 20

    fun generate(
        plan: ClassSchedulePlan,
        from: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<ClassSession> {
        if (!plan.isValid) return emptyList()

        val weeks = plan.weeks.coerceIn(1, MAX_WEEKS)

        return plan.days
            .flatMap { day ->
                // El primer día de la semana en curso que aún no ha pasado; si ya pasó,
                // la primera clase cae la semana siguiente.
                val shift = ((day.value - from.dayOfWeek.value) + 7) % 7
                val first = from.plusDays(shift.toLong())

                (0 until weeks).map { week ->
                    val date = first.plusWeeks(week.toLong())
                    ClassSession(
                        startMillis = date.atTime(plan.startTime)
                            .atZone(zone)
                            .toInstant()
                            .toEpochMilli(),
                        durationMinutes = plan.durationMinutes
                    )
                }
            }
            // Se generan por día y luego se ordenan: así la lista sale en orden real de
            // calendario y no agrupada por día de la semana.
            .sortedBy { it.startMillis }
    }
}
