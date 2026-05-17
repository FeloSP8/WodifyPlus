package com.example.wodifyplus.ui.screens.weekly

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wodifyplus.data.local.WodDatabase
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.data.repository.WodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.*

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WeeklyViewModel @Inject constructor(
    application: Application,
    private val repository: WodRepository
) : AndroidViewModel(application) {

    private val _weeklyWods = MutableStateFlow<List<Wod>>(emptyList())
    val weeklyWods: StateFlow<List<Wod>> = _weeklyWods.asStateFlow()

    fun loadWeeklyWods() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val startOfWeek = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L)
            val endOfWeek = startOfWeek.plusDays(6)
            
            // Obtener WODs completados de la semana
            repository.getCompletedWodsBetween(
                startOfWeek.atStartOfDay(),
                endOfWeek.atTime(23, 59, 59)
            ).collect { completed ->
                // Obtener WODs seleccionados de la semana
                repository.selectedWods.first().let { selected ->
                    val weeklySelected = selected.filter { wod ->
                        !wod.fecha.isBefore(startOfWeek) && !wod.fecha.isAfter(endOfWeek)
                    }
                    _weeklyWods.value = (weeklySelected + completed).distinctBy { it.id }
                }
            }
        }
    }

    fun updateWodTime(wod: Wod, time: LocalTime) {
        viewModelScope.launch {
            val updatedWod = wod.copy(hora = time)
            repository.updateWod(updatedWod)
        }
    }

    fun deleteWodFromCalendar(wod: Wod) {
        viewModelScope.launch {
            val updatedWod = wod.copy(seleccionado = false, hora = null, notificacionActiva = false)
            repository.updateWod(updatedWod)
        }
    }

    fun completeWod(
        wod: Wod,
        calorias: Int?,
        distancia: Double?,
        duracion: Int?,
        notas: String?
    ) {
        viewModelScope.launch {
            val completedWod = wod.copy(
                completada = true,
                fechaCompletado = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime(),
                caloriasQuemadas = calorias,
                distanciaKm = distancia,
                duracionMinutos = duracion,
                notas = notas
            )
            repository.updateWod(completedWod)
        }
    }
}

