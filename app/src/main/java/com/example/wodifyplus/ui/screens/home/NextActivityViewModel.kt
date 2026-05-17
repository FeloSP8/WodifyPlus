package com.example.wodifyplus.ui.screens.home

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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NextActivityViewModel @Inject constructor(
    application: Application,
    private val repository: WodRepository
) : AndroidViewModel(application) {

    private val _weeklyActivities = MutableStateFlow<Map<LocalDate, List<Wod>>>(emptyMap())
    val weeklyActivities: StateFlow<Map<LocalDate, List<Wod>>> = _weeklyActivities.asStateFlow()

    init {
        loadWeeklyActivities()
    }

    fun loadWeeklyActivities() {
        viewModelScope.launch {
            repository.selectedWods.collect { wods ->
                val now = LocalDateTime.now()
                val today = now.toLocalDate()
                val endDate = today.with(DayOfWeek.SUNDAY).plusWeeks(1)

                // Regla: una actividad aparece hasta que llega su hora planificada.
                // - Con hora: visible si fecha+hora > ahora (aún no ha llegado su momento)
                // - Sin hora: visible todo el día (fecha >= hoy)
                // Completada o no es irrelevante — la hora manda.
                val filteredWods = wods
                    .filter { wod ->
                        val wodDateTime = if (wod.hora != null) {
                            wod.fecha.atTime(wod.hora)
                        } else {
                            wod.fecha.atStartOfDay()
                        }
                        wodDateTime.isAfter(now) && !wod.fecha.isAfter(endDate)
                    }
                    .sortedWith(compareBy({ it.fecha }, { it.hora }))

                _weeklyActivities.value = filteredWods.groupBy { it.fecha }
            }
        }
    }

    // Mantener compatibilidad: obtener la próxima actividad
    fun getNextActivity(): Wod? {
        return _weeklyActivities.value
            .flatMap { it.value }
            .firstOrNull()
    }
}

