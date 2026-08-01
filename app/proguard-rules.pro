# Reglas de R8 para la build de release.
# Room, Hilt y Play Billing incluyen sus propias reglas de consumidor en el AAR,
# por lo que aquí solo van las que dependen de este proyecto.

# --- kotlinx.serialization ---
# Las clases @Serializable necesitan conservar su serializador generado.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    kotlinx.serialization.KSerializer serializer(...);
    static final kotlinx.serialization.KSerializer $childSerializers;
}

-keepclasseswithmembers class **$$serializer {
    *** INSTANCE;
}

# Modelo serializado a JSON dentro de DataStore.
-keep,includedescriptorclasses class com.example.pomodoro.core.audio.CustomTrack { *; }

# --- Entidades de Room ---
# Room accede a los constructores por reflexión al mapear filas.
-keep class com.example.pomodoro.features.tasks.data.TaskEntity { *; }
-keep class com.example.pomodoro.features.stats.data.DailyStatsEntity { *; }
-keepclassmembers enum com.example.pomodoro.features.** { *; }

# --- Componentes declarados en el manifest ---
-keep class com.example.pomodoro.features.timer.service.** { *; }
-keep class com.example.pomodoro.features.tasks.service.** { *; }
-keep class com.example.pomodoro.features.tasks.data.TaskReceiver { *; }
-keep class com.example.pomodoro.features.widget.** { *; }
