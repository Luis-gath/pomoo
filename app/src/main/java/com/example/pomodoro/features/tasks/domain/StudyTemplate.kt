package com.example.pomodoro.features.tasks.domain

/**
 * Perfiles de estudio con la configuración Pomodoro ya ajustada al tipo de material.
 *
 * La idea es que crear una tarea no obligue a decidir cinco números cada vez: se elige el
 * perfil que encaja con lo que se va a estudiar y los valores se rellenan solos. Siguen
 * siendo editables después, son un punto de partida y no una imposición.
 *
 * Los tiempos no son arbitrarios: los bloques largos convienen para resolver problemas,
 * donde interrumpir a mitad de un ejercicio cuesta caro; los cortos, para memorizar, donde
 * el repaso frecuente rinde más.
 */
enum class StudyTemplate(
    val label: String,
    val hint: String,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val longBreakEvery: Int,
    val totalPomodoros: Int
) {
    CLASICO(
        label = "Clásico",
        hint = "El Pomodoro de toda la vida",
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        longBreakEvery = 4,
        totalPomodoros = 4
    ),

    INGENIERIA(
        label = "Ingeniería",
        hint = "Bloques largos para resolver problemas",
        focusMinutes = 50,
        shortBreakMinutes = 10,
        longBreakMinutes = 20,
        longBreakEvery = 3,
        totalPomodoros = 3
    ),

    MEDICINA(
        label = "Medicina",
        hint = "Bloques cortos con repaso frecuente",
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        longBreakEvery = 4,
        totalPomodoros = 6
    ),

    IDIOMAS(
        label = "Idiomas",
        hint = "Sesiones breves y muy repetidas",
        focusMinutes = 15,
        shortBreakMinutes = 3,
        longBreakMinutes = 10,
        longBreakEvery = 6,
        totalPomodoros = 6
    ),

    LECTURA(
        label = "Lectura",
        hint = "Pocas pausas para no perder el hilo",
        focusMinutes = 45,
        shortBreakMinutes = 15,
        longBreakMinutes = 20,
        longBreakEvery = 2,
        totalPomodoros = 2
    );

    /** Duración total aproximada, para mostrarla antes de elegir. */
    val approximateMinutes: Int
        get() = TaskDurationCalculator.calculateTotalMinutes(
            totalPomodoros = totalPomodoros,
            focusMinutes = focusMinutes,
            shortBreakMinutes = shortBreakMinutes,
            longBreakMinutes = longBreakMinutes,
            longBreakEvery = longBreakEvery,
            includeFinalBreak = false
        )
}
