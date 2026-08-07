package com.example.pomodoro.features.areas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.features.areas.data.AreaRepository
import com.example.pomodoro.features.areas.data.DeliverableAlarmScheduler
import com.example.pomodoro.features.areas.data.Item
import com.example.pomodoro.features.areas.domain.DeliverableGrouping
import com.example.pomodoro.features.areas.domain.DueBucket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Una entrega con el nombre de su área, para no consultarla por cada fila. */
data class DeliverableRow(
    val item: Item,
    val areaName: String,
    val bucket: DueBucket
)

@HiltViewModel
class UpcomingDeliverablesViewModel @Inject constructor(
    private val repository: AreaRepository,
    private val alarmScheduler: DeliverableAlarmScheduler
) : ViewModel() {

    /**
     * Marca el paso del tiempo, no solo el cambio de datos.
     *
     * Sin esto la clasificación se calculaba una única vez por emisión del repositorio,
     * así que con la pantalla abierta una entrega seguía apareciendo bajo «Hoy» después
     * de haber vencido. El minuto es de sobra: las franjas son de horas y días.
     */
    private val tick: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    val rows: StateFlow<List<DeliverableRow>> = combine(
        repository.getPendingDeliverables(),
        repository.getAllAreas(),
        tick
    ) { items, areas, now ->
        val names = areas.associate { it.id to it.name }
        items.mapNotNull { item ->
            val dueAt = item.dueAt ?: return@mapNotNull null
            DeliverableRow(
                item = item,
                areaName = names[item.areaId].orEmpty(),
                bucket = DeliverableGrouping.bucketOf(dueAt, now)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markDone(item: Item) {
        viewModelScope.launch {
            repository.setDeliverableCompleted(item.id, System.currentTimeMillis())
            alarmScheduler.cancel(item.id)
        }
    }
}
