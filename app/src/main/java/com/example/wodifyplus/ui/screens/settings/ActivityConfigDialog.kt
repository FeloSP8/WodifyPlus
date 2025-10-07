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
    var minute by remember { mutableStateOf(config?.preferredMinute ?: 0) }
    
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
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                    Text(
                        String.format("%02d:%02d", hour, minute),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                // Sliders para hora
                Text("Hora:", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = hour.toFloat(),
                    onValueChange = { hour = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 0 // Sin steps para permitir todos los valores
                )
                
                Text("Minutos:", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = minute.toFloat(),
                    onValueChange = { minute = it.toInt() },
                    valueRange = 0f..59f,
                    steps = 0 // Sin steps para permitir todos los valores
                )
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

