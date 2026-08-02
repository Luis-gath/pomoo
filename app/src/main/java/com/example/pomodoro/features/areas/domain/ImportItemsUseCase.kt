package com.example.pomodoro.features.areas.domain

import android.net.Uri
import com.example.pomodoro.features.areas.data.AreaRepository
import com.example.pomodoro.features.areas.data.FileImporter
import com.example.pomodoro.features.areas.data.Item
import com.example.pomodoro.features.areas.data.ItemKind
import com.example.pomodoro.features.areas.data.ItemMark
import javax.inject.Inject

/** De dónde viene el archivo: determina si hay que copiarlo o basta referenciarlo. */
enum class ImportSource {
    /** Menú "Compartir" de otra app: el permiso caduca, así que hay que copiar. */
    SHARE,

    /** Selector de la propia app: admite permiso permanente, se puede referenciar. */
    PICKER
}

data class ImportResult(
    val saved: Int,
    val failed: Int,
    val copiedBytes: Long
)

/**
 * Guarda material dentro de un área.
 *
 * La regla que decide copiar o referenciar:
 *
 * - Lo que llega **compartido** se copia siempre, porque el permiso que concede Android es
 *   temporal: si solo guardásemos el URI, el material dejaría de abrirse al poco tiempo.
 * - Lo que se elige con el **selector propio** se referencia si se logra el permiso
 *   permanente, y así un vídeo de 500 MB no duplica espacio. Si el permiso falla, se copia
 *   como último recurso.
 */
class ImportItemsUseCase @Inject constructor(
    private val fileImporter: FileImporter,
    private val repository: AreaRepository
) {

    suspend operator fun invoke(
        uris: List<Uri>,
        areaId: Int,
        source: ImportSource,
        mark: ItemMark = ItemMark.NINGUNA,
        dueAt: Long? = null
    ): ImportResult {
        var saved = 0
        var failed = 0
        var copiedBytes = 0L

        for (uri in uris) {
            val info = fileImporter.describe(uri)
            val kind = ItemKindResolver.resolve(info.mimeType, info.displayName)
            val title = ItemKindResolver.defaultTitle(info.displayName, "Material")

            val canReference = source == ImportSource.PICKER && fileImporter.tryPersist(uri)

            val localPath = if (canReference) {
                null
            } else {
                fileImporter.copyIntoArea(uri, areaId, info.displayName ?: "archivo")
                    ?.also { copiedBytes += info.sizeBytes ?: 0L }
            }

            if (!canReference && localPath == null) {
                failed++
                continue
            }

            repository.saveItem(
                Item(
                    areaId = areaId,
                    kind = kind,
                    title = title,
                    uri = if (canReference) uri.toString() else null,
                    localPath = localPath,
                    mark = mark,
                    dueAt = dueAt,
                    sizeBytes = info.sizeBytes
                )
            )
            saved++
        }

        return ImportResult(saved = saved, failed = failed, copiedBytes = copiedBytes)
    }

    suspend fun saveLink(
        url: String,
        areaId: Int,
        title: String? = null,
        mark: ItemMark = ItemMark.NINGUNA
    ): Long = repository.saveItem(
        Item(
            areaId = areaId,
            kind = ItemKind.ENLACE,
            title = title?.takeIf { it.isNotBlank() } ?: Uri.parse(url).host ?: "Enlace",
            content = url.trim(),
            mark = mark
        )
    )

    suspend fun saveNote(
        text: String,
        areaId: Int,
        title: String? = null,
        mark: ItemMark = ItemMark.NINGUNA
    ): Long = repository.saveItem(
        Item(
            areaId = areaId,
            kind = ItemKind.NOTA,
            title = title?.takeIf { it.isNotBlank() } ?: text.take(40).trim(),
            content = text.trim(),
            mark = mark
        )
    )
}
