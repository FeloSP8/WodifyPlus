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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.ZoneId

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SelectionViewModel @Inject constructor(
    application: Application,
    private val repository: WodRepository,
    private val activityConfigDao: com.example.wodifyplus.data.local.ActivityConfigDao,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

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
        android.util.Log.d("SelectionViewModel", "Filtering ${wods.size} WODs with ${configs.size} configs")
        configs.forEach { config ->
            android.util.Log.d("SelectionViewModel", "Config: ${config.name}, enabled: ${config.isEnabled}, sunday: ${config.sunday}")
        }
        
        // Filtrar por rango de fechas (usar zona horaria del sistema)
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val today = now.toLocalDate()
        val currentDayOfWeek = today.dayOfWeek
        val currentHour = now.hour

        android.util.Log.d("SelectionViewModel", "Current time: $now (hour: $currentHour, day: $currentDayOfWeek)")

        // Determinar la semana activa
        val (startDate, endDate) = if (currentDayOfWeek == DayOfWeek.SUNDAY && currentHour >= 12) {
            // Domingo después de las 12:00 → próxima semana (lunes a domingo)
            val nextMonday = today.plusDays(1)
            val nextSunday = nextMonday.plusDays(6)
            nextMonday to nextSunday
        } else {
            // Cualquier otro día → semana actual (desde el lunes hasta el domingo)
            val mondayOfWeek = today.with(DayOfWeek.MONDAY)
            val sundayOfWeek = mondayOfWeek.plusDays(6)
            mondayOfWeek to sundayOfWeek
        }

        android.util.Log.d("SelectionViewModel", "Date range: $startDate to $endDate (today: $today ${currentDayOfWeek}, hour: $currentHour)")
        
        return wods.filter { wod ->
            // Filtrar por rango de fechas
            if (wod.fecha.isBefore(startDate) || wod.fecha.isAfter(endDate)) {
                android.util.Log.d("SelectionViewModel", "Filtered out by date: ${wod.gimnasio} - ${wod.fecha} (outside range $startDate to $endDate)")
                return@filter false
            }
            
            val config = configs.find { it.name == wod.gimnasio }
            android.util.Log.d("SelectionViewModel", "WOD: ${wod.gimnasio} - ${wod.fecha.dayOfWeek}, config found: ${config != null}")
            
            if (config == null || !config.isEnabled) {
                android.util.Log.d("SelectionViewModel", "Filtered out: ${wod.gimnasio} (no config or disabled)")
                return@filter false
            }
            
            val dayEnabled = isDayEnabled(wod.fecha.dayOfWeek, config)
            android.util.Log.d("SelectionViewModel", "Day ${wod.fecha.dayOfWeek} enabled for ${wod.gimnasio}: $dayEnabled")
            dayEnabled
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

