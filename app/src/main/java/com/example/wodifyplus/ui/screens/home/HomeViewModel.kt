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
import java.time.format.TextStyle
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WodRepository
    private val activityConfigDao = WodDatabase.getDatabase(application).activityConfigDao()

    init {
        val wodDao = WodDatabase.getDatabase(application).wodDao()
        repository = WodRepository(wodDao)

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
            // Extraer el JSON del resultado
            val jsonStartMarker = "JSON_DATA_START"
            val jsonEndMarker = "JSON_DATA_END"
            
            val jsonStart = result.indexOf(jsonStartMarker)
            val jsonEnd = result.indexOf(jsonEndMarker)
            
            if (jsonStart == -1 || jsonEnd == -1) {
                android.util.Log.e("WodParser", "No se encontraron marcadores JSON")
                // Si no hay JSON, crear WODs de prueba
                val wods = createSampleWods()
                repository.insertWods(wods)
                return wods.size
            }
            
            val jsonString = result.substring(jsonStart + jsonStartMarker.length, jsonEnd).trim()
            
            // LOG: Imprimir JSON completo
            android.util.Log.d("WodParser", "=== JSON COMPLETO ===")
            android.util.Log.d("WodParser", jsonString)
            android.util.Log.d("WodParser", "=== FIN JSON ===")
            
            // Usar el parser JSON nativo de Android
            val wods = mutableListOf<Wod>()
            
            try {
                val jsonObject = org.json.JSONObject(jsonString)
                
                // Parsear WODs de N8
                if (jsonObject.has("wods_n8")) {
                    val wodsN8Array = jsonObject.getJSONArray("wods_n8")
                    android.util.Log.d("WodParser", "N8: Encontrados ${wodsN8Array.length()} WODs")
                    
                    for (i in 0 until wodsN8Array.length()) {
                        val wodObj = wodsN8Array.getJSONObject(i)
                        android.util.Log.d("WodParser", "N8 WOD $i:")
                        android.util.Log.d("WodParser", "  Fecha: ${wodObj.optString("fecha_iso", wodObj.optString("fecha"))}")
                        android.util.Log.d("WodParser", "  Dia: ${wodObj.optString("dia_semana")}")
                        android.util.Log.d("WodParser", "  Contenido: ${wodObj.optString("contenido").take(50)}...")
                        
                        parseWodFromJson(wodObj, "N8")?.let { 
                            wods.add(it)
                            android.util.Log.d("WodParser", "  ✓ WOD agregado")
                        } ?: android.util.Log.e("WodParser", "  ✗ Error parseando WOD")
                    }
                }
                
                // Parsear WODs de CrossFit DB
                if (jsonObject.has("wods_crossfitdb")) {
                    val wodsCrossfit = jsonObject.getJSONArray("wods_crossfitdb")
                    android.util.Log.d("WodParser", "CrossFit DB: Encontrados ${wodsCrossfit.length()} WODs")
                    
                    for (i in 0 until wodsCrossfit.length()) {
                        val wodObj = wodsCrossfit.getJSONObject(i)
                        android.util.Log.d("WodParser", "CF DB WOD $i:")
                        android.util.Log.d("WodParser", "  Fecha: ${wodObj.optString("fecha_iso", wodObj.optString("fecha"))}")
                        android.util.Log.d("WodParser", "  Dia: ${wodObj.optString("dia_semana")}")
                        
                        parseWodFromJson(wodObj, "CrossFit DB")?.let { 
                            wods.add(it)
                            android.util.Log.d("WodParser", "  ✓ WOD agregado")
                        } ?: android.util.Log.e("WodParser", "  ✗ Error parseando WOD")
                    }
                }
            } catch (e: Exception) {
                // Error parseando JSON
                android.util.Log.e("WodParser", "Error parseando JSON: ${e.message}")
                e.printStackTrace()
            }
            
            // Guardar en la base de datos
            android.util.Log.d("WodParser", "Total WODs a guardar: ${wods.size}")
            if (wods.isNotEmpty()) {
                // IMPORTANTE: Limpiar WODs antiguos antes de insertar nuevos
                android.util.Log.d("WodParser", "Limpiando WODs antiguos...")
                repository.deleteAllWods()
                
                // Insertar nuevos WODs
                repository.insertWods(wods)
                android.util.Log.d("WodParser", "✓ WODs guardados en BD")
                
                // Crear WODs para actividades personalizadas
                val customWods = createCustomActivityWods(wods)
                if (customWods.isNotEmpty()) {
                    repository.insertWods(customWods)
                    android.util.Log.d("WodParser", "✓ ${customWods.size} WODs personalizados creados")
                }
                
                return wods.size + customWods.size
            }
            
            android.util.Log.w("WodParser", "No hay WODs para guardar")
            return 0
        } catch (e: Exception) {
            // En caso de error, crear WODs de prueba
            android.util.Log.e("WodParser", "Error en parseAndSaveWods", e)
            val wods = createSampleWods()
            repository.deleteAllWods()
            repository.insertWods(wods)
            
            // También crear WODs personalizados para los de prueba
            val customWods = createCustomActivityWods(wods)
            if (customWods.isNotEmpty()) {
                repository.insertWods(customWods)
                android.util.Log.d("WodParser", "✓ ${customWods.size} WODs personalizados creados (modo prueba)")
            }
            
            return wods.size + customWods.size
        }
    }
    
    private fun parseWodFromJson(jsonObject: org.json.JSONObject, gimnasioName: String): Wod? {
        try {
            // Obtener fecha (probar diferentes campos)
            val fechaStr = when {
                jsonObject.has("fecha_iso") -> jsonObject.getString("fecha_iso")
                jsonObject.has("fecha") -> jsonObject.getString("fecha")
                else -> return null
            }
            
            // Parsear fecha
            val fecha = try {
                val fechaLimpia = fechaStr.substringBefore(" ").substringBefore("T")
                LocalDate.parse(fechaLimpia)
            } catch (e: Exception) {
                LocalDate.now()
            }
            
            val diaSemana = jsonObject.optString("dia_semana", "")
            val contenido = jsonObject.optString("contenido", "")
            val contenidoHtml = jsonObject.optString("contenido_html", "")
            
            return Wod(
                fecha = fecha,
                diaSemana = diaSemana,
                gimnasio = gimnasioName,
                contenido = contenido,
                contenidoHtml = contenidoHtml
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
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
        
        // Obtener todas las fechas únicas de los WODs scrapeados
        val dates = scrapedWods.map { it.fecha }.distinct().sorted()
        android.util.Log.d("CustomWods", "Fechas de WODs scrapeados: ${dates.size}")
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
                        contenido = "Sesión de ${config.name}\n\nConfigurado para ${dayName}s",
                        contenidoHtml = "<div><strong>Sesión de ${config.name}</strong></div><div>Configurado para ${dayName}s</div>"
                    )
                    customWods.add(wod)
                    android.util.Log.d("CustomWods", "    ✓ WOD creado: ${config.name} - $dayName $date")
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

