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
import java.time.LocalDateTime

class NextActivityViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: WodRepository
    
    init {
        val wodDao = WodDatabase.getDatabase(application).wodDao()
        repository = WodRepository(wodDao)
    }
    
    private val _nextActivity = MutableStateFlow<Wod?>(null)
    val nextActivity: StateFlow<Wod?> = _nextActivity.asStateFlow()
    
    init {
        loadNextActivity()
    }
    
    fun loadNextActivity() {
        viewModelScope.launch {
            repository.selectedWods.collect { wods ->
                // Buscar el próximo WOD con hora
                val now = LocalDateTime.now()
                val nextWod = wods
                    .filter { it.hora != null && !it.completada }
                    .mapNotNull { wod ->
                        wod.hora?.let { hora ->
                            val wodDateTime = wod.fecha.atTime(hora)
                            if (wodDateTime.isAfter(now)) wod to wodDateTime else null
                        }
                    }
                    .minByOrNull { it.second }
                    ?.first
                
                _nextActivity.value = nextWod
            }
        }
    }
}

