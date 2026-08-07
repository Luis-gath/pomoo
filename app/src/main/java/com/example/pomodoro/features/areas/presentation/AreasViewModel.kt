package com.example.pomodoro.features.areas.presentation

import kotlinx.coroutines.flow.first
import com.example.pomodoro.features.premium.domain.FreeLimits
import com.example.pomodoro.features.premium.data.PremiumStorage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.features.areas.data.Area
import com.example.pomodoro.features.areas.data.AreaRepository
import com.example.pomodoro.features.areas.data.AreaType
import com.example.pomodoro.features.areas.data.Item
import com.example.pomodoro.features.areas.data.ItemMark
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AreaOverviewUi(
    val area: Area,
    val materialCount: Int,
    val importantCount: Int,
    val deliveryCount: Int,
    val lastActivityAt: Long?
)

@HiltViewModel
class AreasViewModel @Inject constructor(
    private val repository: AreaRepository,
    private val premiumStorage: PremiumStorage
) : ViewModel() {

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    val areas: StateFlow<List<Area>> = repository.getActiveAreas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Métricas de portada calculadas en una sola combinación reactiva. */
    val areaOverviews: StateFlow<List<AreaOverviewUi>> =
        combine(repository.getActiveAreas(), repository.getAllItems()) { activeAreas, allItems ->
            activeAreas.map { area ->
                val areaItems = allItems.filter { it.areaId == area.id }
                AreaOverviewUi(
                    area = area,
                    materialCount = areaItems.size,
                    importantCount = areaItems.count { it.mark == ItemMark.IMPORTANTE },
                    deliveryCount = areaItems.count { it.mark == ItemMark.ENTREGA },
                    lastActivityAt = areaItems.maxOfOrNull { it.createdAt }
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Entregas de todas las áreas: lo urgente se ve sin entrar en cada una. */
    val deliverables: StateFlow<List<Item>> = repository.getUpcomingDeliverables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun showCreateDialog() { _showCreateDialog.value = true }

    fun hideCreateDialog() { _showCreateDialog.value = false }

    /** Áreas que aún caben en la versión gratuita, o null si es premium. */
    val remainingFreeAreas: StateFlow<Int?> =
        combine(areas, premiumStorage.isPremium) { list, isPremium ->
            FreeLimits.remainingAreas(list.size, isPremium)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _limitReached = MutableStateFlow(false)
    val limitReached: StateFlow<Boolean> = _limitReached.asStateFlow()

    fun dismissLimitNotice() { _limitReached.value = false }

    fun createArea(name: String, type: AreaType, colorHex: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            // El tope se comprueba aquí y no en la pantalla para que no dependa de que la
            // interfaz recuerde deshabilitar el botón.
            val isPremium = premiumStorage.isPremium.first()
            if (!FreeLimits.canCreateArea(areas.value.size, isPremium)) {
                _showCreateDialog.value = false
                _limitReached.value = true
                return@launch
            }

            repository.saveArea(Area(name = trimmed, type = type, colorHex = colorHex))
            _showCreateDialog.value = false
        }
    }

    fun archiveArea(area: Area) {
        viewModelScope.launch { repository.archiveArea(area.id) }
    }
}
