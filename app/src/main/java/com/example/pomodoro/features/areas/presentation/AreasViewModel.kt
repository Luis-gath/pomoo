package com.example.pomodoro.features.areas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.features.areas.data.Area
import com.example.pomodoro.features.areas.data.AreaRepository
import com.example.pomodoro.features.areas.data.AreaType
import com.example.pomodoro.features.areas.data.Item
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AreasUiState(
    val areas: List<Area> = emptyList(),
    val deliverables: List<Item> = emptyList(),
    val showCreateDialog: Boolean = false
)

@HiltViewModel
class AreasViewModel @Inject constructor(
    private val repository: AreaRepository
) : ViewModel() {

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    val areas: StateFlow<List<Area>> = repository.getActiveAreas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Entregas de todas las áreas: lo urgente se ve sin entrar en cada una. */
    val deliverables: StateFlow<List<Item>> = repository.getUpcomingDeliverables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun showCreateDialog() { _showCreateDialog.value = true }

    fun hideCreateDialog() { _showCreateDialog.value = false }

    fun createArea(name: String, type: AreaType, colorHex: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            repository.saveArea(Area(name = trimmed, type = type, colorHex = colorHex))
            _showCreateDialog.value = false
        }
    }

    fun archiveArea(area: Area) {
        viewModelScope.launch { repository.archiveArea(area.id) }
    }
}
