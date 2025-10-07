package com.example.wodifyplus.ui.screens.selection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wodifyplus.data.local.WodDatabase
import com.example.wodifyplus.data.local.entities.ActivityConfigEntity
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.data.preferences.PreferencesManager
import com.example.wodifyplus.data.repository.WodRepository
import com.example.wodifyplus.notifications.NotificationScheduler
import com.example.wodifyplus.widget.WodWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class SelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WodRepository
    private val activityConfigDao = WodDatabase.getDatabase(application).activityConfigDao()
    private val preferencesManager = PreferencesManager(application)

    init {
        val wodDao = WodDatabase.getDatabase(application).wodDao()
        repository = WodRepository(wodDao)
    }

    private val _wodsByDate = MutableStateFlow<Map<LocalDate, List<Wod>>>(emptyMap())
    val wodsByDate: StateFlow<Map<LocalDate, List<Wod>>> = _wodsByDate.asStateFlow()
    
    private val _activityConfigs = MutableStateFlow<List<ActivityConfigEntity>>(emptyList())

    fun loadWods() {
        viewModelScope.launch {
            // Combinar WODs y configuraciones para filtrar
            combine(
                repository.allWods,
                activityConfigDao.getAllActiveConfigs()
            ) { wods, configs ->
                _activityConfigs.value = configs
                filterWodsByConfig(wods, configs)
            }.collect { filteredWods ->
                _wodsByDate.value = filteredWods.groupBy { it.fecha }
            }
        }
    }
    
    private fun filterWodsByConfig(
        wods: List<Wod>,
        configs: List<ActivityConfigEntity>
    ): List<Wod> {
        return wods.filter { wod ->
            val config = configs.find { it.name == wod.gimnasio }
            if (config == null || !config.isEnabled) return@filter false
            
            // Verificar si el día está configurado
            isDayEnabled(wod.fecha.dayOfWeek, config)
        }
    }
    
    private fun isDayEnabled(dayOfWeek: DayOfWeek, config: ActivityConfigEntity): Boolean {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> config.monday
            DayOfWeek.TUESDAY -> config.tuesday
            DayOfWeek.WEDNESDAY -> config.wednesday
            DayOfWeek.THURSDAY -> config.thursday
            DayOfWeek.FRIDAY -> config.friday
            DayOfWeek.SATURDAY -> config.saturday
            DayOfWeek.SUNDAY -> config.sunday
        }
    }
    
    fun getPreferredTimeForActivity(activityName: String): Pair<Int, Int>? {
        val config = _activityConfigs.value.find { it.name == activityName }
        return config?.let { it.preferredHour to it.preferredMinute }
    }

    fun selectWod(wod: Wod, hora: LocalTime?) {
        viewModelScope.launch {
            val updatedWod = wod.copy(
                seleccionado = true,
                hora = hora,
                notificacionActiva = hora != null
            )
            repository.updateWod(updatedWod)
            
            // Programar notificación si tiene hora
            if (hora != null) {
                val minutesBefore = preferencesManager.notificationMinutesBefore.first()
                NotificationScheduler.scheduleWodReminder(
                    getApplication(),
                    updatedWod,
                    minutesBefore
                )
            }
            
            // Actualizar widget
            WodWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deselectWod(wod: Wod) {
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
}

