package com.example.wodifyplus.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.wodifyplus.data.local.WodDatabase
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.data.repository.WodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val repository: WodRepository,
    private val activityConfigDao: com.example.wodifyplus.data.local.ActivityConfigDao,
    private val wodParser: com.example.wodifyplus.data.parser.WodParser
) : AndroidViewModel(application) {

    init {
        // Inicializar Python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(application))
        }
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun resetState() {
        _uiState.value = HomeUiState.Idle
    }

    fun fetchWods() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            try {
                val result = withContext(Dispatchers.IO) {
                    val py = Python.getInstance()
                    val module = py.getModule("wod_scraper")
                    module.callAttr("main").toString()
                }

                // Parsear resultado y guardar en BD
                val wodsGuardados = parseAndSaveWods(result)

                // Mensaje limpio de éxito
                val mensaje = if (wodsGuardados > 0) {
                    "Se encontraron $wodsGuardados WODs en total.\n\n¡Listos para seleccionar!"
                } else {
                    "No se encontraron WODs. Se han cargado WODs de prueba."
                }

                _uiState.value = HomeUiState.Success(mensaje)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private suspend fun parseAndSaveWods(result: String): Int {
        try {
            val wods = wodParser.parseWods(result).toMutableList()
            
            if (wods.isEmpty()) {
                android.util.Log.e("WodParser", "No se encontraron WODs en el resultado, usando modo prueba")
                val sampleWods = createSampleWods()
                repository.updateOrInsertScrapedWods(sampleWods, listOf("CrossFit DB", "N8"))
                val customWods = createCustomActivityWods(sampleWods)
                repository.updateOrInsertCustomWods(customWods)
                return sampleWods.size + customWods.size
            }
            
            // Guardar en la base de datos
            android.util.Log.d("WodParser", "Total WODs a guardar: ${wods.size}")
            
            // IMPORTANTE: Usar merge inteligente que preserva WODs completados y seleccionados
            repository.updateOrInsertScrapedWods(wods, listOf("CrossFit DB", "N8"))
            
            // Crear/actualizar WODs para actividades personalizadas
            val customWods = createCustomActivityWods(wods)
            if (customWods.isNotEmpty()) {
                repository.updateOrInsertCustomWods(customWods)
            }
            
            return wods.size + customWods.size
        } catch (e: Exception) {
            android.util.Log.e("WodParser", "Error en parseAndSaveWods", e)
            val sampleWods = createSampleWods()
            repository.updateOrInsertScrapedWods(sampleWods, listOf("CrossFit DB", "N8"))
            val customWods = createCustomActivityWods(sampleWods)
            if (customWods.isNotEmpty()) {
                repository.updateOrInsertCustomWods(customWods)
            }
            return sampleWods.size + customWods.size
        }
    }

    private suspend fun createCustomActivityWods(scrapedWods: List<Wod>): List<Wod> {
        val customWods = mutableListOf<Wod>()
        
        android.util.Log.d("CustomWods", "=== Creando WODs personalizados ===")
        
        // Obtener TODAS las actividades (no solo las activas, el filtro se hace en getAllActiveConfigs)
        val allConfigs = activityConfigDao.getAllConfigs().first()
        android.util.Log.d("CustomWods", "Total configuraciones: ${allConfigs.size}")
        allConfigs.forEach { 
            android.util.Log.d("CustomWods", "  - ${it.name}: enabled=${it.isEnabled}, isDefault=${it.isDefault}")
        }
        
        // Filtrar: activas Y que no sean CrossFit DB ni N8
        val customConfigs = allConfigs
            .filter { it.isEnabled }
            .filter { it.name != "CrossFit DB" && it.name != "N8" }
        
        android.util.Log.d("CustomWods", "Configuraciones personalizadas activas: ${customConfigs.size}")
        customConfigs.forEach {
            android.util.Log.d("CustomWods", "  - ${it.name}: L=${it.monday} M=${it.tuesday} X=${it.wednesday} J=${it.thursday} V=${it.friday} S=${it.saturday} D=${it.sunday}")
        }
        
        if (customConfigs.isEmpty()) {
            android.util.Log.d("CustomWods", "No hay actividades personalizadas activas")
            return customWods
        }
        
        // Generar fechas desde hoy hasta el domingo de esta semana
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val currentHour = now.hour
        
        // Si son más de las 12 del mediodía, mostrar la semana siguiente
        val startDate = if (currentHour >= 12) {
            today.plusDays(1)
        } else {
            today
        }
        
        // Calcular el domingo de la semana actual
        val sundayOfWeek = startDate.with(DayOfWeek.SUNDAY)
        
        // Generar fechas desde startDate hasta el domingo
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate
        while (!currentDate.isAfter(sundayOfWeek)) {
            dates.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }
        
        android.util.Log.d("CustomWods", "Fechas generadas: ${dates.size} (desde $startDate hasta $sundayOfWeek)")
        dates.forEach { 
            android.util.Log.d("CustomWods", "  - $it (${it.dayOfWeek})")
        }
        
        // Para cada fecha y cada actividad personalizada, crear un WOD si el día está configurado
        dates.forEach { date ->
            val dayOfWeek = date.dayOfWeek
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                .replaceFirstChar { it.uppercase() }
            
            customConfigs.forEach { config ->
                // Verificar si esta actividad está configurada para este día
                val isDayEnabled = when (dayOfWeek) {
                    DayOfWeek.MONDAY -> config.monday
                    DayOfWeek.TUESDAY -> config.tuesday
                    DayOfWeek.WEDNESDAY -> config.wednesday
                    DayOfWeek.THURSDAY -> config.thursday
                    DayOfWeek.FRIDAY -> config.friday
                    DayOfWeek.SATURDAY -> config.saturday
                    DayOfWeek.SUNDAY -> config.sunday
                }
                
                android.util.Log.d("CustomWods", "  ${config.name} en $dayName ($date): isDayEnabled=$isDayEnabled")
                
                if (isDayEnabled) {
                    val wod = Wod(
                        fecha = date,
                        diaSemana = dayName,
                        gimnasio = config.name,
                        contenido = "Sesión de ${config.name}\n\nConfigurado para $dayName",
                        contenidoHtml = "<div><strong>Sesión de ${config.name}</strong></div><div>Configurado para $dayName</div>"
                    )
                    customWods.add(wod)
                    android.util.Log.d("CustomWods", "    ✓ WOD creado: ${config.name} - $dayName $date")
                } else {
                    android.util.Log.d("CustomWods", "    ✗ WOD NO creado: ${config.name} - $dayName $date (día no habilitado)")
                }
            }
        }
        
        android.util.Log.d("CustomWods", "=== Total WODs personalizados creados: ${customWods.size} ===")
        return customWods
    }
    
    private fun createSampleWods(): List<Wod> {
        val today = LocalDate.now()
        val wods = mutableListOf<Wod>()

        for (i in 0..6) {
            val date = today.plusDays(i.toLong())
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES"))

            // WOD de CrossFit DB
            wods.add(
                Wod(
                    fecha = date,
                    diaSemana = dayName.replaceFirstChar { it.uppercase() },
                    gimnasio = "CrossFit DB",
                    contenido = "STRENGTH\n5x5 Back Squat\n\nMETCON\n21-15-9\nThrusters\nPull-ups",
                    contenidoHtml = ""
                )
            )

            // WOD de N8
            wods.add(
                Wod(
                    fecha = date,
                    diaSemana = dayName.replaceFirstChar { it.uppercase() },
                    gimnasio = "N8",
                    contenido = "AMRAP 20'\n10 Box Jumps\n15 Wall Balls\n20 Double Unders",
                    contenidoHtml = ""
                )
            )
        }

        return wods
    }
}

sealed class HomeUiState {
    object Idle : HomeUiState()
    object Loading : HomeUiState()
    data class Success(val message: String) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

