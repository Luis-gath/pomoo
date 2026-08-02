package com.example.pomodoro.features.areas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.core.share.ShareInbox
import com.example.pomodoro.features.areas.data.Area
import com.example.pomodoro.features.areas.data.AreaRepository
import com.example.pomodoro.features.areas.data.AreaType
import com.example.pomodoro.features.areas.data.ItemMark
import com.example.pomodoro.features.areas.domain.ImportItemsUseCase
import com.example.pomodoro.features.areas.domain.ImportSource
import com.example.pomodoro.features.areas.domain.ItemKindResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiveShareViewModel @Inject constructor(
    private val shareInbox: ShareInbox,
    private val repository: AreaRepository,
    private val importItems: ImportItemsUseCase
) : ViewModel() {

    val incoming = shareInbox.pending

    val areas: StateFlow<List<Area>> = repository.getActiveAreas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    fun createAreaAndSave(name: String, mark: ItemMark, dueAt: Long?) {
        viewModelScope.launch {
            val id = repository.saveArea(Area(name = name.trim(), type = AreaType.CURSO)).toInt()
            save(id, mark, dueAt)
        }
    }

    fun save(areaId: Int, mark: ItemMark, dueAt: Long?) {
        if (_saving.value) return
        _saving.value = true

        viewModelScope.launch {
            val content = shareInbox.pending.value

            // Un texto que es una URL se guarda como enlace; el resto, como nota.
            content.text?.takeIf { it.isNotBlank() && content.uris.isEmpty() }?.let { text ->
                if (ItemKindResolver.isLink(text)) {
                    importItems.saveLink(text, areaId, mark = mark)
                } else {
                    importItems.saveNote(text, areaId, mark = mark)
                }
            }

            var saved = 0
            if (content.uris.isNotEmpty()) {
                // ImportSource.SHARE: el permiso que da Android al compartir es temporal,
                // así que estos archivos se copian sí o sí.
                val outcome = importItems(
                    uris = content.uris,
                    areaId = areaId,
                    source = ImportSource.SHARE,
                    mark = mark,
                    dueAt = dueAt
                )
                saved = outcome.saved
                _result.value = if (outcome.failed > 0) {
                    "$saved guardados, ${outcome.failed} no se pudieron leer"
                } else {
                    "$saved guardados"
                }
            } else {
                _result.value = "Guardado"
            }

            shareInbox.consume()
            _saving.value = false
        }
    }

    fun discard() {
        shareInbox.consume()
    }
}
