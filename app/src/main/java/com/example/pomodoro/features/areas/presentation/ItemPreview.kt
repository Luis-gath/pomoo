package com.example.pomodoro.features.areas.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.util.Size
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pomodoro.features.areas.data.Item
import com.example.pomodoro.features.areas.data.ItemKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt

private const val PREVIEW_SIZE_PX = 288

/** Conserva unas pocas miniaturas para que el scroll no vuelva a decodificar archivos. */
private val previewCache = object : LruCache<String, Bitmap>(12 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

/**
 * Miniatura local para los formatos que Android puede representar sin abrir otra app.
 * Fotos muestran la imagen, vídeos un fotograma y PDFs su primera página.
 */
@Composable
fun ItemPreview(
    item: Item,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cacheKey = previewKey(item)
    val cachedPreview = previewCache.get(cacheKey)
    var preview by remember(cacheKey) { mutableStateOf(cachedPreview) }
    LaunchedEffect(cacheKey) {
        preview = cachedPreview ?: if (item.kind.supportsPreview()) {
            withContext(Dispatchers.IO) {
                previewCache.get(cacheKey) ?: loadItemPreview(context, item)?.also { bitmap ->
                    previewCache.put(cacheKey, bitmap)
                }
            }
        } else null
    }

    Crossfade(targetState = preview, label = "item_preview") { bitmap ->
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Vista previa de ${item.title}",
                contentScale = ContentScale.Crop,
                modifier = modifier.clip(RoundedCornerShape(13.dp))
            )
        } else {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = itemKindIcon(item.kind),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

private fun previewKey(item: Item): String =
    "${item.id}:${item.kind}:${item.uri}:${item.localPath}:${item.createdAt}"

private fun ItemKind.supportsPreview(): Boolean =
    this == ItemKind.FOTO || this == ItemKind.VIDEO || this == ItemKind.PDF

private fun loadItemPreview(context: Context, item: Item): Bitmap? = runCatching {
    when (item.kind) {
        ItemKind.FOTO -> loadPhotoPreview(context, item)
        ItemKind.VIDEO -> loadVideoPreview(context, item)
        ItemKind.PDF -> loadPdfPreview(context, item)
        else -> null
    }
}.getOrNull()

private fun loadPhotoPreview(context: Context, item: Item): Bitmap? {
    val contentUri = item.uri?.let(Uri::parse)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && contentUri != null) {
        runCatching {
            return context.contentResolver.loadThumbnail(
                contentUri,
                Size(PREVIEW_SIZE_PX, PREVIEW_SIZE_PX),
                null
            )
        }
    }

    val openSource = item.sourceOpener(context) ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openSource()?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
    }
    return openSource()?.use { BitmapFactory.decodeStream(it, null, options) }
}

private fun calculateSampleSize(width: Int, height: Int): Int {
    var sample = 1
    while (width / (sample * 2) >= PREVIEW_SIZE_PX &&
        height / (sample * 2) >= PREVIEW_SIZE_PX
    ) {
        sample *= 2
    }
    return sample
}

private fun loadVideoPreview(context: Context, item: Item): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        when {
            item.localPath != null -> retriever.setDataSource(item.localPath)
            item.uri != null -> retriever.setDataSource(context, Uri.parse(item.uri))
            else -> return null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(
                -1,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                PREVIEW_SIZE_PX,
                PREVIEW_SIZE_PX
            )
        } else {
            @Suppress("DEPRECATION")
            retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
    } finally {
        retriever.release()
    }
}

private fun loadPdfPreview(context: Context, item: Item): Bitmap? {
    val descriptor = when {
        item.localPath != null -> ParcelFileDescriptor.open(
            File(item.localPath),
            ParcelFileDescriptor.MODE_READ_ONLY
        )
        item.uri != null -> context.contentResolver.openFileDescriptor(Uri.parse(item.uri), "r")
        else -> null
    } ?: return null

    return descriptor.use { pfd ->
        PdfRenderer(pfd).use { renderer ->
            if (renderer.pageCount == 0) return@use null
            renderer.openPage(0).use { page ->
                val width = PREVIEW_SIZE_PX
                val height = (page.height * (width.toFloat() / page.width))
                    .roundToInt()
                    .coerceIn(1, PREVIEW_SIZE_PX * 2)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }
}

private fun Item.sourceOpener(context: Context): (() -> InputStream?)? {
    localPath?.let { path ->
        val file = File(path)
        return { file.inputStream() }
    }
    uri?.let { value ->
        val parsed = Uri.parse(value)
        return { context.contentResolver.openInputStream(parsed) }
    }
    return null
}
