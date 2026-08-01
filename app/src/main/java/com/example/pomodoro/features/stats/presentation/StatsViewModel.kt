package com.example.pomodoro.features.stats.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.features.stats.data.DailyStatsEntity
import com.example.pomodoro.features.stats.data.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class StatsUiState(
    val selectedRange: StatsRange = StatsRange.TODAY,

    // KPI Data
    val focusCount: Int = 0,
    val focusMinutes: Int = 0,
    val tasksCompleted: Int = 0,

    // Charts
    val weeklyChartData: List<BarData> = emptyList(),

    // Productivity
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,

    // Top Lists
    val topTasks: List<TopItemData> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    /** Se cancela al cambiar de rango para no acumular colectores sobre el flujo anterior. */
    private var rangeJob: Job? = null

    init {
        loadStatsForRange(StatsRange.TODAY)
        loadTopLists()
        observeStreak()
    }

    private fun observeStreak() {
        viewModelScope.launch {
            repository.getCurrentStreak().collect { streak ->
                _uiState.update { it.copy(currentStreak = streak) }
            }
        }
    }

    fun setRange(range: StatsRange) {
        _uiState.update { it.copy(selectedRange = range) }
        loadStatsForRange(range)
    }

    private fun loadStatsForRange(range: StatsRange) {
        rangeJob?.cancel()
        rangeJob = viewModelScope.launch {
            val (start, end) = getDatesForRange(range)

            repository.getStatsForRange(start, end).collect { list ->
                val count = list.sumOf { it.focusCount }
                val minutes = list.sumOf { it.focusMinutes }
                val tasks = list.sumOf { it.tasksCompleted }

                val chartData = if (range == StatsRange.DAYS_7 || range == StatsRange.TODAY) {
                    generateWeeklyChartData(list, start)
                } else {
                    generateMonthlyChartData()
                }

                _uiState.update { state ->
                    state.copy(
                        focusCount = count,
                        focusMinutes = minutes,
                        tasksCompleted = tasks,
                        weeklyChartData = chartData
                    )
                }
            }
        }
    }

    private fun generateWeeklyChartData(
        statsDocs: List<DailyStatsEntity>,
        startDate: LocalDate
    ): List<BarData> {
        val result = mutableListOf<BarData>()
        val map = statsDocs.associateBy { it.date }

        // Find max for scaling
        val maxVal = statsDocs.maxOfOrNull { it.focusCount } ?: 10
        val scaleMax = if (maxVal == 0) 10 else (maxVal * 1.2).toInt()

        for (i in 0 until 7) {
            val date = startDate.plusDays(i.toLong())
            val stat = map[date.toString()]
            result.add(BarData(dayLabel(date), stat?.focusCount ?: 0, scaleMax))
        }
        return result
    }

    // Rangos largos aún no tienen agregación propia; se muestra vacío en lugar de datos erróneos.
    private fun generateMonthlyChartData(): List<BarData> = emptyList()

    private fun dayLabel(date: LocalDate): String = when (date.dayOfWeek.value) {
        1 -> "L"
        2 -> "M"
        3 -> "X"
        4 -> "J"
        5 -> "V"
        6 -> "S"
        7 -> "D"
        else -> "?"
    }

    private fun loadTopLists() {
        viewModelScope.launch {
            repository.getTopTasksByPomodoros().collect { tasks ->
                val topItems = tasks.map {
                    TopItemData(
                        title = it.title,
                        subtitle = it.courseOrProject,
                        value = it.completedPomodoros,
                        progress = it.pomodoroProgress
                    )
                }
                _uiState.update { state -> state.copy(topTasks = topItems) }
            }
        }
    }

    private fun getDatesForRange(range: StatsRange): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (range) {
            StatsRange.TODAY -> today to today
            StatsRange.DAYS_7 -> today.minusDays(6) to today
            StatsRange.DAYS_30 -> today.minusDays(29) to today
            StatsRange.ALL -> LocalDate.of(2024, 1, 1) to today
        }
    }
}
