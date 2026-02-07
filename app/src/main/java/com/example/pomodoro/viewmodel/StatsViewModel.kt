package com.example.pomodoro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.data.local.TaskDatabase
import com.example.pomodoro.data.repository.StatsRepository
import com.example.pomodoro.ui.components.BarData
import com.example.pomodoro.ui.components.StatsRange
import com.example.pomodoro.ui.components.TopItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    val topTasks: List<TopItemData> = emptyList(),
    val topCourses: List<TopItemData> = emptyList()
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TaskDatabase.getInstance(application)
    private val repository = StatsRepository(database.statsDao, database.taskDao)
    
    // We can also access StatsDataStore for legacy streak info if needed
    // private val statsDataStore = StatsDataStore(application)

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStatsForRange(StatsRange.TODAY)
        loadTopLists()
    }

    fun setRange(range: StatsRange) {
        _uiState.value = _uiState.value.copy(selectedRange = range)
        loadStatsForRange(range)
    }

    private fun loadStatsForRange(range: StatsRange) {
        viewModelScope.launch {
            val (start, end) = getDatesForRange(range)
            
            repository.getStatsForRange(start, end).collect { list ->
                // Calculate KPIs
                val count = list.sumOf { it.focusCount }
                val minutes = list.sumOf { it.focusMinutes }
                val tasks = list.sumOf { it.tasksCompleted }
                
                // Chart Data Logic (simplified for brevity)
                // If range is 7 days, we want 7 bars. If list has gaps, we need to fill them.
                val chartData = if (range == StatsRange.DAYS_7 || range == StatsRange.TODAY) {
                     generateWeeklyChartData(list, start)
                } else {
                     generateMonthlyChartData(list, start)
                }
                
                _uiState.value = _uiState.value.copy(
                    focusCount = count,
                    focusMinutes = minutes,
                    tasksCompleted = tasks,
                    weeklyChartData = chartData,
                    // Mock streak for now, real implementation would require complex query
                    currentStreak = 0 
                )
            }
        }
    }
    
    private fun generateWeeklyChartData(
        statsDocs: List<com.example.pomodoro.data.model.DailyStatsEntity>, 
        startDate: LocalDate
    ): List<BarData> {
        val result = mutableListOf<BarData>()
        val map = statsDocs.associateBy { it.date }
        
        // Find max for scaling
        val maxVal = statsDocs.maxOfOrNull { it.focusCount } ?: 10
        val scaleMax = if (maxVal == 0) 10 else (maxVal * 1.2).toInt()

        for (i in 0 until 7) {
            val date = startDate.plusDays(i.toLong())
            val dateStr = date.toString()
            val stat = map[dateStr]
            
            val label = when(date.dayOfWeek.value) {
                1 -> "L"
                2 -> "M"
                3 -> "X"
                4 -> "J"
                5 -> "V"
                6 -> "S"
                7 -> "D"
                else -> "?"
            }
            
            result.add(BarData(label, stat?.focusCount ?: 0, scaleMax))
        }
        return result
    }

    private fun generateMonthlyChartData(
        statsDocs: List<com.example.pomodoro.data.model.DailyStatsEntity>, 
        startDate: LocalDate
    ): List<BarData> {
         // Aggregated by week roughly
          val result = mutableListOf<BarData>()
          // Simplify: just show total bars for days is too crowded, maybe chunks?
          // Fallback to simple logic: Just show last 7 entries even for month for now to reuse component
          return emptyList()
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
                _uiState.value = _uiState.value.copy(topTasks = topItems)
            }
        }
    }

    private fun getDatesForRange(range: StatsRange): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (range) {
            StatsRange.TODAY -> today to today
            StatsRange.DAYS_7 -> today.minusDays(6) to today // Last 7 days inclusive
            StatsRange.DAYS_30 -> today.minusDays(29) to today
            StatsRange.ALL -> LocalDate.of(2024, 1, 1) to today
        }
    }
}
