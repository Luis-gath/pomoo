package com.example.pomodoro.features.areas.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "FileImporter"

/** Datos de un archivo antes de guardarlo como material. */
data class IncomingFile(
    val uri: Uri,
    val displayName: String?,
    val mimeType: String?,
    val sizeBytes: Long?
)

/**
 * Trae archivos de fuera hacia un área.
 *
 * Hay una diferencia de Android que condiciona todo el diseño:
 *
 * - Los archivos que llegan **compartidos** (`ACTION_SEND`) traen un permiso temporal que
 *   caduca al cerrar la app. `takePersistableUriPermission` falla con ellos. Por eso hay
 *   que **copiarlos**: si no, el material queda inservible mañana.
 * - Los archivos elegidos con el **selector de la propia app** (`OpenDocument` o
 *   `OpenDocumentTree`) sí admiten permiso permanente, así que se pueden **referenciar**
 *   sin ocupar espacio.
 */
class FileImporter(private val context: Context) {

    /** Lee nombre, tipo y tamaño sin copiar nada. */
    fun describe(uri: Uri): IncomingFile {
        var name: String? = null
        var size: Long? = null

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                            name = cursor.getString(nameIndex)
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo leer la información de $uri", e)
            }
        }

        if (name == null) name = uri.lastPathSegment

        return IncomingFile(
            uri = uri,
            displayName = name,
            mimeType = context.contentResolver.getType(uri),
            sizeBytes = size
        )
    }

    /**
     * Intenta quedarse el permiso de forma permanente. Solo funciona con URIs del selector
     * del sistema; con los que llegan compartidos falla, y entonces toca copiar.
     */
    fun tryPersist(uri: Uri): Boolean = try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        true
    } catch (e: SecurityException) {
        false
    }

    /**
     * Copia el archivo dentro de la app, en una carpeta por área.
     * @return ruta absoluta del archivo copiado, o null si falla.
     */
    suspend fun copyIntoArea(uri: Uri, areaId: Int, fileName: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "areas/$areaId").apply { mkdirs() }
                val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80)
                val target = File(dir, "${System.currentTimeMillis()}_$safeName")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext null

                target.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo copiar $uri", e)
                null
            }
        }

    /** Comprueba que un material referenciado sigue existiendo. */
    fun stillAvailable(item: Item): Boolean {
        item.localPath?.let { return File(it).exists() }
        val uri = item.uri ?: return false
        return try {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /** Borra la copia propia, si la hay. Los referenciados no se tocan. */
    fun deleteLocalCopy(item: Item) {
        item.localPath?.let { path ->
            runCatching { File(path).delete() }
        }
    }
}
