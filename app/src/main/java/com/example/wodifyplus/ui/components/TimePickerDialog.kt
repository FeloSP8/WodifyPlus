package com.example.wodifyplus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalTime

@Composable
fun TimePickerDialog(
    initialTime: LocalTime? = null,
    preferredHour: Int = 18,
    preferredMinute: Int = 0,
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime?) -> Unit
) {
    var selectedHour by remember { 
        mutableStateOf(initialTime?.hour ?: preferredHour) 
    }
    var selectedMinute by remember { 
        mutableStateOf(initialTime?.minute ?: preferredMinute) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar hora de entrenamiento") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = String.format("%02d:%02d", selectedHour, selectedMinute),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de hora
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Hora:")
                    Slider(
                        value = selectedHour.toFloat(),
                        onValueChange = { selectedHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 0, // Sin steps para permitir todos los valores
                        modifier = Modifier.weight(1f)
                    )
                    Text(String.format("%02d", selectedHour))
                }

                // Selector de minutos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Minutos:")
                    Slider(
                        value = selectedMinute.toFloat(),
                        onValueChange = { selectedMinute = it.toInt() },
                        valueRange = 0f..59f,
                        steps = 0, // Sin steps para permitir todos los valores
                        modifier = Modifier.weight(1f)
                    )
                    Text(String.format("%02d", selectedMinute))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val time = LocalTime.of(selectedHour, selectedMinute)
                    onTimeSelected(time)
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onTimeSelected(null) // Sin hora
                    }
                ) {
                    Text("Sin hora")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

