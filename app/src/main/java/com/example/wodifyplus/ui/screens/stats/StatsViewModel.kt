package com.example.wodifyplus.ui.screens.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wodifyplus.data.local.WodDatabase
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.data.repository.WodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class StatsData(
    val totalWorkouts: Int = 0,
    val totalCalories: Int = 0,
    val totalDistance: Double = 0.0,
    val totalMinutes: Int = 0,
    val workoutsByActivity: Map<String, Int> = emptyMap(),
    val workoutsByDate: List<Pair<LocalDateTime, Int>> = emptyList()
)

enum class StatsPeriod {
    WEEK, MONTH, YEAR
}

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WodRepository
    
    init {
        val wodDao = WodDatabase.getDatabase(application).wodDao()
        repository = WodRepository(wodDao)
    }

    private val _statsData = MutableStateFlow(StatsData())
    val statsData: StateFlow<StatsData> = _statsData.asStateFlow()
    
    private val _selectedPeriod = MutableStateFlow(StatsPeriod.WEEK)
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod.asStateFlow()

    fun loadStats(period: StatsPeriod) {
        _selectedPeriod.value = period
        
        viewModelScope.launch {
            val endDate = LocalDateTime.now()
            val startDate = when (period) {
                StatsPeriod.WEEK -> endDate.minusDays(7)
                StatsPeriod.MONTH -> endDate.minusMonths(1)
                StatsPeriod.YEAR -> endDate.minusYears(1)
            }
            
            repository.getCompletedWodsBetween(startDate, endDate).collect { wods ->
                _statsData.value = calculateStats(wods, startDate, endDate)
            }
        }
    }
    
    private fun calculateStats(wods: List<Wod>, startDate: LocalDateTime, endDate: LocalDateTime): StatsData {
        val totalCalories = wods.sumOf { it.caloriasQuemadas ?: 0 }
        val totalDistance = wods.sumOf { it.distanciaKm ?: 0.0 }
        val totalMinutes = wods.sumOf { it.duracionMinutos ?: 0 }
        
        val workoutsByActivity = wods.groupBy { it.gimnasio }
            .mapValues { it.value.size }
        
        // Agrupar por día para la gráfica
        val workoutsByDate = wods
            .groupBy { it.fechaCompletado?.truncatedTo(ChronoUnit.DAYS) }
            .mapNotNull { (date, wods) -> 
                date?.let { it to wods.size }
            }
            .sortedBy { it.first }
        
        return StatsData(
            totalWorkouts = wods.size,
            totalCalories = totalCalories,
            totalDistance = totalDistance,
            totalMinutes = totalMinutes,
            workoutsByActivity = workoutsByActivity,
            workoutsByDate = workoutsByDate
        )
    }
}


