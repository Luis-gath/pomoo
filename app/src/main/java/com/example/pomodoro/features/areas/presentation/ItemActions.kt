package com.example.pomodoro.features.areas.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.pomodoro.features.areas.data.Item
import com.example.pomodoro.features.areas.data.ItemKind
import java.io.File

private const val TAG = "ItemActions"

/**
 * Abrir y enviar material.
 *
 * En vez de construir visores propios de PDF o vídeo, se delega en las apps que el usuario
 * ya tiene: abre su lector habitual, y el material se envía con el menú de compartir de
 * Android, que es la única vía posible hacia NotebookLM (no tiene API pública).
 */
object ItemActions {

    private fun authority(context: Context) = "${context.packageName}.fileprovider"

    /**
     * URI que se puede entregar a otra app.
     * - Copia propia: hay que envolverla con el FileProvider; pasar un `file://` lanza excepción.
     * - Referencia externa: ya es un `content://` del sistema y se reenvía tal cual.
     */
    private fun shareableUri(context: Context, item: Item): Uri? {
        item.localPath?.let { path ->
            val file = File(path)
            if (!file.exists()) return null
            return runCatching {
                FileProvider.getUriForFile(context, authority(context), file)
            }.onFailure { Log.e(TAG, "FileProvider no pudo exponer $path", it) }.getOrNull()
        }
        return item.uri?.let(Uri::parse)
    }

    private fun mimeTypeOf(item: Item): String {
        val name = item.localPath ?: item.uri ?: return "*/*"
        val ext = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: when (item.kind) {
                ItemKind.PDF -> "application/pdf"
                ItemKind.FOTO -> "image/*"
                ItemKind.VIDEO -> "video/*"
                ItemKind.AUDIO -> "audio/*"
                else -> "*/*"
            }
    }

    /** Abre un material con la app que el usuario prefiera. */
    fun open(context: Context, item: Item): String? {
        if (item.kind == ItemKind.ENLACE) {
            val url = item.content.trim()
            if (url.isEmpty()) return "El enlace está vacío"
            return launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // Las notas se muestran dentro de la app; no hay nada que abrir fuera.
        if (item.kind == ItemKind.NOTA) return null

        val uri = shareableUri(context, item)
            ?: return "El archivo ya no está disponible"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeTypeOf(item))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return launch(context, intent)
    }

    /**
     * Envía material a otra app: NotebookLM, Drive, correo…
     * @return mensaje de error, o null si salió bien.
     */
    fun share(context: Context, items: List<Item>): String? {
        val shareable = items.filter { it.kind != ItemKind.NOTA && it.kind != ItemKind.ENLACE }
        val uris = ArrayList(shareable.mapNotNull { shareableUri(context, it) })

        // Enlaces y notas viajan como texto, no como archivo.
        val text = items
            .filter { it.kind == ItemKind.ENLACE || it.kind == ItemKind.NOTA }
            .joinToString("\n\n") { "${it.title}\n${it.content}" }
            .ifBlank { null }

        if (uris.isEmpty() && text == null) return "No hay nada que enviar"

        val intent = when {
            uris.size > 1 -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = commonMimeType(shareable)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }

            uris.size == 1 -> Intent(Intent.ACTION_SEND).apply {
                type = mimeTypeOf(shareable.first())
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }

            else -> Intent(Intent.ACTION_SEND).apply { type = "text/plain" }
        }.apply {
            text?.let { putExtra(Intent.EXTRA_TEXT, it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return launch(context, Intent.createChooser(intent, "Enviar material a…"))
    }

    /** Si todo es del mismo tipo se declara ese; si se mezclan, un comodín. */
    private fun commonMimeType(items: List<Item>): String {
        val types = items.map { mimeTypeOf(it) }.distinct()
        if (types.size == 1) return types.first()
        val families = types.map { it.substringBefore('/') }.distinct()
        return if (families.size == 1) "${families.first()}/*" else "*/*"
    }

    private fun launch(context: Context, intent: Intent): String? = try {
        context.startActivity(intent)
        null
    } catch (e: ActivityNotFoundException) {
        "No hay ninguna app que pueda abrir esto"
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo lanzar el intent", e)
        "No se pudo abrir"
    }
}
