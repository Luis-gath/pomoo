# Plan de migración de arquitectura — App Pomodoro

> **Objetivo:** pasar de un monolito con capas nominales (`data/domain/ui` con fugas) a un MVVM estricto, con inyección de dependencias, tests y organización feature-first, **sin reescribir la app**.
>
> **Regla de oro:** cada paso deja la app compilando y funcionando. Ningún paso depende de "terminar todo". Marcar cada checkbox solo cuando el criterio de verificación se cumpla.

## Estado de la implementación

**21 de 24 pasos implementados y verificados** (Etapas 0 a 5 completas, más las mejoras rápidas de la 6).
Verificación: `assembleDebug` ✅ · `assembleRelease` con R8 ✅ · **34 tests unitarios, 0 fallos** ✅

Pendientes: paso 22 (verificación de compras en backend), 23 (widget funcional), 24 (limpieza de campos legacy de `TaskEntity`). Los tres son trabajo funcional, no estructural.

### Estructura resultante (paso 18 aplicado)

```text
com.example.pomodoro/
  core/audio/ (5)          core/database/ (3)      core/di/ (3)
  core/notification/ (2)   core/time/ (1)
  features/timer/          domain (6) · data (2) · presentation (13) · service (1)
  features/tasks/          domain (3) · data (5) · presentation (9)  · service (1)
  features/stats/          domain (1) · data (5) · presentation (7)
  features/premium/        data (2)   · presentation (2)
  features/widget/ (2)
  shared/ui/theme/ (3)     navigation/ (1)
```

Desaparecieron `viewmodel/`, `util/`, `ui/components/`, `ui/screens/`, `receiver/` y `notification/`: cada archivo vive ahora junto a la funcionalidad a la que pertenece.

**Acoplamiento entre features que queda (consciente):** `features/timer/domain` depende de `features/tasks` — `PomodoroSessionController` usa `TaskRepository`, `TaskEntity` y `CompleteFocusForTaskUseCase` porque el temporizador registra el progreso de la tarea activa. Es una dependencia real del producto, no un descuido, pero conviene tenerla presente si algún día se separan en módulos Gradle: `tasks` no puede depender de `timer` sin crear un ciclo.

### Desviaciones respecto al plan original

1. **Paso 8 — `TaskReceiver` usa `EntryPointAccessors`, no `@AndroidEntryPoint`.** Este último exige llamar a `super.onReceive(...)`, que no compila en Kotlin por ser miembro abstracto de `BroadcastReceiver`. El objetivo se cumple igual: el grafo lo resuelve Hilt.
2. **Paso 15 — el enlace por binder desapareció.** Al ser `PomodoroSessionController` un `@Singleton`, el ViewModel lo observa directamente y ya no hace falta `ServiceConnection`. Esto además corrige un fallo real: `setActiveTaskWithConfig` se perdía en silencio si el Service aún no estaba enlazado.
3. **Paso 17 — no hizo falta subir a la versión 8 de la base de datos.** La consolidación no cambia el esquema (la tabla `daily_stats` ya existía), así que la importación del historial antiguo se hace una sola vez al arrancar (`LegacyStatsMigrator`). En vez de eliminar `fallbackToDestructiveMigration()` sin red, ahora se usa `fallbackToDestructiveMigrationFrom(1..6)`: destruye solo al venir de esquemas antiguos que nunca se exportaron, y **obliga** a escribir una `Migration` real de la 7 en adelante.
4. **Paso 21 — se usó `android.util.Log`, no Timber**, para no añadir una dependencia por un beneficio marginal en esta etapa. Los `printStackTrace()` ya no existen y los errores de importar audio/fondo sí llegan a la UI vía `PomodoroViewModel.errorMessage`.

### Fallos encontrados y corregidos durante la implementación

- **Bucle infinito de escritura** en `PomodoroViewModel.updateBackground(null)`: hacía `settingsFlow.collect { updateSettings(...) }`, y cada escritura provocaba una nueva emisión. Ahora usa `first()`.
- **Tarea completada que seguía apareciendo como pendiente**: al terminar el último pomodoro, el Service marcaba `isCompleted = true` pero nunca `status = DONE`, que es justo lo que consultan el dashboard y el calendario. Ahora pasa por `CompleteFocusForTaskUseCase`, que sí usa `markTaskDone`. Lo mismo en la acción "Hecho" de la notificación.
- **Riesgo de crash al arrancar el Service**: se llamaba a `startForegroundService()` pero `startForeground()` solo ocurría al iniciar el temporizador, lo que puede lanzar `ForegroundServiceDidNotStartInTimeException`. Ahora se entra en primer plano en `onCreate()`.
- **Las tareas se guardaban un día antes del elegido** en zonas horarias con desfase negativo (UTC−5, la del usuario). El `DatePicker` de Material 3 devuelve la medianoche **UTC** del día seleccionado, y el código la combinaba con una hora **local** sin convertir: el 5 de agosto a las 00:00 UTC son las 19:00 del día 4 en Perú. La conversión vive ahora en `PickerDateUtils`, con tests que fijan el comportamiento en `America/Lima`.
- **Dos selectores de fecha distintos**: la hoja "Nueva Tarea" usaba los de Material 3 y el editor de tareas los del sistema (`android.app.DatePickerDialog`). Unificados en `shared/ui/components/DateTimePickers.kt`.
- **Riesgo de cierre al abrir la hoja "Nueva Tarea"**: se pedía el foco del teclado antes de que el campo existiera (`LaunchedEffect` fuera del `ModalBottomSheet`), lo que puede lanzar `FocusRequester is not initialized`.
- **El móvil no vibraba al terminar**: la vibración no estaba implementada en absoluto — ni una llamada a la API `Vibrator`, ni el permiso `VIBRATE` en el manifest. Los tres interruptores de Ajustes ("Vibración", "Sonido", "Mantener pantalla encendida") se guardaban y se pintaban, pero **ningún código los leía**. Ahora existe `core/feedback/SessionFeedback` (vibración con patrón + sonido de notificación), se dispara desde el Service al terminar un intervalo o una tarea, y `keepScreenOn` aplica `FLAG_KEEP_SCREEN_ON` en la pantalla principal.
- **Las notificaciones no aparecían en Android 13+**: `POST_NOTIFICATIONS` estaba declarado en el manifest pero nunca se pedía en tiempo de ejecución, así que el sistema las descartaba en silencio. Ahora se solicita al abrir la app (`MainActivity.RequestNotificationPermission`).
- **Icono de notificación como mancha blanca**: se usaba `ic_launcher_foreground`, un círculo relleno; Android solo usa el canal alfa del icono pequeño, así que se veía como un círculo blanco sólido. Se creó `ic_notification_timer.xml` (contorno sobre fondo transparente).
- **El tablero kanban no se refrescaba**: `TaskDashboardScreen` leía `viewModel.dashboardState.value` dentro de la composición, y esa lectura no suscribe a cambios. Ahora usa el estado ya recolectado. Lo detectó lint (`StateFlowValueCalledInComposition`).
- **La animación del gráfico de estadísticas no animaba nada**: `AnimatedContent` ignoraba su parámetro `range` y pintaba siempre los mismos datos en ambos lados de la transición. Ahora se anima sobre los datos.
- **`lintDebug` fallaba siempre** (y con él, el CI) por dos detectores de Compose que revientan con Kotlin 2.0. Se desactivan explícitamente en `build.gradle.kts`, documentando que es un fallo de la herramienta y que hay que reactivarlos al subir de AGP.
- **`proguard-rules.pro` no existía** pese a estar referenciado en `build.gradle.kts`.
- **Racha siempre en 0**: `StatsViewModel` la tenía mockeada. Ahora se calcula en Room con `StreakCalculator` (lógica pura, con tests).

> **Antes de publicar:** el APK de release ya compila con R8, pero **no se ha probado en un dispositivo**. Instalar y verificar el flujo completo (temporizador, notificación, compras) antes de subir a Play.

---

## Diagnóstico de partida (situación previa al refactor)

- Kotlin 2.0.21 + Jetpack Compose + Room 2.6.1 + DataStore. Un solo módulo `:app`. Sin backend.
- **Sin DI**: el grafo de objetos se construye a mano 8 veces (`NavGraph.kt`, `PomodoroViewModel.kt`, `StatsViewModel.kt`, `PomodoroForegroundService.kt`, `TaskReceiver.kt`).
- **Fugas de capas**: `TaskEditorScreen` recibe `TaskRepository` como parámetro; `PomodoroViewModel` consulta Room directo; `HomeScreen`/`SettingsScreen` instancian `SettingsDataStore` directo.
- **Capa `domain/usecases` muerta**: 5 use cases con 0 referencias; sus validaciones (`coerceIn`) no se ejecutan en la ruta real de creación de tareas.
- **God Service**: `PomodoroForegroundService` (456 líneas) contiene la máquina de estados completa del Pomodoro, sin tests posibles.
- **Doble fuente de verdad de estadísticas**: `StatsDataStore` (JSON en Preferences) + Room (`DailyStatsEntity`), escritas a la vez.
- **`fallbackToDestructiveMigration()`** en DB versión 7 sin ninguna migración: pérdida de datos garantizada al cambiar el esquema.
- **0 tests**, sin CI, `minifyEnabled = false` en release, sin entornos dev/prod.

---

## Etapa 0 — Red de seguridad (1-2 días, riesgo cero)

Nada de refactor todavía. Primero, poder detectar cuándo se rompe algo.

### ✅ Paso 1. Primeros tests unitarios (sin tocar código de producción)

`PomodoroEngine` y `TaskDurationCalculator` son lógica pura — se testean hoy mismo.

Crear `app/src/test/java/com/example/pomodoro/domain/logic/PomodoroEngineTest.kt`:

```kotlin
class PomodoroEngineTest {
    private val engine = PomodoroEngine()

    @Test
    fun `despues de N focus toca descanso largo`() {
        val (mode, _) = engine.calculateNextMode(PomodoroMode.Focus, currentCycle = 4, longBreakEveryN = 4)
        assertEquals(PomodoroMode.LongBreak, mode)
    }

    @Test
    fun `descanso corto vuelve a focus e incrementa ciclo`() {
        val (mode, cycle) = engine.calculateNextMode(PomodoroMode.ShortBreak, currentCycle = 2, longBreakEveryN = 4)
        assertEquals(PomodoroMode.Focus, mode)
        assertEquals(3, cycle)
    }

    @Test
    fun `descanso largo reinicia el ciclo a 1`() {
        val (mode, cycle) = engine.calculateNextMode(PomodoroMode.LongBreak, currentCycle = 4, longBreakEveryN = 4)
        assertEquals(PomodoroMode.Focus, mode)
        assertEquals(1, cycle)
    }
}
```

Crear también `TaskDurationCalculatorTest.kt` cubriendo:
- `includeFinalBreak = true` vs `false`
- `totalPomodoros = 0` (debe devolver 0)
- descanso largo intercalado correctamente (`longBreakEvery = 2` con 4 pomodoros)
- `calculateEndTime(null, x)` devuelve null

**Verificación:** `./gradlew testDebugUnitTest` pasa en verde.

### ✅ Paso 2. CI mínimo

Crear `.github/workflows/ci.yml`:

```yaml
name: CI
on:
  push: { branches: [main] }
  pull_request:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew assembleDebug testDebugUnitTest lint
```

**Verificación:** el workflow corre en verde en el primer push.

### ✅ Paso 3. Asegurar el esquema de Room

En `data/local/TaskDatabase.kt` hoy: `version = 7, exportSchema = false` + `fallbackToDestructiveMigration()`. No se pueden escribir migraciones 1→7 (los esquemas viejos no se exportaron), pero sí cortar la sangría hacia adelante:

1. Cambiar a `exportSchema = true`.
2. Configurar la exportación de esquema en `app/build.gradle.kts` (vía KSP arg `room.schemaLocation` — se completa en el Paso 5).
3. Commitear `app/schemas/com.example.pomodoro.data.local.TaskDatabase/7.json`.
4. **Regla de equipo desde hoy:** todo cambio de esquema (v8+) lleva `Migration(7, 8)` explícita. `fallbackToDestructiveMigration()` se elimina en el Paso 17 (primera migración real).

**Verificación:** el JSON del esquema v7 existe en git.

---

## Etapa 1 — Fundación: Application + KSP + Hilt (2-3 días)

Sin DI no se puede imponer disciplina de capas, porque cada clase "se fabrica sus dependencias" como excusa.

### ✅ Paso 4. Crear la clase Application

No existe hoy (el manifest no declara `android:name`).

1. Crear `app/src/main/java/com/example/pomodoro/PomodoroApp.kt` (vacía por ahora).
2. Registrar en `AndroidManifest.xml`: `android:name=".PomodoroApp"` en `<application>`.

**Verificación:** la app arranca normal.

### ✅ Paso 5. Migrar kapt → KSP

kapt está en modo mantenimiento y es lento con Kotlin 2.0.

1. En `gradle/libs.versions.toml`: añadir `ksp = "2.0.21-1.0.28"` y el plugin `ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }`.
2. En `app/build.gradle.kts`: reemplazar `kapt(libs.androidx.room.compiler)` por `ksp(libs.androidx.room.compiler)`, eliminar el plugin `org.jetbrains.kotlin.kapt`.
3. Configurar aquí el schema location del Paso 3:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

**Verificación:** compila; tests del Paso 1 en verde; carpeta `app/schemas/` generada.

### ✅ Paso 6. Introducir Hilt

1. Dependencias: `com.google.dagger:hilt-android` + `hilt-compiler` (por KSP) + plugin `com.google.dagger.hilt.android`. Añadir `androidx.hilt:hilt-navigation-compose` para `hiltViewModel()`.
2. `@HiltAndroidApp` en `PomodoroApp`, `@AndroidEntryPoint` en `MainActivity`.
3. Crear **un solo módulo** para empezar:

```kotlin
// core/di/DataModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TaskDatabase =
        Room.databaseBuilder(context, TaskDatabase::class.java, "task_database").build()

    @Provides @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context) = TaskAlarmScheduler(context)

    @Provides @Singleton
    fun provideTaskRepository(db: TaskDatabase, scheduler: TaskAlarmScheduler): TaskRepository =
        TaskRepository(db.taskDao, scheduler)

    @Provides @Singleton
    fun provideStatsRepository(db: TaskDatabase): StatsRepository =
        StatsRepository(db.statsDao, db.taskDao)

    @Provides @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context) = SettingsDataStore(context)

    @Provides @Singleton
    fun provideAudioSettingsDataStore(@ApplicationContext context: Context) = AudioSettingsDataStore(context)

    @Provides @Singleton
    fun provideStatsDataStore(@ApplicationContext context: Context) = StatsDataStore(context)

    @Provides @Singleton
    fun providePremiumStorage(@ApplicationContext context: Context) = PremiumStorage(context)

    @Provides @Singleton @AppScope
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Qualifier annotation class AppScope
```

4. Eliminar el singleton manual `TaskDatabase.getInstance()` — Hilt **es** ahora el singleton.

**Verificación:** compila y arranca. (Los consumidores se migran en los pasos 7-8; mientras tanto pueden coexistir.)

### ✅ Paso 7. Convertir ViewModels y limpiar NavGraph

1. `TaskViewModel`, `StatsViewModel`, `PremiumViewModel` → `@HiltViewModel` con `@Inject constructor(...)`.
2. Borrar `TaskViewModel.Factory` (líneas 380-388) y `PremiumViewModel.Factory` (líneas 74-91). El `GlobalScope` de `BillingManager` se reemplaza por el `@AppScope` del Paso 6.
3. En `NavGraph.kt`: eliminar los 4 bloques `remember { TaskDatabase.getInstance(...) / TaskRepository(...) }` (líneas 48-50, 71-73, 90-92, 140-142). Cada pantalla usa `hiltViewModel()`.
4. `PomodoroViewModel` sigue siendo `AndroidViewModel` por ahora (necesita `Application` para `bindService`); solo se le inyectan repos/datastores con `@HiltViewModel @Inject constructor(application: Application, ...)`.

**Verificación:** `grep -rn "TaskViewModel.Factory\|viewModel(factory" app/src` → cero resultados.

### ✅ Paso 8. Inyectar en Service y Receiver

1. `PomodoroForegroundService` → `@AndroidEntryPoint`; los 7 `lateinit var` de `onCreate()` pasan a `@Inject lateinit var`.
2. `TaskReceiver` → `@AndroidEntryPoint`; eliminar la reconstrucción del grafo (líneas 22-25).
3. En `TaskReceiver.onReceive`, reemplazar `CoroutineScope(Dispatchers.IO).launch` por `goAsync()`:

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // ... lógica actual ...
        } finally {
            pendingResult.finish()
        }
    }
}
```

**Verificación de la etapa:** `grep -rn "TaskDatabase.getInstance" app/src` → **cero** resultados. La app funciona igual que antes.

---

## Etapa 2 — Disciplina de capas (3-5 días)

### ✅ Paso 9. Repositorios detrás de interfaces

```kotlin
interface TaskRepository {
    fun getAllTasks(): Flow<List<TaskEntity>>
    suspend fun getTaskById(id: Int): TaskEntity?
    fun getTaskByIdFlow(id: Int): Flow<TaskEntity?>
    suspend fun insertOrUpdateTask(task: TaskEntity): Long
    suspend fun deleteTask(task: TaskEntity)
    suspend fun updateTaskStatus(taskId: Int, status: TaskStatus)
    suspend fun incrementPomodoro(taskId: Int): Boolean
    // ... resto del contrato actual
}

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmScheduler: TaskAlarmScheduler
) : TaskRepository { /* cuerpo actual sin cambios */ }
```

Bind con `@Binds` en un módulo abstracto de Hilt. Ídem `StatsRepository` → `StatsRepositoryImpl`.

**Beneficio:** fakes en tests (Paso 16) y seguro contra futuro cambio de fuente de datos.

**Verificación:** compila; ningún consumidor referencia `*Impl` directamente.

### ✅ Paso 10. `PomodoroViewModel` deja de tocar Room

Hoy: `TaskDatabase.getInstance(application)` (línea 63) y `database.taskDao.getTaskByIdFlow(id)` (línea 72).

Cambio: inyectar `TaskRepository` y usar `repository.getTaskByIdFlow(id)`. El `flatMapLatest` no cambia.

**Verificación:** `PomodoroViewModel` no importa nada de `data.local`.

### ✅ Paso 11. `TaskEditorScreen` deja de recibir el repositorio

Eliminar el parámetro `repository` de `TaskEditorScreen` (línea 42). La carga se mueve al ViewModel:

```kotlin
// En TaskViewModel
private val _editingTask = MutableStateFlow<TaskEntity?>(null)
val editingTask: StateFlow<TaskEntity?> = _editingTask.asStateFlow()

fun loadTaskForEdit(taskId: Int?) {
    if (taskId == null) { _editingTask.value = null; return }
    viewModelScope.launch { _editingTask.value = repository.getTaskById(taskId) }
}
```

La pantalla observa `editingTask` y llama `loadTaskForEdit(taskId)` en su `LaunchedEffect`. Actualizar la ruta `task_editor` en `NavGraph.kt` para dejar de pasar `repository`.

**Verificación:** ninguna función `@Composable` recibe `Repository` como parámetro.

### ✅ Paso 12. Las pantallas dejan de instanciar DataStores

`HomeScreen.kt:64` y `SettingsScreen.kt:34` hacen `remember { SettingsDataStore(context) }`.

1. `PomodoroViewModel` expone `val settings: StateFlow<Settings>` (desde `SettingsDataStore` inyectado) para HomeScreen.
2. Crear un `SettingsViewModel` pequeño (`@HiltViewModel`) con `settings: StateFlow<Settings>` + `fun updateSettings(Settings)` para SettingsScreen.

**Verificación:** `grep -rn "DataStore(context)\|DataStore(LocalContext" app/src/main/java/com/example/pomodoro/ui` → cero resultados.

### ✅ Paso 13. Resolver los use cases muertos

Los 5 archivos de `domain/usecases/` tienen 0 llamadas, y `CreateTaskUseCase` contiene validaciones (`coerceIn(1,12)`, `coerceIn(1,90)`...) que la ruta real (`TaskViewModel.createTask`, líneas 233-281) **no aplica**.

Decisión recomendada:
- **Conectar 2:**
  - `CreateTaskUseCase`: absorber el cálculo de duración/endTime que hoy vive en `TaskViewModel.createTask` y aplicar sus validaciones. `TaskViewModel.createTask` queda como delegación al use case.
  - `CompleteFocusForTaskUseCase`: lo usará el `PomodoroSessionController` (Paso 15) para la lógica de finalización.
- **Eliminar 3:** `UpdateTaskUseCase`, `MarkTaskDoneUseCase`, `StartTaskPomodoroUseCase` — son pass-through de una línea; mantenerlos es ceremonia sin valor a este tamaño. Si algún día ganan lógica, se recrean.

**Verificación:** `grep -rn "UseCase" app/src/main/java` — todo lo que existe, se usa.

### ✅ Paso 14. Renombrar el `NotificationHelper` duplicado

Hay dos clases con el mismo nombre: `notification/NotificationHelper.kt` (timer) y `util/NotificationHelper.kt` (recordatorios de tareas).

- `util/NotificationHelper` → `TaskReminderNotifier` (mover a `core/notification/` en el Paso 18).
- `notification/NotificationHelper` → `TimerNotifier` (opcional, o dejar).

**Verificación:** no existen dos clases homónimas en el proyecto.

---

## Etapa 3 — Extraer el corazón del Service (3-5 días, el paso de mayor valor)

### ✅ Paso 15. `PomodoroSessionController`

`PomodoroForegroundService` mezcla máquina de estados + ciclo de vida Android + notificaciones + audio. Extraer la máquina de estados a una clase sin Android:

```kotlin
// features/timer/domain/PomodoroSessionController.kt
@Singleton
class PomodoroSessionController @Inject constructor(
    private val engine: PomodoroEngine,
    private val statsRepository: StatsRepository,
    private val taskRepository: TaskRepository,
    private val clock: Clock,                    // interfaz propia: fun now(): Long
) {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SessionEvent>()
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    fun start(scope: CoroutineScope) { /* lógica actual de startTimer + startTick */ }
    fun pause() { /* pauseTimer sin updateNotification */ }
    fun reset() { /* resetTimer */ }
    fun nextMode() { /* nextMode */ }
    fun setActiveTask(task: TaskEntity) { /* setActiveTaskWithConfig */ }
    fun clearActiveTask() { /* clearActiveTask */ }
    suspend fun onTimerFinished() { /* lógica de onTimerFinished() líneas 330-382, intacta */ }
}

sealed interface SessionEvent {
    data class TimerFinished(val mode: PomodoroMode) : SessionEvent
    data class TaskCompleted(val title: String) : SessionEvent
}
```

El Service queda reducido a:
- Recibir Intents (`ACTION_*`) → delegar al controller.
- Observar `state` → actualizar la notificación foreground.
- Observar `events` → `showCompleteNotification` / sonidos.
- Foreground lifecycle + binder.

El audio de tarea (`MediaPlayer` crudo, líneas 194-222 del Service) se mueve a una clase `TaskAudioPlayer` propia o a `AudioPlayerManager` — el Service no debe instanciar `MediaPlayer`.

**Verificación:** el Service queda en <150 líneas y no importa `PomodoroEngine` ni repositorios.

### ✅ Paso 16. Tests del controller con tiempo virtual

Añadir `kotlinx-coroutines-test`. Con `runTest` + `advanceTimeBy` + `FakeClock` + fakes de los repositorios (habilitados por el Paso 9), testear:

- Al terminar un Focus **sin** tarea activa: registra sesión en stats y transiciona a descanso.
- Al terminar un Focus **con** tarea activa: incrementa `completedPomodoros` y actualiza la tarea.
- Al llegar `completedPomodoros == totalPomodoros`: marca DONE, emite `TaskCompleted`, limpia la tarea activa.
- `autoStartNext = true` arranca el siguiente modo automáticamente.
- Pausar y reanudar conserva el tiempo restante.

Este flujo (`onTimerFinished`) es el código más crítico de la app y hoy tiene cobertura cero.

**Verificación:** los 5 escenarios pasan en JVM sin emulador.

---

## Etapa 4 — Una sola fuente de verdad para estadísticas (2-3 días)

### ✅ Paso 17. Consolidar en Room

Hoy cada sesión escribe en dos sitios (Service líneas 336-341): `StatsDataStore` (JSON en Preferences, lo lee Home) y `StatsRepository`/Room (lo lee Stats). Además `StatsViewModel.kt:87` tiene el streak **mockeado a 0**.

1. Añadir cálculo de streak a `StatsRepository` (query sobre `DailyStatsEntity` ordenada por fecha + fold simple).
2. Migración one-shot al arrancar: si `StatsDataStore` tiene historial y Room no, importar el JSON a `DailyStatsEntity`. Esto sube la DB a v8 → **primera `Migration(7, 8)` real**. Aquí se elimina `fallbackToDestructiveMigration()` (cierra el Paso 3).
3. El controller escribe solo en `StatsRepository`; `UiState.sessionsToday/sessionsTotal` se alimenta de Room.
4. `StatsDataStore` se marca `@Deprecated` y se borra un release después.

**Verificación:** una sesión completada actualiza Home, Stats y streak desde la misma tabla; el streak real ya no es 0.

---

## Etapa 5 — Reorganización feature-first (1-2 días, solo mover archivos)

### ✅ Paso 18. Mover paquetes

Con las dependencias disciplinadas, es mecánico (refactor → move en Android Studio). Hacerlo **después** de Hilt, no antes, para no mover dos veces.

```text
com.example.pomodoro/
  core/
    di/                  # módulos Hilt
    database/            # TaskDatabase + Migrations
    notification/        # TimerNotifier, TaskReminderNotifier
    audio/               # AudioPlayerManager, TaskAudioPlayer, AudioRecorder, AudioPlayer
  features/
    timer/
      domain/            # PomodoroEngine, PomodoroSessionController, modelos del timer
      presentation/      # PomodoroViewModel, HomeScreen, TimerRing*, controles
      service/           # PomodoroForegroundService (adelgazado)
    tasks/
      data/              # TaskDao, TaskEntity, TaskRepositoryImpl, TaskAlarmScheduler, TaskReceiver
      domain/            # CreateTaskUseCase, CompleteFocusForTaskUseCase, TaskDurationCalculator
      presentation/      # TaskViewModel, TaskDashboardScreen, TaskEditorScreen, TaskDetailScreen, CalendarWeekScreen
    stats/
      data/              # StatsDao, DailyStatsEntity, StatsRepositoryImpl
      presentation/      # StatsViewModel, StatsScreen, WeeklyBarChart
    premium/
      data/              # BillingManager, PremiumStorage
      presentation/      # PremiumViewModel, PremiumUpgradeScreen
    widget/              # PomodoroWidget, PomodoroWidgetReceiver
  shared/
    ui/                  # theme/, components/ genéricos
  navigation/            # NavGraph.kt (solo rutas)
```

**No dividir en módulos Gradle todavía.** Con equipo pequeño, los paquetes feature-first dan el 80% del beneficio sin el costo de builds multi-módulo. El split real solo se justifica con 3+ devs en paralelo o builds de varios minutos.

**Verificación (opcional, en CI con Konsist o detekt):** `features/X` no importa de `features/Y`; `presentation` no importa `data.local`.

---

## Etapa 6 — Endurecimiento y producción (continuo, intercalable desde la Etapa 2)

### ✅ Paso 19. Entornos dev/prod

En `app/build.gradle.kts`:

```kotlin
buildTypes {
    debug {
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-dev"
    }
    release { /* ... */ }
}
```

Flags por entorno con `buildConfigField`. Flavors solo si algún día hay backend con URLs distintas.

### ✅ Paso 20. Minify en release

`isMinifyEnabled = true` (hoy `false` en `app/build.gradle.kts:28`) + reglas keep para Room/Billing en `proguard-rules.pro`. **Probar el APK release en dispositivo antes de publicar.**

### ✅ Paso 21. Logging estructurado

Timber en `PomodoroApp.onCreate()`. Reemplazar los `printStackTrace()` de:
- `PomodoroViewModel.importAudio` / `updateBackground`
- `TaskAlarmScheduler.schedule`
- `StatsDataStore.parseHistory`
- `AudioPlayerManager.playTrack`
- `PomodoroForegroundService.startTaskAudio`

por `Timber.e(e, "contexto")`. Donde el usuario espera resultado (importar audio), reflejar el error en el UiState en vez de tragarlo.

### ⬜ Paso 22. Billing endurecido

- Ya sin `GlobalScope` (resuelto en Paso 7 con `@AppScope`).
- Validar firma de compra localmente; documentar el riesgo aceptado en `PremiumStorage`.
- Verificación server-side de recibos cuando haya presupuesto de backend (cierra el fraude de premium).

### ⬜ Paso 23. Widget funcional

`PomodoroWidget.kt:76` muestra `--:--` fijo. El controller (Paso 15) persiste estado mínimo (modo + timestamp de fin) en un DataStore que Glance lee; llamar `PomodoroWidget().updateAll(context)` al cambiar de estado.

### ⬜ Paso 24. Limpieza de `TaskEntity` (al final)

Migración v9 que consolida `dueDateTime` / `dueDateTimeMillis` / `timestamp` / `startDateTimeMillis` en `startAt` / `dueAt`, y elimina `isCompleted` (redundante con `status`). Requiere migraciones maduras (Pasos 3/17) y tests de DAO en marcha.

---

## Orden de dependencias

```
Paso 1-2 (tests+CI) ──── sin dependencias, EMPEZAR HOY
Paso 3 (schema export) ─┐
Paso 4-5 (App+KSP) ─────┼─▶ Paso 6-8 (Hilt) ─▶ Paso 9-14 (capas) ─▶ Paso 15-16 (Service) ─▶ Paso 18 (feature-first)
                        │                                        └─▶ Paso 17 (stats, cierra el 3)
Paso 19-24 ── intercalables desde la Etapa 2 en adelante
```

## Criterios de éxito globales

- [ ] `grep -rn "TaskDatabase.getInstance" app/src` → 0 resultados (después de Etapa 1)
- [ ] Ninguna función `@Composable` recibe `Repository`/`DataStore`/`Dao` (después de Etapa 2)
- [ ] Ningún ViewModel importa `data.local.*` (después de Etapa 2)
- [ ] Todo `UseCase` que existe, se usa (después de Paso 13)
- [ ] `PomodoroForegroundService` < 150 líneas (después de Etapa 3)
- [ ] Flujo completo de sesión Pomodoro con tests en JVM (después de Paso 16)
- [ ] Una sola fuente de estadísticas; streak real, no mockeado (después de Etapa 4)
- [ ] `fallbackToDestructiveMigration()` eliminado (después de Paso 17)
- [ ] CI en verde en cada PR (desde Paso 2)

> El punto de no retorno más valioso es el **Paso 16**: cuando la máquina de estados del Pomodoro tenga tests en JVM, cualquier feature futura (sync en la nube, más modos de timer, multi-perfil) se construye sobre suelo firme en lugar de sobre un Service de 456 líneas que nadie se atreve a tocar.
