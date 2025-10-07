package com.example.wodifyplus.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wodifyplus.data.local.entities.ActivityConfigEntity
import com.example.wodifyplus.data.preferences.PreferencesManager
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val activityConfigs by viewModel.activityConfigs.collectAsState()
    var editingConfig by remember { mutableStateOf<ActivityConfigEntity?>(null) }
    var showNewActivityDialog by remember { mutableStateOf(false) }
    val notificationMinutesBefore by preferencesManager.notificationMinutesBefore.collectAsState(initial = 60)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showNewActivityDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir actividad")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Notificaciones",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Text(
                            "Tiempo de anticipación: ${notificationMinutesBefore} minutos",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Slider(
                            value = notificationMinutesBefore.toFloat(),
                            onValueChange = { 
                                scope.launch {
                                    preferencesManager.setNotificationMinutesBefore(it.roundToInt())
                                }
                            },
                            valueRange = 15f..120f,
                            steps = 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            "Recibirás una notificación ${notificationMinutesBefore} minutos antes de tu actividad programada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Gestión de actividades",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Activa/desactiva actividades según tus necesidades. Las actividades desactivadas no aparecerán en la selección de WODs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tus actividades personalizadas aparecerán automáticamente cuando obtengas los WODs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(activityConfigs) { config ->
                ActivityConfigCard(
                    config = config,
                    onEdit = { editingConfig = config },
                    onToggle = { viewModel.toggleEnabled(config) },
                    onDelete = { viewModel.deleteConfig(config) }
                )
            }
        }
    }
    
    // Diálogo para nueva actividad
    if (showNewActivityDialog) {
        ActivityConfigDialog(
            config = null,
            onDismiss = { showNewActivityDialog = false },
            onSave = { newConfig ->
                viewModel.saveConfig(newConfig)
                showNewActivityDialog = false
            }
        )
    }
    
    // Diálogo para editar actividad
    editingConfig?.let { config ->
        ActivityConfigDialog(
            config = config,
            onDismiss = { editingConfig = null },
            onSave = { updatedConfig ->
                viewModel.saveConfig(updatedConfig)
                editingConfig = null
            }
        )
    }
}

@Composable
private fun ActivityConfigCard(
    config: ActivityConfigEntity,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (config.isEnabled)
                MaterialTheme.colorScheme.surfaceContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            config.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (config.isEnabled) 
                                MaterialTheme.colorScheme.onSurface 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        if (config.isDefault) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    "Por defecto",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Días
                    Text(
                        getDaysText(config),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    // Hora
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            String.format("%02d:%02d", config.preferredHour, config.preferredMinute),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Switch para activar/desactivar
                    Switch(
                        checked = config.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    if (!config.isDefault) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getDaysText(config: ActivityConfigEntity): String {
    val days = mutableListOf<String>()
    if (config.monday) days.add("L")
    if (config.tuesday) days.add("M")
    if (config.wednesday) days.add("X")
    if (config.thursday) days.add("J")
    if (config.friday) days.add("V")
    if (config.saturday) days.add("S")
    if (config.sunday) days.add("D")
    return if (days.isNotEmpty()) days.joinToString(", ") else "Sin días configurados"
}

