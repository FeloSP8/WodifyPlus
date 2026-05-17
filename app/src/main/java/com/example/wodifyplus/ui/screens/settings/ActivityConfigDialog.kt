package com.example.wodifyplus.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.wodifyplus.data.local.entities.ActivityConfigEntity

@Composable
fun ActivityConfigDialog(
    config: ActivityConfigEntity?,
    onDismiss: () -> Unit,
    onSave: (ActivityConfigEntity) -> Unit
) {
    var name by remember { mutableStateOf(config?.name ?: "") }
    var monday by remember { mutableStateOf(config?.monday ?: true) }
    var tuesday by remember { mutableStateOf(config?.tuesday ?: true) }
    var wednesday by remember { mutableStateOf(config?.wednesday ?: true) }
    var thursday by remember { mutableStateOf(config?.thursday ?: true) }
    var friday by remember { mutableStateOf(config?.friday ?: true) }
    var saturday by remember { mutableStateOf(config?.saturday ?: false) }
    var sunday by remember { mutableStateOf(config?.sunday ?: false) }
    var hour by remember { mutableStateOf(config?.preferredHour ?: 18) }
    // Redondear minutos al múltiplo de 15 más cercano
    var minute by remember {
        val rawMinute = config?.preferredMinute ?: 0
        val roundedMinute = ((rawMinute + 7.5) / 15).toInt() * 15
        mutableStateOf(if (roundedMinute >= 60) 45 else roundedMinute)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (config == null) "Nueva actividad" else "Editar actividad")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nombre
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("Ej: Gimnasio, Club OCR, Running...") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = config?.isDefault != true,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    supportingText = if (config?.isDefault == true) {
                        { Text("Las actividades por defecto no se pueden renombrar") }
                    } else null
                )
                
                Divider()
                
                // Días de la semana
                Text(
                    "Días de asistencia",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayCheckbox("Lunes", monday) { monday = it }
                    DayCheckbox("Martes", tuesday) { tuesday = it }
                    DayCheckbox("Miércoles", wednesday) { wednesday = it }
                    DayCheckbox("Jueves", thursday) { thursday = it }
                    DayCheckbox("Viernes", friday) { friday = it }
                    DayCheckbox("Sábado", saturday) { saturday = it }
                    DayCheckbox("Domingo", sunday) { sunday = it }
                }
                
                Divider()
                
                // Hora preferida
                Text(
                    "Hora preferida",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            String.format("%02d:%02d", hour, minute),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Selector de hora
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Hora:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(70.dp)
                        )
                        Slider(
                            value = hour.toFloat(),
                            onValueChange = { hour = it.toInt() },
                            valueRange = 0f..23f,
                            steps = 22,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            String.format("%02d", hour),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                }

                // Selector de minutos (15 en 15)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Minutos:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(70.dp)
                        )
                        Slider(
                            value = (minute / 15).toFloat(),
                            onValueChange = { minute = (it.toInt() * 15) },
                            valueRange = 0f..3f,
                            steps = 2,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            String.format("%02d", minute),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                    Text(
                        "Opciones: 00, 15, 30, 45",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 70.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newConfig = ActivityConfigEntity(
                        id = config?.id ?: 0,
                        name = name,
                        monday = monday,
                        tuesday = tuesday,
                        wednesday = wednesday,
                        thursday = thursday,
                        friday = friday,
                        saturday = saturday,
                        sunday = sunday,
                        preferredHour = hour,
                        preferredMinute = minute,
                        isEnabled = config?.isEnabled ?: true,
                        isDefault = config?.isDefault ?: false
                    )
                    onSave(newConfig)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DayCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

