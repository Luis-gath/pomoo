package com.example.pomodoro.features.tasks.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Base64
import java.util.zip.CRC32

sealed interface ImportedStudyPlan {
    data class Weekly(
        val template: WeeklyScheduleTemplate,
        val weeks: Int,
        val courseOrProject: String,
        val withReminders: Boolean
    ) : ImportedStudyPlan

    data class Habit(val plan: StudyHabitPlan) : ImportedStudyPlan
}

/**
 * Formato portátil y versionado para intercambiar planes sin cuenta ni servidor.
 *
 * El QR contiene el plan completo. El CRC detecta códigos truncados o modificados antes de
 * mostrar la previsualización; los límites evitan que un código externo cree una cantidad
 * descontrolada de tareas.
 */
object StudyPlanCodeCodec {
    private const val PREFIX = "POMOO1:"
    private const val MAX_INPUT_LENGTH = 12_000
    private val codePattern = Regex("POMOO1:[A-Za-z0-9_-]+\\.[0-9A-Fa-f]{8}")
    private val json = Json { ignoreUnknownKeys = false }

    fun encodeWeekly(
        template: WeeklyScheduleTemplate,
        weeks: Int,
        courseOrProject: String,
        withReminders: Boolean
    ): String {
        require(weeks in 1..8) { "La duración del horario no es válida" }
        return encode(
            StudyPlanPayload(
                type = "weekly",
                weeks = weeks,
                course = courseOrProject.cleanCourse(),
                reminders = withReminders,
                template = template.name
            )
        )
    }

    fun encodeHabit(plan: StudyHabitPlan): String = encode(
        StudyPlanPayload(
            type = "habit",
            weeks = plan.weeks,
            course = plan.courseOrProject.cleanCourse(),
            reminders = plan.withReminders,
            days = plan.days.map { it.value }.sorted(),
            hour = plan.time.hour,
            minute = plan.time.minute,
            sessionLength = plan.sessionLength.name,
            cue = plan.startCue.trim().take(MAX_CUE_LENGTH)
        )
    )

    fun decode(input: String): Result<ImportedStudyPlan> = runCatching {
        require(input.length <= MAX_INPUT_LENGTH) { "El código es demasiado largo" }
        val code = codePattern.find(input.trim())?.value
            ?: throw IllegalArgumentException("No se encontró un código de horario válido")

        val body = code.removePrefix(PREFIX)
        val separator = body.lastIndexOf('.')
        require(separator > 0) { "El código está incompleto" }

        val encoded = body.substring(0, separator)
        val expectedChecksum = body.substring(separator + 1).uppercase()
        val bytes = runCatching { Base64.getUrlDecoder().decode(encoded) }
            .getOrElse { throw IllegalArgumentException("El código está dañado") }
        require(bytes.size <= 8_192) { "El plan supera el tamaño permitido" }
        require(checksum(bytes) == expectedChecksum) { "El código está incompleto o fue modificado" }

        val payload = json.decodeFromString<StudyPlanPayload>(
            bytes.toString(StandardCharsets.UTF_8)
        )
        payload.toImportedPlan()
    }

    private fun encode(payload: StudyPlanPayload): String {
        val bytes = json.encodeToString(payload).toByteArray(StandardCharsets.UTF_8)
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return "$PREFIX$encoded.${checksum(bytes)}"
    }

    private fun StudyPlanPayload.toImportedPlan(): ImportedStudyPlan {
        require(version == 1) { "Esta versión del horario todavía no es compatible" }
        require(course.length <= MAX_COURSE_LENGTH) { "El nombre del curso es demasiado largo" }

        return when (type) {
            "weekly" -> {
                require(weeks in 1..8) { "La duración del horario no es válida" }
                val parsedTemplate = runCatching { WeeklyScheduleTemplate.valueOf(template.orEmpty()) }
                    .getOrElse { throw IllegalArgumentException("La plantilla del horario no existe") }
                ImportedStudyPlan.Weekly(parsedTemplate, weeks, course.trim(), reminders)
            }

            "habit" -> {
                require(weeks in 1..12) { "La duración del hábito no es válida" }
                require(days.isNotEmpty() && days.distinct().size == days.size) { "Los días del hábito no son válidos" }
                require(days.all { it in 1..7 }) { "Los días del hábito no son válidos" }
                require(hour in 0..23 && minute in 0..59) { "La hora del hábito no es válida" }
                require(cue.length <= MAX_CUE_LENGTH) { "La señal de inicio es demasiado larga" }
                val length = runCatching { HabitSessionLength.valueOf(sessionLength.orEmpty()) }
                    .getOrElse { throw IllegalArgumentException("La duración de la sesión no existe") }

                ImportedStudyPlan.Habit(
                    StudyHabitPlan(
                        days = days.mapTo(linkedSetOf()) { DayOfWeek.of(it) },
                        time = LocalTime.of(hour, minute),
                        sessionLength = length,
                        weeks = weeks,
                        courseOrProject = course.trim(),
                        startCue = cue.trim(),
                        withReminders = reminders
                    )
                )
            }

            else -> throw IllegalArgumentException("El tipo de plan no es compatible")
        }
    }

    private fun checksum(bytes: ByteArray): String = CRC32().run {
        update(bytes)
        value.toString(16).uppercase().padStart(8, '0')
    }

    private fun String.cleanCourse(): String = trim().take(MAX_COURSE_LENGTH)

    private const val MAX_COURSE_LENGTH = 120
    private const val MAX_CUE_LENGTH = 200
}

@Serializable
private data class StudyPlanPayload(
    val version: Int = 1,
    val type: String,
    val weeks: Int,
    val course: String = "",
    val reminders: Boolean = true,
    val template: String? = null,
    val days: List<Int> = emptyList(),
    val hour: Int = 0,
    val minute: Int = 0,
    val sessionLength: String? = null,
    val cue: String = ""
)
