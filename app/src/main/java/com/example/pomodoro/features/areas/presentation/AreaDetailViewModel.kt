package com.example.pomodoro.features.areas.presentation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.features.areas.data.Area
import com.example.pomodoro.features.areas.data.AreaRepository
import com.example.pomodoro.features.areas.data.FileImporter
import com.example.pomodoro.features.areas.data.Item
import com.example.pomodoro.features.areas.data.ItemMark
import com.example.pomodoro.features.areas.domain.ImportItemsUseCase
import com.example.pomodoro.features.areas.domain.ImportSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Filtro de la línea temporal del área. */
enum class AreaFilter { TODO, IMPORTANTES, ENTREGAS }

@HiltViewModel
class AreaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AreaRepository,
    private val importItems: ImportItemsUseCase,
    private val fileImporter: FileImporter
) : ViewModel() {

    private val areaId: Int = checkNotNull(savedStateHandle["areaId"])

    private val _filter = MutableStateFlow(AreaFilter.TODO)
    val filter: StateFlow<AreaFilter> = _filter.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val area: StateFlow<Area?> = repository.getAreaByIdFlow(areaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allItems: StateFlow<List<Item>> = repository.getItemsForArea(areaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<Item>> =
        combine(allItems, _filter) { all, filter ->
            when (filter) {
                AreaFilter.TODO -> all
                AreaFilter.IMPORTANTES -> all.filter { it.mark == ItemMark.IMPORTANTE }
                AreaFilter.ENTREGAS -> all.filter { it.mark == ItemMark.ENTREGA }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: AreaFilter) { _filter.value = filter }

    fun consumeMessage() { _message.value = null }

    /** Archivos elegidos con el selector del sistema: se referencian si se puede. */
    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val result = importItems(uris, areaId, ImportSource.PICKER)
            _message.value = buildString {
                append("${result.saved} añadidos")
                if (result.failed > 0) append(", ${result.failed} fallaron")
            }
        }
    }

    /**
     * Adopta una carpeta entera. Android no permite compartir carpetas, así que este es el
     * camino: el selector de árbol de documentos concede acceso permanente a todo lo que
     * hay dentro.
     */
    fun addFolder(treeUri: Uri, childUris: List<Uri>) {
        viewModelScope.launch {
            fileImporter.tryPersist(treeUri)
            val result = importItems(childUris, areaId, ImportSource.PICKER)
            _message.value = "${result.saved} archivos de la carpeta añadidos"
        }
    }

    fun addLink(url: String) {
        viewModelScope.launch {
            importItems.saveLink(url, areaId)
            _message.value = "Enlace guardado"
        }
    }

    fun addNote(text: String) {
        viewModelScope.launch {
            importItems.saveNote(text, areaId)
            _message.value = "Nota guardada"
        }
    }

    fun cycleMark(item: Item) {
        val next = when (item.mark) {
            ItemMark.NINGUNA -> ItemMark.IMPORTANTE
            ItemMark.IMPORTANTE -> ItemMark.ENTREGA
            ItemMark.ENTREGA -> ItemMark.NINGUNA
        }
        viewModelScope.launch {
            repository.updateMark(item.id, next, if (next == ItemMark.ENTREGA) item.dueAt else null)
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            fileImporter.deleteLocalCopy(item)
            repository.deleteItem(item)
            _message.value = "Material eliminado"
        }
    }
}
