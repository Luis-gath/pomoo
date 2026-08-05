package com.example.pomodoro.features.areas.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Qué es el material. Determina cómo se abre y con qué icono se muestra. */
enum class ItemKind {
    FOTO,
    PDF,
    VIDEO,
    AUDIO,
    ENLACE,
    NOTA,

    /** Resultado traído de vuelta desde NotebookLM u otra herramienta. */
    GENERADO,

    /** Cualquier otro archivo cuyo tipo no reconocemos. */
    ARCHIVO
}

/**
 * Una sola marca por material, en vez de etiquetas libres. Es deliberado: las etiquetas
 * libres degeneran en un cajón desordenado, y aquí lo que hace falta es responder rápido a
 * "qué es importante" y "qué tengo que entregar".
 */
enum class ItemMark {
    NINGUNA,
    IMPORTANTE,
    ENTREGA
}

@Entity(
    tableName = "items",
    indices = [Index("areaId")]
)
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val areaId: Int,
    val kind: ItemKind,
    val title: String,

    /**
     * Referencia a un archivo que vive fuera de la app (Descargas, Drive, la galería).
     * Se guarda con permiso persistente. Es la estrategia para PDFs y vídeos: copiarlos
     * duplicaría gigabytes.
     */
    val uri: String? = null,

    /**
     * Copia propia dentro de `filesDir`. Solo para lo que crea la app, como las fotos de
     * cuaderno: si las hace la app, la app debe poseerlas.
     */
    val localPath: String? = null,

    /** Texto libre: el contenido si es una NOTA, o la URL si es un ENLACE. */
    val content: String = "",

    val mark: ItemMark = ItemMark.NINGUNA,

    /** Fecha de entrega cuando la marca es ENTREGA. */
    val dueAt: Long? = null,

    /** Cuándo se dio por entregada. Null mientras siga pendiente. */
    val completedAt: Long? = null,

    /** Tarea del Pomodoro con la que se relaciona, si el usuario la enlaza. */
    val taskId: Int? = null,

    val sizeBytes: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Un material referenciado puede quedar roto si el usuario borra el original. */
    val isExternalReference: Boolean get() = uri != null && localPath == null

    /** Entrega con fecha que todavía no se ha marcado como hecha. */
    val isPendingDeliverable: Boolean
        get() = mark == ItemMark.ENTREGA && dueAt != null && completedAt == null
}
