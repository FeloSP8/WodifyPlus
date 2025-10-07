package com.example.wodifyplus.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wodifyplus.data.local.WodDatabase
import com.example.wodifyplus.data.local.entities.ActivityConfigEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val activityConfigDao = WodDatabase.getDatabase(application).activityConfigDao()
    
    private val _activityConfigs = MutableStateFlow<List<ActivityConfigEntity>>(emptyList())
    val activityConfigs: StateFlow<List<ActivityConfigEntity>> = _activityConfigs.asStateFlow()
    
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
}

