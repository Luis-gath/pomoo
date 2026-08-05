# Entregas con fecha y avisos — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que marcar un material como entrega permita elegir fecha y hora, avise con antelación escalonada, y exista una pantalla que cruce las entregas pendientes de todas las áreas.

**Architecture:** La lógica de cuándo avisar es un objeto puro y testeable (`DeliverableReminders`); Android solo la ejecuta. El programador de alarmas y el receptor replican el patrón que ya usan las tareas (`TaskAlarmScheduler` / `TaskReceiver`), pero con espacio de códigos propio para no colisionar. Se añade `completedAt` a `Item` para poder archivar una entrega en vez de borrarla.

**Tech Stack:** Kotlin, Jetpack Compose, Room 2.6.1, Hilt 2.52, AlarmManager, JUnit 4.

## Global Constraints

- `minSdk = 26`, `targetSdk = 34`, `compileSdk = 34`.
- `namespace = "com.example.pomodoro"`, `applicationId = "com.luis.pomodoro"`.
- Room exporta esquema a `app/schemas/`. **Toda subida de versión exige una `Migration` explícita**: `fallbackToDestructiveMigrationFrom` solo cubre las versiones 1 a 6.
- Arquitectura feature-first: `features/<nombre>/{data,domain,presentation}`. Lógica pura en `domain`, sin dependencias de Android, para que sea testeable.
- Repositorios se declaran como interfaz en `data/` y se enlazan en `core/di/RepositoryModule.kt`.
- Todo el texto visible al usuario va en español.
- Los comentarios de código explican el porqué, no el qué. En español, como el resto del proyecto.

---

### Task 1: Lógica pura de recordatorios

Calcula en qué instantes hay que avisar de una entrega. Sin Android, para poder testearla.

**Files:**
- Create: `app/src/main/java/com/example/pomodoro/features/areas/domain/DeliverableReminders.kt`
- Test: `app/src/test/java/com/example/pomodoro/features/areas/domain/DeliverableRemindersTest.kt`

**Interfaces:**
- Consumes: nada.
- Produces:
  - `data class ReminderInstant(val offsetIndex: Int, val triggerAt: Long)`
  - `DeliverableReminders.OFFSETS_MILLIS: List<Long>`
  - `DeliverableReminders.instantsFor(dueAt: Long, now: Long): List<ReminderInstant>`
  - `DeliverableReminders.requestCode(itemId: Int, offsetIndex: Int): Int`

- [ ] **Step 1: Escribir el test que falla**

Crear `app/src/test/java/com/example/pomodoro/features/areas/domain/DeliverableRemindersTest.kt`:

```kotlin
package com.example.pomodoro.features.areas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliverableRemindersTest {

    private val dia = 24L * 60 * 60 * 1000
    private val ahora = 1_000_000_000_000L

    @Test
    fun `una entrega lejana genera los tres avisos`() {
        val instantes = DeliverableReminders.instantsFor(dueAt = ahora + 30 * dia, now = ahora)

        assertEquals(3, instantes.size)
        assertEquals(listOf(0, 1, 2), instantes.map { it.offsetIndex })
    }

    @Test
    fun `los avisos van en orden cronologico y terminan en la fecha de entrega`() {
        val entrega = ahora + 30 * dia
        val instantes = DeliverableReminders.instantsFor(dueAt = entrega, now = ahora)

        assertEquals(instantes.sortedBy { it.triggerAt }, instantes)
        assertEquals(entrega, instantes.last().triggerAt)
    }

    @Test
    fun `si falta menos de una semana se omite el aviso semanal`() {
        val instantes = DeliverableReminders.instantsFor(dueAt = ahora + 2 * dia, now = ahora)

        assertEquals(2, instantes.size)
        assertTrue(instantes.none { it.offsetIndex == 0 })
    }

    @Test
    fun `si falta menos de un dia solo queda el aviso de la propia entrega`() {
        val instantes = DeliverableReminders.instantsFor(dueAt = ahora + 3 * 60 * 60 * 1000, now = ahora)

        assertEquals(1, instantes.size)
        assertEquals(2, instantes.single().offsetIndex)
    }

    @Test
    fun `una entrega ya pasada no genera avisos`() {
        val instantes = DeliverableReminders.instantsFor(dueAt = ahora - dia, now = ahora)

        assertTrue(instantes.isEmpty())
    }

    @Test
    fun `cada aviso de cada material tiene un codigo distinto`() {
        val codigos = listOf(
            DeliverableReminders.requestCode(itemId = 1, offsetIndex = 0),
            DeliverableReminders.requestCode(itemId = 1, offsetIndex = 1),
            DeliverableReminders.requestCode(itemId = 2, offsetIndex = 0)
        )

        assertEquals(codigos.size, codigos.toSet().size)
    }

    @Test
    fun `los codigos no invaden el rango que usan las tareas`() {
        // TaskReminderNotifier usa taskId, taskId+10000 y taskId+20000.
        val codigo = DeliverableReminders.requestCode(itemId = 0, offsetIndex = 0)

        assertTrue(codigo > 20_000 + 100_000)
    }
}
```

- [ ] **Step 2: Ejecutar el test y comprobar que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.pomodoro.features.areas.domain.DeliverableRemindersTest"`
Expected: FAIL con `Unresolved reference: DeliverableReminders`

- [ ] **Step 3: Escribir la implementación mínima**

Crear `app/src/main/java/com/example/pomodoro/features/areas/domain/DeliverableReminders.kt`:

```kotlin
package com.example.pomodoro.features.areas.domain

/** Un aviso concreto: cuál de los adelantos es y en qué instante se dispara. */
data class ReminderInstant(
    val offsetIndex: Int,
    val triggerAt: Long
)

/**
 * Cuándo avisar de una entrega.
 *
 * Se avisa varias veces porque un único aviso el mismo día llega tarde para un trabajo
 * largo. Es lógica pura y sin Android a propósito: así se puede comprobar con tests en
 * lugar de tener que instalar la app y esperar a que salte una alarma.
 */
object DeliverableReminders {

    /** Adelantos respecto a la fecha de entrega, del más lejano al más próximo. */
    val OFFSETS_MILLIS: List<Long> = listOf(
        7L * 24 * 60 * 60 * 1000,
        24L * 60 * 60 * 1000,
        0L
    )

    /**
     * Base del espacio de códigos de las alarmas de entregas.
     *
     * Las tareas ya ocupan `taskId`, `taskId + 10000` y `taskId + 20000` en
     * TaskReminderNotifier. Sin una base separada, la alarma de un material podría
     * sobrescribir la de una tarea.
     */
    private const val REQUEST_BASE = 500_000

    /** Avisos que todavía tienen sentido: los que ya pasaron se descartan. */
    fun instantsFor(dueAt: Long, now: Long): List<ReminderInstant> =
        OFFSETS_MILLIS
            .mapIndexed { index, offset -> ReminderInstant(index, dueAt - offset) }
            .filter { it.triggerAt > now }

    fun requestCode(itemId: Int, offsetIndex: Int): Int =
        REQUEST_BASE + itemId * OFFSETS_MILLIS.size + offsetIndex
}
```

- [ ] **Step 4: Ejecutar el test y comprobar que pasa**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.pomodoro.features.areas.domain.DeliverableRemindersTest"`
Expected: PASS, 7 tests

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/pomodoro/features/areas/domain/DeliverableReminders.kt app/src/test/java/com/example/pomodoro/features/areas/domain/DeliverableRemindersTest.kt
git commit -m "feat(areas): logica de avisos escalonados para entregas"
```

---

### Task 2: Campo `completedAt` y migración a la versión 10

Una entrega hecha debe archivarse, no borrarse: el material sigue sirviendo para estudiar.

**Files:**
- Modify: `app/src/main/java/com/example/pomodoro/features/areas/data/Item.kt`
- Modify: `app/src/main/java/com/example/pomodoro/core/database/Migrations.kt`
- Modify: `app/src/main/java/com/example/pomodoro/core/database/TaskDatabase.kt:24`
- Modify: `app/src/main/java/com/example/pomodoro/core/di/DataModule.kt:55`
- Modify: `app/src/main/java/com/example/pomodoro/core/database/ItemDao.kt`
- Modify: `app/src/main/java/com/example/pomodoro/features/areas/data/AreaRepository.kt`
- Modify: `app/src/main/java/com/example/pomodoro/features/areas/data/AreaRepositoryImpl.kt`

**Interfaces:**
- Consumes: nada de Task 1.
- Produces:
  - `Item.completedAt: Long?`
  - `Item.isPendingDeliverable: Boolean`
  - `ItemDao.getPendingDeliverables(): Flow<List<Item>>`
  - `ItemDao.setCompletedAt(id: Int, completedAt: Long?)`
  - `AreaRepository.getPendingDeliverables(): Flow<List<Item>>`
  - `AreaRepository.setDeliverableCompleted(id: Int, completedAt: Long?)`
  - `val MIGRATION_9_10: Migration`

- [ ] **Step 1: Añadir el campo a la entidad**

En `Item.kt`, tras `val dueAt: Long? = null,` añadir:

```kotlin
    /** Cuándo se dio por entregada. Null mientras siga pendiente. */
    val completedAt: Long? = null,
```

Y dentro del cuerpo de la clase, junto a `isExternalReference`, añadir:

```kotlin
    /** Entrega con fecha que todavía no se ha marcado como hecha. */
    val isPendingDeliverable: Boolean
        get() = mark == ItemMark.ENTREGA && dueAt != null && completedAt == null
```

- [ ] **Step 2: Escribir la migración**

En `Migrations.kt`, al final del archivo:

```kotlin
/**
 * Permite archivar una entrega en vez de borrarla.
 *
 * Se añade anulable y sin valor por defecto: las entregas que ya existan quedan como
 * pendientes, que es justo lo que eran antes de este cambio.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `items` ADD COLUMN `completedAt` INTEGER")
    }
}
```

- [ ] **Step 3: Subir la versión y registrar la migración**

En `TaskDatabase.kt` cambiar `version = 9,` por `version = 10,`.

En `DataModule.kt`, importar `MIGRATION_9_10` junto a las otras migraciones y cambiar:

```kotlin
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
```

- [ ] **Step 4: Añadir las consultas al DAO**

En `ItemDao.kt`, sustituir el método `getUpcomingDeliverables` por:

```kotlin
    /** Entregas pendientes de todas las áreas, para una vista general de lo urgente. */
    @Query("""
        SELECT * FROM items
        WHERE mark = 'ENTREGA' AND dueAt IS NOT NULL
        ORDER BY dueAt ASC
    """)
    fun getUpcomingDeliverables(): Flow<List<Item>>

    /** Solo lo que sigue sin entregar: es lo que alimenta los avisos y la vista de urgencias. */
    @Query("""
        SELECT * FROM items
        WHERE mark = 'ENTREGA' AND dueAt IS NOT NULL AND completedAt IS NULL
        ORDER BY dueAt ASC
    """)
    fun getPendingDeliverables(): Flow<List<Item>>

    @Query("UPDATE items SET completedAt = :completedAt WHERE id = :id")
    suspend fun setCompletedAt(id: Int, completedAt: Long?)
```

- [ ] **Step 5: Exponerlas en el repositorio**

En `AreaRepository.kt`, tras `fun getUpcomingDeliverables(): Flow<List<Item>>` añadir:

```kotlin
    /** Entregas con fecha que aún no se han marcado como hechas. */
    fun getPendingDeliverables(): Flow<List<Item>>

    /** Marca o desmarca una entrega como hecha. `null` la devuelve a pendiente. */
    suspend fun setDeliverableCompleted(id: Int, completedAt: Long?)
```

En `AreaRepositoryImpl.kt`, junto a las demás implementaciones:

```kotlin
    override fun getPendingDeliverables(): Flow<List<Item>> = itemDao.getPendingDeliverables()

    override suspend fun setDeliverableCompleted(id: Int, completedAt: Long?) =
        itemDao.setCompletedAt(id, completedAt)
```

- [ ] **Step 6: Compilar y comprobar que Room acepta el esquema**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL, y aparece `app/schemas/com.example.pomodoro.core.database.TaskDatabase/10.json`

Si Room protesta con "Migration didn't properly handle", el SQL no coincide con la entidad: comparar contra el JSON del esquema.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/pomodoro/features/areas/data/Item.kt app/src/main/java/com/example/pomodoro/core/database/ app/src/main/java/com/example/pomodoro/core/di/DataModule.kt app/src/main/java/com/example/pomodoro/features/areas/data/AreaRepository.kt app/src/main/java/com/example/pomodoro/features/areas/data/AreaRepositoryImpl.kt app/schemas/
git commit -m "feat(areas): completedAt para archivar entregas, migracion 9-10"
```

---

### Task 3: Programar y notificar los avisos

**Files:**
- Create: `app/src/main/java/com/example/pomodoro/features/areas/data/DeliverableAlarmScheduler.kt`
- Create: `app/src/main/java/com/example/pomodoro/features/areas/data/DeliverableReceiver.kt`
- Create: `app/src/main/java/com/example/pomodoro/core/notification/DeliverableNotifier.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/example/pomodoro/core/di/DataModule.kt`

**Interfaces:**
- Consumes: `DeliverableReminders.instantsFor`, `DeliverableReminders.requestCode`, `Item.completedAt`, `AreaRepository.setDeliverableCompleted`.
- Produces:
  - `DeliverableAlarmScheduler.schedule(item: Item)`
  - `DeliverableAlarmScheduler.cancel(itemId: Int)`
  - `DeliverableNotifier.show(item: Item, areaName: String)`

- [ ] **Step 1: Crear el notificador**

Crear `app/src/main/java/com/example/pomodoro/core/notification/DeliverableNotifier.kt`:

```kotlin
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
        private const val NOTIFICATION_BASE = 500_000
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
            NOTIFICATION_BASE + 100_000 + item.id,
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
```

- [ ] **Step 2: Crear el programador de alarmas**

Crear `app/src/main/java/com/example/pomodoro/features/areas/data/DeliverableAlarmScheduler.kt`:

```kotlin
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
```

- [ ] **Step 3: Crear el receptor**

Crear `app/src/main/java/com/example/pomodoro/features/areas/data/DeliverableReceiver.kt`:

```kotlin
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
```

- [ ] **Step 4: Declarar el receptor en el manifiesto**

En `AndroidManifest.xml`, junto a la declaración de `TaskReceiver`, añadir:

```xml
        <receiver
            android:name=".features.areas.data.DeliverableReceiver"
            android:enabled="true"
            android:exported="false" />
```

- [ ] **Step 5: Registrar las dependencias**

En `DataModule.kt`, dentro de la sección de infraestructura Android:

```kotlin
    @Provides
    @Singleton
    fun provideDeliverableAlarmScheduler(@ApplicationContext context: Context) =
        DeliverableAlarmScheduler(context)

    @Provides
    @Singleton
    fun provideDeliverableNotifier(@ApplicationContext context: Context) =
        DeliverableNotifier(context)
```

Añadir los imports de `com.example.pomodoro.features.areas.data.DeliverableAlarmScheduler` y `com.example.pomodoro.core.notification.DeliverableNotifier`.

- [ ] **Step 6: Compilar**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/pomodoro/features/areas/data/DeliverableAlarmScheduler.kt app/src/main/java/com/example/pomodoro/features/areas/data/DeliverableReceiver.kt app/src/main/java/com/example/pomodoro/core/notification/DeliverableNotifier.kt app/src/main/AndroidManifest.xml app/src/main/java/com/example/pomodoro/core/di/DataModule.kt
git commit -m "feat(areas): alarmas y notificaciones de entregas"
```

---

### Task 4: Elegir fecha al marcar una entrega

Hoy `cycleMark` reenvía el `dueAt` que ya había, así que nunca se puede fijar una fecha.

**Files:**
- Modify: `app/src/main/java/com/example/pomodoro/features/areas/presentation/AreaDetailViewModel.kt`
- Modify: `app/src/main/java/com/example/pomodoro/features/areas/presentation/AreaDetailScreen.kt:219`

**Interfaces:**
- Consumes: `DeliverableAlarmScheduler.schedule/cancel`, `AreaRepository.setDeliverableCompleted`, `PomodoroDatePickerDialog`, `PomodoroTimePickerDialog`, `PickerDateUtils.combineDateAndTime`.
- Produces:
  - `AreaDetailViewModel.pendingDueDateFor: StateFlow<Item?>`
  - `AreaDetailViewModel.requestMarkChange(item: Item)`
  - `AreaDetailViewModel.confirmDueDate(item: Item, dueAt: Long)`
  - `AreaDetailViewModel.dismissDueDatePicker()`
  - `AreaDetailViewModel.toggleCompleted(item: Item)`

- [ ] **Step 1: Sustituir `cycleMark` en el ViewModel**

En `AreaDetailViewModel.kt`, añadir al constructor `private val alarmScheduler: DeliverableAlarmScheduler` y los imports correspondientes. Reemplazar el método `cycleMark` por:

```kotlin
    private val _pendingDueDateFor = MutableStateFlow<Item?>(null)
    /** Material a la espera de que el usuario elija fecha de entrega. */
    val pendingDueDateFor: StateFlow<Item?> = _pendingDueDateFor.asStateFlow()

    /**
     * Avanza la marca. Al llegar a ENTREGA se pide fecha en lugar de aplicarla en el acto:
     * una entrega sin fecha no puede avisar de nada, que era el fallo anterior.
     */
    fun requestMarkChange(item: Item) {
        when (item.mark) {
            ItemMark.NINGUNA -> setMark(item, ItemMark.IMPORTANTE, null)
            ItemMark.IMPORTANTE -> _pendingDueDateFor.value = item
            ItemMark.ENTREGA -> setMark(item, ItemMark.NINGUNA, null)
        }
    }

    fun confirmDueDate(item: Item, dueAt: Long) {
        _pendingDueDateFor.value = null
        setMark(item, ItemMark.ENTREGA, dueAt)
    }

    fun dismissDueDatePicker() { _pendingDueDateFor.value = null }

    private fun setMark(item: Item, mark: ItemMark, dueAt: Long?) {
        viewModelScope.launch {
            repository.updateMark(item.id, mark, dueAt)
            val updated = item.copy(mark = mark, dueAt = dueAt, completedAt = null)
            if (mark == ItemMark.ENTREGA && dueAt != null) {
                repository.setDeliverableCompleted(item.id, null)
                alarmScheduler.schedule(updated)
                _message.value = "Entrega programada"
            } else {
                alarmScheduler.cancel(item.id)
            }
        }
    }

    /** Marca la entrega como hecha, o la devuelve a pendiente. */
    fun toggleCompleted(item: Item) {
        viewModelScope.launch {
            val done = item.completedAt == null
            repository.setDeliverableCompleted(item.id, if (done) System.currentTimeMillis() else null)
            if (done) {
                alarmScheduler.cancel(item.id)
                _message.value = "Entrega completada"
            } else {
                alarmScheduler.schedule(item.copy(completedAt = null))
            }
        }
    }
```

En `deleteItem`, antes de borrar, añadir `alarmScheduler.cancel(item.id)` para no dejar alarmas huérfanas.

- [ ] **Step 2: Mostrar los selectores en la pantalla**

En `AreaDetailScreen.kt`, cambiar `onToggleMark = { viewModel.cycleMark(item) }` por `onToggleMark = { viewModel.requestMarkChange(item) }`.

Dentro del `Scaffold`, tras el `LazyColumn`, añadir:

```kotlin
    val pendingDueDate by viewModel.pendingDueDateFor.collectAsState()
    var chosenDateUtc by remember { mutableStateOf<Long?>(null) }

    pendingDueDate?.let { item ->
        val base = item.dueAt ?: System.currentTimeMillis()
        if (chosenDateUtc == null) {
            PomodoroDatePickerDialog(
                initialDateMillis = base,
                onDismiss = { viewModel.dismissDueDatePicker() },
                onConfirm = { utcDateMillis -> chosenDateUtc = utcDateMillis },
                confirmText = "Siguiente"
            )
        } else {
            PomodoroTimePickerDialog(
                initialHour = PickerDateUtils.hourOf(base),
                initialMinute = PickerDateUtils.minuteOf(base),
                onDismiss = {
                    chosenDateUtc = null
                    viewModel.dismissDueDatePicker()
                },
                onConfirm = { hour, minute ->
                    val dueAt = PickerDateUtils.combineDateAndTime(chosenDateUtc!!, hour, minute)
                    chosenDateUtc = null
                    viewModel.confirmDueDate(item, dueAt)
                }
            )
        }
    }
```

Añadir estos imports:

```kotlin
import com.example.pomodoro.shared.ui.components.PickerDateUtils
import com.example.pomodoro.shared.ui.components.PomodoroDatePickerDialog
import com.example.pomodoro.shared.ui.components.PomodoroTimePickerDialog
```

Las firmas reales, ya verificadas en `DateTimePickers.kt`, son:

```kotlin
fun PomodoroDatePickerDialog(
    initialDateMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (utcDateMillis: Long) -> Unit,
    confirmText: String = "Aceptar"
)

fun PomodoroTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
)
```

El selector de fecha devuelve milisegundos **en UTC** (así los entrega Material 3), por eso hay que
combinarlo con la hora mediante `PickerDateUtils.combineDateAndTime` en lugar de sumarlos a mano.

- [ ] **Step 3: Compilar**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Probar en el dispositivo**

```bash
./gradlew :app:assembleDebug -PsameAppId
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Comprobar: entrar en un área, tocar la marca de un material dos veces hasta llegar a entrega, elegir fecha y hora, y ver el aviso «Entrega programada».

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/pomodoro/features/areas/presentation/AreaDetailViewModel.kt app/src/main/java/com/example/pomodoro/features/areas/presentation/AreaDetailScreen.kt
git commit -m "feat(areas): elegir fecha y hora al marcar una entrega"
```

---

### Task 5: Pantalla «Próximas entregas»

Responde la pregunta que hoy no tiene respuesta: *¿qué tengo que entregar esta semana?*

**Files:**
- Create: `app/src/main/java/com/example/pomodoro/features/areas/presentation/UpcomingDeliverablesViewModel.kt`
- Create: `app/src/main/java/com/example/pomodoro/features/areas/presentation/UpcomingDeliverablesScreen.kt`
- Modify: `app/src/main/java/com/example/pomodoro/navigation/NavGraph.kt`
- Modify: `app/src/main/java/com/example/pomodoro/features/areas/presentation/AreasScreen.kt`
- Test: `app/src/test/java/com/example/pomodoro/features/areas/domain/DeliverableGroupingTest.kt`
- Create: `app/src/main/java/com/example/pomodoro/features/areas/domain/DeliverableGrouping.kt`

**Interfaces:**
- Consumes: `AreaRepository.getPendingDeliverables`, `AreaRepository.getAllAreas`, `AreaDetailViewModel.toggleCompleted` (equivalente propio).
- Produces:
  - `enum class DueBucket { VENCIDA, HOY, ESTA_SEMANA, MAS_ADELANTE }`
  - `DeliverableGrouping.bucketOf(dueAt: Long, now: Long): DueBucket`
  - `NavGraph.UPCOMING = "upcoming_deliverables"`

- [ ] **Step 1: Escribir el test de agrupación**

Crear `app/src/test/java/com/example/pomodoro/features/areas/domain/DeliverableGroupingTest.kt`:

```kotlin
package com.example.pomodoro.features.areas.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DeliverableGroupingTest {

    private val hora = 60L * 60 * 1000
    private val dia = 24 * hora
    private val ahora = 1_000_000_000_000L

    @Test
    fun `una fecha pasada esta vencida`() {
        assertEquals(DueBucket.VENCIDA, DeliverableGrouping.bucketOf(ahora - hora, ahora))
    }

    @Test
    fun `dentro de las proximas horas es hoy`() {
        assertEquals(DueBucket.HOY, DeliverableGrouping.bucketOf(ahora + 2 * hora, ahora))
    }

    @Test
    fun `dentro de tres dias es esta semana`() {
        assertEquals(DueBucket.ESTA_SEMANA, DeliverableGrouping.bucketOf(ahora + 3 * dia, ahora))
    }

    @Test
    fun `dentro de un mes es mas adelante`() {
        assertEquals(DueBucket.MAS_ADELANTE, DeliverableGrouping.bucketOf(ahora + 30 * dia, ahora))
    }
}
```

- [ ] **Step 2: Ejecutar el test y comprobar que falla**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.pomodoro.features.areas.domain.DeliverableGroupingTest"`
Expected: FAIL con `Unresolved reference: DeliverableGrouping`

- [ ] **Step 3: Implementar la agrupación**

Crear `app/src/main/java/com/example/pomodoro/features/areas/domain/DeliverableGrouping.kt`:

```kotlin
package com.example.pomodoro.features.areas.domain

/** Franja de urgencia de una entrega. El orden del enum es el orden de la pantalla. */
enum class DueBucket { VENCIDA, HOY, ESTA_SEMANA, MAS_ADELANTE }

/**
 * Reparte las entregas en franjas de urgencia.
 *
 * Se agrupa por cercanía y no por fecha exacta porque lo que el usuario necesita saber de
 * un vistazo es qué le aprieta, no el calendario completo.
 */
object DeliverableGrouping {

    private const val DIA = 24L * 60 * 60 * 1000

    fun bucketOf(dueAt: Long, now: Long): DueBucket = when {
        dueAt < now -> DueBucket.VENCIDA
        dueAt < now + DIA -> DueBucket.HOY
        dueAt < now + 7 * DIA -> DueBucket.ESTA_SEMANA
        else -> DueBucket.MAS_ADELANTE
    }
}
```

- [ ] **Step 4: Ejecutar el test y comprobar que pasa**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.pomodoro.features.areas.domain.DeliverableGroupingTest"`
Expected: PASS, 4 tests

- [ ] **Step 5: Crear el ViewModel**

Crear `app/src/main/java/com/example/pomodoro/features/areas/presentation/UpcomingDeliverablesViewModel.kt`:

```kotlin
package com.example.pomodoro.features.areas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.features.areas.data.AreaRepository
import com.example.pomodoro.features.areas.data.DeliverableAlarmScheduler
import com.example.pomodoro.features.areas.data.Item
import com.example.pomodoro.features.areas.domain.DeliverableGrouping
import com.example.pomodoro.features.areas.domain.DueBucket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Una entrega con el nombre de su área, para no consultarla por cada fila. */
data class DeliverableRow(
    val item: Item,
    val areaName: String,
    val bucket: DueBucket
)

@HiltViewModel
class UpcomingDeliverablesViewModel @Inject constructor(
    private val repository: AreaRepository,
    private val alarmScheduler: DeliverableAlarmScheduler
) : ViewModel() {

    val rows: StateFlow<List<DeliverableRow>> = combine(
        repository.getPendingDeliverables(),
        repository.getAllAreas()
    ) { items, areas ->
        val names = areas.associate { it.id to it.name }
        val now = System.currentTimeMillis()
        items.mapNotNull { item ->
            val dueAt = item.dueAt ?: return@mapNotNull null
            DeliverableRow(
                item = item,
                areaName = names[item.areaId].orEmpty(),
                bucket = DeliverableGrouping.bucketOf(dueAt, now)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markDone(item: Item) {
        viewModelScope.launch {
            repository.setDeliverableCompleted(item.id, System.currentTimeMillis())
            alarmScheduler.cancel(item.id)
        }
    }
}
```

- [ ] **Step 6: Crear la pantalla**

Crear `app/src/main/java/com/example/pomodoro/features/areas/presentation/UpcomingDeliverablesScreen.kt`:

```kotlin
package com.example.pomodoro.features.areas.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pomodoro.features.areas.domain.DueBucket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingDeliverablesScreen(
    onBack: () -> Unit,
    onAreaClick: (Int) -> Unit,
    viewModel: UpcomingDeliverablesViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Próximas entregas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tienes entregas pendientes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DueBucket.entries.forEach { bucket ->
                val ofBucket = rows.filter { it.bucket == bucket }
                if (ofBucket.isEmpty()) return@forEach

                item(key = "header_$bucket") {
                    Text(
                        text = bucket.label(),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (bucket == DueBucket.VENCIDA) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                items(ofBucket, key = { it.item.id }) { row ->
                    ElevatedCard(onClick = { onAreaClick(row.item.areaId) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.item.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${row.areaName} · ${formatDue(row.item.dueAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { viewModel.markDone(row.item) }) {
                                Text("Entregado")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DueBucket.label(): String = when (this) {
    DueBucket.VENCIDA -> "Vencidas"
    DueBucket.HOY -> "Hoy"
    DueBucket.ESTA_SEMANA -> "Esta semana"
    DueBucket.MAS_ADELANTE -> "Más adelante"
}

private fun formatDue(millis: Long?): String {
    if (millis == null) return ""
    return SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()).format(Date(millis))
}
```

- [ ] **Step 7: Añadir la ruta y el acceso**

En `NavGraph.kt`, dentro del objeto `NavGraph`, añadir:

```kotlin
    const val UPCOMING = "upcoming_deliverables"
```

Y como destino, junto a los demás `composable`:

```kotlin
        composable(NavGraph.UPCOMING) {
            UpcomingDeliverablesScreen(
                onBack = { navController.popBackStack() },
                onAreaClick = { areaId -> navController.navigate("area_detail/$areaId") }
            )
        }
```

Importar `com.example.pomodoro.features.areas.presentation.UpcomingDeliverablesScreen`.

En `AreasScreen.kt`, añadir un parámetro `onUpcomingClick: () -> Unit` y un `IconButton` en las acciones del `TopAppBar` que lo invoque, con `contentDescription = "Próximas entregas"`. En `NavGraph.kt`, en el destino `NavGraph.AREAS`, pasar `onUpcomingClick = { navController.navigate(NavGraph.UPCOMING) }`.

- [ ] **Step 8: Compilar y pasar toda la batería de tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, sin tests fallidos

- [ ] **Step 9: Probar en el dispositivo**

```bash
./gradlew :app:assembleDebug -PsameAppId
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Comprobar: crear dos entregas con fechas distintas (una pasada y una de dentro de tres días), abrir «Próximas entregas» y ver que aparecen bajo «Vencidas» y «Esta semana». Pulsar «Entregado» y comprobar que desaparece de la lista.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/example/pomodoro/features/areas/ app/src/main/java/com/example/pomodoro/navigation/NavGraph.kt app/src/test/java/com/example/pomodoro/features/areas/
git commit -m "feat(areas): pantalla de proximas entregas agrupadas por urgencia"
```

---

## Fuera de alcance de este plan

Cada uno necesita su propio plan, y ninguno bloquea a los demás:

- **Cámara dentro del área** con modo varias páginas y OCR con ML Kit.
- **Visor a pantalla completa** con el temporizador visible.
- **Tarjetas y repaso espaciado** (`StudyCard`, SM-2).
- **Generación de preguntas**: analizador de texto compartido primero, modelo en servidor después, condicionado a la decisión del plan Blaze.

## Riesgo conocido

Android 14+ restringe `SCHEDULE_EXACT_ALARM`. El programador ya degrada a alarma inexacta si falta el permiso, así que el aviso llega igual pero puede retrasarse. Si en pruebas se observan retrasos grandes, hay que añadir una pantalla que pida el permiso, igual que se hace con el acceso a No molestar.
