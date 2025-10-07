package com.example.wodifyplus.ui.screens.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wodifyplus.data.local.WodDatabase
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.data.preferences.PreferencesManager
import com.example.wodifyplus.data.repository.WodRepository
import com.example.wodifyplus.notifications.NotificationScheduler
import com.example.wodifyplus.widget.WodWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WodRepository
    private val preferencesManager = PreferencesManager(application)

    init {
        val wodDao = WodDatabase.getDatabase(application).wodDao()
        repository = WodRepository(wodDao)
    }

    private val _selectedWods = MutableStateFlow<List<Wod>>(emptyList())
    val selectedWods: StateFlow<List<Wod>> = _selectedWods.asStateFlow()

    fun loadSelectedWods() {
        viewModelScope.launch {
            repository.selectedWods.collect { wods ->
                _selectedWods.value = wods
            }
        }
    }

    fun updateWodTime(wod: Wod, newTime: LocalTime?) {
        viewModelScope.launch {
            val updatedWod = wod.copy(
                hora = newTime,
                notificacionActiva = newTime != null
            )
            repository.updateWod(updatedWod)
            
            // Reprogramar notificación
            if (newTime != null) {
                val minutesBefore = preferencesManager.notificationMinutesBefore.first()
                NotificationScheduler.scheduleWodReminder(
                    getApplication(),
                    updatedWod,
                    minutesBefore
                )
            } else {
                NotificationScheduler.cancelWodReminder(getApplication(), wod.id)
            }
            
            // Actualizar widget
            WodWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteWodFromCalendar(wod: Wod) {
        viewModelScope.launch {
            val updatedWod = wod.copy(
                seleccionado = false,
                hora = null,
                notificacionActiva = false
            )
            repository.updateWod(updatedWod)
            
            // Cancelar notificación
            NotificationScheduler.cancelWodReminder(getApplication(), wod.id)
            
            // Actualizar widget
            WodWidgetProvider.updateAllWidgets(getApplication())
        }
    }
    
    fun completeWod(
        wod: Wod,
        calories: Int?,
        distance: Double?,
        duration: Int?,
        notes: String?
    ) {
        viewModelScope.launch {
            val updatedWod = wod.copy(
                completada = true,
                fechaCompletado = LocalDateTime.now(),
                caloriasQuemadas = calories,
                distanciaKm = distance,
                duracionMinutos = duration,
                notas = notes
            )
            repository.updateWod(updatedWod)
            
            // Actualizar widget
            WodWidgetProvider.updateAllWidgets(getApplication())
        }
    }
}

