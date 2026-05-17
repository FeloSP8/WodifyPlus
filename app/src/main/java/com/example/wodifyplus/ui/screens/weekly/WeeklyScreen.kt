package com.example.wodifyplus.ui.screens.weekly

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
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.data.preferences.PreferencesManager
import com.example.wodifyplus.ui.components.TimePickerDialog
import com.example.wodifyplus.ui.components.CompleteActivityDialog
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScreen(
    onNavigateBack: () -> Unit,
    viewModel: WeeklyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val preferredHour by preferencesManager.preferredHour.collectAsState(initial = 18)
    val preferredMinute by preferencesManager.preferredMinute.collectAsState(initial = 0)
    
    val weeklyWods by viewModel.weeklyWods.collectAsState()
    var editingWod by remember { mutableStateOf<Wod?>(null) }
    var completingWod by remember { mutableStateOf<Wod?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadWeeklyWods()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Semana") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (weeklyWods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No tienes actividades planificadas para esta semana",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                // Header de la semana
                item {
                    WeekHeader()
                }
                
                // Agrupar WODs por día
                val wodsByDay = weeklyWods.groupBy { it.fecha }
                    .toSortedMap()
                
                items(wodsByDay.entries.toList()) { (date, wods) ->
                    DayCard(
                        date = date,
                        wods = wods,
                        onEditTime = { editingWod = it },
                        onDelete = { 
                            // No necesitamos coroutine aquí, el ViewModel ya maneja las coroutines
                            viewModel.deleteWodFromCalendar(it)
                        },
                        onComplete = { completingWod = it }
                    )
                }
            }
        }
    }

    // Time Picker Dialog para editar hora
    editingWod?.let { wod ->
        TimePickerDialog(
            initialTime = wod.hora ?: java.time.LocalTime.of(18, 0),
            preferredHour = preferredHour,
            preferredMinute = preferredMinute,
            onDismiss = { editingWod = null },
            onTimeSelected = { time ->
                // No necesitamos coroutine aquí, el ViewModel ya maneja las coroutines
                time?.let { 
                    viewModel.updateWodTime(wod, it)
                }
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
                // No necesitamos coroutine aquí, el ViewModel ya maneja las coroutines
                viewModel.completeWod(wod, calories, distance, duration, notes)
                completingWod = null
            }
        )
    }
}

@Composable
private fun WeekHeader() {
    val today = LocalDate.now()
    val startOfWeek = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L)
    val endOfWeek = startOfWeek.plusDays(6)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Semana del ${startOfWeek.format(DateTimeFormatter.ofPattern("dd/MM"))} al ${endOfWeek.format(DateTimeFormatter.ofPattern("dd/MM"))}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Planifica y completa tus actividades de la semana",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun DayCard(
    date: LocalDate,
    wods: List<Wod>,
    onEditTime: (Wod) -> Unit,
    onDelete: (Wod) -> Unit,
    onComplete: (Wod) -> Unit
) {
    val today = LocalDate.now()
    val isToday = date == today
    val isPast = date.isBefore(today)
    val isFuture = date.isAfter(today)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isToday -> MaterialTheme.colorScheme.primaryContainer
                isPast -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header del día
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", Locale("es"))),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isToday -> "Hoy"
                            isPast -> "Pasado"
                            else -> "Futuro"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                if (isToday) {
                    Icon(
                        Icons.Default.Today,
                        contentDescription = "Hoy",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // WODs del día
            wods.forEach { wod ->
                WeeklyWodItem(
                    wod = wod,
                    onEditTime = { onEditTime(wod) },
                    onDelete = { onDelete(wod) },
                    onComplete = { onComplete(wod) }
                )
                if (wod != wods.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WeeklyWodItem(
    wod: Wod,
    onEditTime: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (wod.completada) 
                MaterialTheme.colorScheme.tertiaryContainer
            else if (wod.gimnasio == "CrossFit DB")
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Encabezado del WOD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = wod.gimnasio,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (wod.hora != null) {
                        Text(
                            text = wod.hora.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Row {
                    if (!wod.completada) {
                        IconButton(onClick = onEditTime) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar hora",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
            
            // Estado del WOD
            if (wod.completada) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Completada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Mostrar métricas si están disponibles
                val metrics = mutableListOf<String>()
                wod.caloriasQuemadas?.let { metrics.add("🔥 $it kcal") }
                wod.distanciaKm?.let { metrics.add("📍 $it km") }
                wod.duracionMinutos?.let { metrics.add("⏱️ $it min") }
                
                if (metrics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = metrics.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
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
            }
        }
    }
}

