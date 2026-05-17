package com.example.wodifyplus.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wodifyplus.data.local.WodDatabase
import com.example.wodifyplus.data.local.entities.ActivityConfigEntity
import com.example.wodifyplus.data.repository.WodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val activityConfigDao: com.example.wodifyplus.data.local.ActivityConfigDao,
    private val repository: WodRepository,
    private val exportManager: com.example.wodifyplus.data.export.ExportManager
) : AndroidViewModel(application) {

    fun exportData() {
        viewModelScope.launch {
            repository.allWods.first().let { wods ->
                val file = exportManager.exportWodsToCsv(wods)
                file?.let { exportManager.shareFile(it) }
            }
        }
    }
    
    private val _activityConfigs = MutableStateFlow<List<ActivityConfigEntity>>(emptyList())
    val activityConfigs: StateFlow<List<ActivityConfigEntity>> = _activityConfigs.asStateFlow()
    
    private val _clearDataState = MutableStateFlow<ClearDataState>(ClearDataState.Idle)
    val clearDataState: StateFlow<ClearDataState> = _clearDataState.asStateFlow()
    
    init {
        loadConfigs()
        ensureDefaultConfigs()
    }
    
    private fun ensureDefaultConfigs() {
        viewModelScope.launch {
            // Verificar si existen las configuraciones por defecto
            val crossfitDB = activityConfigDao.getConfigByName("CrossFit DB")
            val n8 = activityConfigDao.getConfigByName("N8")
            
            if (crossfitDB == null) {
                // Insertar CrossFit DB por defecto
                activityConfigDao.insertConfig(
                    ActivityConfigEntity(
                        name = "CrossFit DB",
                        monday = true,
                        tuesday = true,
                        wednesday = true,
                        thursday = true,
                        friday = true,
                        saturday = true,
                        sunday = false,
                        preferredHour = 18,
                        preferredMinute = 0,
                        isEnabled = true,
                        isDefault = true
                    )
                )
            }
            
            if (n8 == null) {
                // Insertar N8 por defecto
                activityConfigDao.insertConfig(
                    ActivityConfigEntity(
                        name = "N8",
                        monday = true,
                        tuesday = true,
                        wednesday = true,
                        thursday = true,
                        friday = true,
                        saturday = true,
                        sunday = false,
                        preferredHour = 18,
                        preferredMinute = 0,
                        isEnabled = true,
                        isDefault = true
                    )
                )
            }
        }
    }
    
    private fun loadConfigs() {
        viewModelScope.launch {
            activityConfigDao.getAllConfigs().collect { configs ->
                _activityConfigs.value = configs
            }
        }
    }
    
    fun saveConfig(config: ActivityConfigEntity) {
        viewModelScope.launch {
            if (config.id == 0) {
                activityConfigDao.insertConfig(config)
            } else {
                activityConfigDao.updateConfig(config)
            }
        }
    }
    
    fun deleteConfig(config: ActivityConfigEntity) {
        viewModelScope.launch {
            if (!config.isDefault) {
                activityConfigDao.deleteConfig(config)
            }
        }
    }
    
    fun toggleEnabled(config: ActivityConfigEntity) {
        viewModelScope.launch {
            activityConfigDao.updateConfig(config.copy(isEnabled = !config.isEnabled))
        }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            _clearDataState.value = ClearDataState.Loading
            try {
                repository.deleteAllWods()
                _clearDataState.value = ClearDataState.Success("Todos los datos han sido eliminados")
            } catch (e: Exception) {
                _clearDataState.value = ClearDataState.Error("Error al eliminar datos: ${e.message}")
            }
        }
    }
    
    fun resetClearDataState() {
        _clearDataState.value = ClearDataState.Idle
    }
}

sealed class ClearDataState {
    object Idle : ClearDataState()
    object Loading : ClearDataState()
    data class Success(val message: String) : ClearDataState()
    data class Error(val message: String) : ClearDataState()
}

