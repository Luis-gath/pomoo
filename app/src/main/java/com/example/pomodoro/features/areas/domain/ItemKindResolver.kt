package com.example.pomodoro.features.areas.domain

import com.example.pomodoro.features.areas.data.ItemKind

/**
 * Deduce qué clase de material es a partir del tipo MIME y del nombre del archivo.
 *
 * Es lógica pura y sin Android a propósito, para poder testearla: el tipo MIME que llega
 * al compartir es poco de fiar (muchas apps mandan `application/octet-stream` o
 * directamente nada), así que la extensión del nombre actúa de red de seguridad.
 */
object ItemKindResolver {

    private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp")
    private val VIDEO_EXT = setOf("mp4", "mkv", "webm", "avi", "mov", "3gp")
    private val AUDIO_EXT = setOf("mp3", "m4a", "aac", "ogg", "wav", "opus", "flac")

    fun resolve(mimeType: String?, fileName: String?): ItemKind {
        val mime = mimeType?.lowercase()?.substringBefore(';')?.trim()

        when {
            mime == "application/pdf" -> return ItemKind.PDF
            mime != null && mime.startsWith("image/") -> return ItemKind.FOTO
            mime != null && mime.startsWith("video/") -> return ItemKind.VIDEO
            mime != null && mime.startsWith("audio/") -> return ItemKind.AUDIO
        }

        // El MIME no dice nada útil: se recurre a la extensión.
        val ext = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return when (ext) {
            "pdf" -> ItemKind.PDF
            in IMAGE_EXT -> ItemKind.FOTO
            in VIDEO_EXT -> ItemKind.VIDEO
            in AUDIO_EXT -> ItemKind.AUDIO
            else -> ItemKind.ARCHIVO
        }
    }

    /** Un texto compartido que parece una dirección web se guarda como enlace. */
    fun isLink(text: String): Boolean {
        val t = text.trim()
        return (t.startsWith("http://") || t.startsWith("https://")) && !t.contains(' ')
    }

    /** Título legible por defecto: el nombre sin extensión, o el dominio si es un enlace. */
    fun defaultTitle(fileName: String?, fallback: String): String {
        val name = fileName?.substringBeforeLast('.', fileName)?.trim()
        return if (name.isNullOrBlank()) fallback else name
    }
}
