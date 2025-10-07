package com.example.wodifyplus.ui.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.data.preferences.PreferencesManager
import com.example.wodifyplus.ui.components.TimePickerDialog
import com.example.wodifyplus.ui.components.CompleteActivityDialog
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val preferredHour by preferencesManager.preferredHour.collectAsState(initial = 18)
    val preferredMinute by preferencesManager.preferredMinute.collectAsState(initial = 0)
    
    val selectedWods by viewModel.selectedWods.collectAsState()
    var editingWod by remember { mutableStateOf<Wod?>(null) }
    var completingWod by remember { mutableStateOf<Wod?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadSelectedWods()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Calendario Semanal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (selectedWods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No has seleccionado ningún WOD",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(selectedWods) { wod ->
                    CalendarWodCard(
                        wod = wod,
                        onEditTime = { editingWod = wod },
                        onDelete = { viewModel.deleteWodFromCalendar(wod) },
                        onComplete = { completingWod = wod }
                    )
                }
            }
        }
    }

    // Time Picker Dialog para editar hora
    editingWod?.let { wod ->
        TimePickerDialog(
            initialTime = wod.hora,
            preferredHour = preferredHour,
            preferredMinute = preferredMinute,
            onDismiss = { editingWod = null },
            onTimeSelected = { time ->
                viewModel.updateWodTime(wod, time)
                editingWod = null
            }
        )
    }
    
    // Complete Activity Dialog
    completingWod?.let { wod ->
        CompleteActivityDialog(
            wod = wod,
            onDismiss = { completingWod = null },
            onComplete = { calories, distance, duration, notes ->
                viewModel.completeWod(wod, calories, distance, duration, notes)
                completingWod = null
            }
        )
    }
}

@Composable
private fun CalendarWodCard(
    wod: Wod,
    onEditTime: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (wod.gimnasio == "CrossFit DB")
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${wod.diaSemana} ${wod.fecha.format(DateTimeFormatter.ofPattern("dd/MM"))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = wod.gimnasio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = wod.hora?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Sin hora",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onEditTime) {
                    Text("Cambiar hora")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Contenido del WOD (resumido)
            Text(
                text = wod.contenido.take(200) + if (wod.contenido.length > 200) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            // Botón de completar
            if (!wod.completada) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Marcar como completada")
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Completada",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                // Mostrar datos de la actividad completada
                wod.fechaCompletado?.let {
                    Text(
                        "Completada el ${it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    wod.caloriasQuemadas?.let {
                        Text("🔥 $it kcal", style = MaterialTheme.typography.bodySmall)
                    }
                    wod.distanciaKm?.let {
                        Text("📍 $it km", style = MaterialTheme.typography.bodySmall)
                    }
                    wod.duracionMinutos?.let {
                        Text("⏱️ $it min", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

