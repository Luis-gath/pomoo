package com.example.pomodoro.core.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Lo que otra app acaba de compartir con nosotros. */
data class SharedContent(
    val uris: List<Uri> = emptyList(),
    val text: String? = null
) {
    val isEmpty: Boolean get() = uris.isEmpty() && text.isNullOrBlank()
}

/**
 * Buzón de lo que llega por el menú "Compartir" de Android.
 *
 * La Activity deja aquí el contenido y la pantalla de destino lo recoge. Se hace así en vez
 * de pasar los URIs como argumentos de navegación porque son listas y hay que conservarlos
 * intactos: convertirlos a texto y de vuelta es una fuente de errores.
 */
@Singleton
class ShareInbox @Inject constructor() {

    private val _pending = MutableStateFlow(SharedContent())
    val pending: StateFlow<SharedContent> = _pending.asStateFlow()

    /** @return true si el intent traía algo que podamos guardar. */
    fun offer(intent: Intent?): Boolean {
        val content = parse(intent) ?: return false
        if (content.isEmpty) return false
        _pending.value = content
        return true
    }

    fun consume() {
        _pending.value = SharedContent()
    }

    private fun parse(intent: Intent?): SharedContent? {
        if (intent == null) return null

        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
                SharedContent(
                    uris = listOfNotNull(uri),
                    text = intent.getStringExtra(Intent.EXTRA_TEXT)
                )
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtraCompat<Uri>(Intent.EXTRA_STREAM)
                SharedContent(uris = uris.orEmpty())
            }

            else -> null
        }
    }

    private inline fun <reified T : android.os.Parcelable> Intent.getParcelableExtraCompat(
        name: String
    ): T? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(name)
    }

    private inline fun <reified T : android.os.Parcelable> Intent.getParcelableArrayListExtraCompat(
        name: String
    ): ArrayList<T>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayListExtra(name)
    }
}
