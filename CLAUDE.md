# Pomodoro — guía rápida del proyecto

App Android de temporizador Pomodoro con gestión de tareas y organización de material de
estudio. Todo funciona en local: **no hay backend ni cuentas de usuario**.

## Poner en marcha en un equipo nuevo

1. Abrir el proyecto en Android Studio. Genera `local.properties` con la ruta del SDK (está
   ignorada en git a propósito).
2. Para compilar **desde la terminal** hace falta indicar el JDK 17+, porque el `java` del
   PATH suele ser otro. En `~/.gradle/gradle.properties` (fuera del repositorio):
   ```properties
   org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr
   ```
   Ojo: barras normales. En un fichero `.properties` la barra invertida es un escape y la
   ruta queda corrupta.

## Comandos

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug   # ciclo de verificación completo
./gradlew assembleDebug                               # APK con applicationId .debug
./gradlew assembleDebug -PsameAppId                   # APK que ACTUALIZA la app instalada
```

La build de debug lleva sufijo `.debug`, así que se instala **junto a** la app real y arranca
con datos vacíos. Para probar migraciones de base de datos hay que usar `-PsameAppId`: con la
variante aislada la base nace vacía y la migración ni se ejecuta.

No hay dispositivo conectado por ADB; los APK se instalan a mano desde `apks/`.

## Estructura

Organización **por funcionalidad**, no por capa técnica:

```
core/        audio · database · di · feedback · focus · notification · share · time
features/
  timer/     domain (motor y máquina de estados) · data · presentation · service
  tasks/     data · domain (casos de uso y plantillas) · presentation
  areas/     data · domain · presentation
  stats/     data · domain · presentation
  premium/   data · presentation
  widget/
shared/ui/   theme · components genéricos
navigation/  NavGraph
```

**La regla:** cada capa solo habla con la de abajo — presentation → domain → data → core. Si
una pantalla importa Room o un repositorio, es una fuga: era el problema principal antes del
refactor.

Dependencia consciente entre features: `timer` usa `tasks` para anotar el progreso. Al revés
no puede ser, o se crea un ciclo.

## Dónde tocar qué

| Para cambiar | Ir a |
|---|---|
| Pantalla principal | `features/timer/presentation/HomeScreen.kt` |
| Cuadrante del temporizador | `features/timer/presentation/TimerGauge.kt` |
| Reglas del Pomodoro | `features/timer/domain/PomodoroEngine.kt` |
| Qué pasa al terminar un intervalo | `features/timer/domain/PomodoroSessionController.kt` |
| Notificaciones, vibración, No molestar | `core/notification/` · `core/feedback/` · `core/focus/` |
| Crear y validar tareas | `features/tasks/domain/CreateTaskUseCase.kt` |
| Plantillas de estudio y horarios | `features/tasks/domain/StudyTemplate.kt` · `WeeklyScheduleTemplate.kt` |
| Consultas a la base de datos | `core/database/` |
| Colores y tonos | `shared/ui/theme/AppTone.kt` |
| Pantallas nuevas | `navigation/NavGraph.kt` |
| Dependencias nuevas | `core/di/DataModule.kt` |

## Decisiones que conviene no romper

**Base de datos.** Va por la versión 8. A partir de la 7 **toda** subida de versión exige una
`Migration` real en `core/database/Migrations.kt`; el `fallbackToDestructiveMigrationFrom(1..6)`
solo cubre esquemas antiguos que nunca se exportaron. El SQL de una migración debe coincidir
exactamente con lo que Room genera (comparar contra `app/schemas/`): un desajuste **no falla al
compilar, revienta al abrir la app**.

**Fechas.** El `DatePicker` de Material 3 devuelve medianoche **UTC**. Combinarla con una hora
local sin convertir resta un día en zonas UTC negativas (aquí, UTC−5). La conversión vive en
`shared/ui/components/DateTimePickers.kt` y tiene tests que fijan `America/Lima`.

**Colores de la pantalla principal.** Siempre se dibuja sobre una imagen con velo oscuro, así
que usa `OnBackdrop` / `OnBackdropMuted` / `BackdropPanel`, nunca `colorScheme.onSurface`: en
tema claro este último es casi negro y el contenido desaparece.

**Archivos que llegan compartidos** traen permiso temporal y hay que **copiarlos**; los
elegidos con el selector propio admiten permiso permanente y se **referencian**. Está
documentado en `features/areas/domain/ImportItemsUseCase.kt`.

**El estado del temporizador** vive en `PomodoroSessionController`, que es `@Singleton`, no en
la pantalla ni en el Service. Por eso sobrevive a rotaciones y a cerrar la app.

**Lint** tiene dos detectores desactivados en `app/build.gradle.kts` porque revientan con
Kotlin 2.0. Es un fallo de la herramienta; revisar al subir de AGP.

## Estado

55 tests unitarios. CI en GitHub Actions (build + tests + lint).

Pendiente: verificar compras contra un servidor (hoy el premium es falsificable), conectar el
widget al estado real, limpiar los campos de fecha duplicados de `TaskEntity`, y que los
horarios semanales se repongan solos cada semana.

Contexto más amplio en `PLAN_ARQUITECTURA.md` (el refactor y por qué) y `PLAN_CLASES.md` (el
diseño de las áreas y por qué NotebookLM se integra por el menú de compartir).
