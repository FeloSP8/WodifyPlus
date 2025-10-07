package com.example.wodifyplus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wodifyplus.data.models.Wod

@Composable
fun WodComparisonCard(
    wod: Wod,
    onSelect: () -> Unit,
    onDeselect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (wod.seleccionado)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header con gimnasio y botón de selección
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wod.gimnasio,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = if (wod.seleccionado) onDeselect else onSelect
                ) {
                    Icon(
                        imageVector = if (wod.seleccionado)
                            Icons.Filled.CheckCircle
                        else
                            Icons.Outlined.Circle,
                        contentDescription = if (wod.seleccionado) "Deseleccionar" else "Seleccionar",
                        tint = if (wod.seleccionado)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contenido del WOD
            Text(
                text = wod.contenido,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

