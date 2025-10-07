package com.example.wodifyplus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.wodifyplus.data.models.Wod
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteActivityDialog(
    wod: Wod,
    onDismiss: () -> Unit,
    onComplete: (calorias: Int?, distancia: Double?, duracion: Int?, notas: String?) -> Unit
) {
    var calories by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Completar actividad") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "${wod.gimnasio} - ${wod.diaSemana}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Divider()
                
                // Calorías
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { char -> char.isDigit() } },
                    label = { Text("Calorías quemadas") },
                    placeholder = { Text("Ej: 350") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Distancia
                OutlinedTextField(
                    value = distance,
                    onValueChange = { 
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            distance = it
                        }
                    },
                    label = { Text("Distancia (km)") },
                    placeholder = { Text("Ej: 5.2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Duración
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter { char -> char.isDigit() } },
                    label = { Text("Duración (minutos)") },
                    placeholder = { Text("Ej: 45") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Notas
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    placeholder = { Text("¿Cómo te fue?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val caloriesInt = calories.toIntOrNull()
                    val distanceDouble = distance.toDoubleOrNull()
                    val durationInt = duration.toIntOrNull()
                    val notesStr = notes.ifBlank { null }
                    
                    onComplete(caloriesInt, distanceDouble, durationInt, notesStr)
                }
            ) {
                Text("Marcar como completada")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


