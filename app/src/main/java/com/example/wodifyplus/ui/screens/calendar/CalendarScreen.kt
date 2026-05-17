package com.example.wodifyplus.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.data.preferences.PreferencesManager
import com.example.wodifyplus.ui.components.TimePickerDialog
import com.example.wodifyplus.ui.components.CompleteActivityDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val preferredHour by preferencesManager.preferredHour.collectAsState(initial = 18)
    val preferredMinute by preferencesManager.preferredMinute.collectAsState(initial = 0)
    
    val selectedWods by viewModel.selectedWods.collectAsState()
    val completedWods by viewModel.completedWods.collectAsState()
    var editingWod by remember { mutableStateOf<Wod?>(null) }
    var completingWod by remember { mutableStateOf<Wod?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showCalendarView by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.loadSelectedWods()
        viewModel.loadCompletedWods()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Calendario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showCalendarView = !showCalendarView }) {
                        Icon(
                            if (showCalendarView) Icons.Default.List else Icons.Default.CalendarMonth,
                            contentDescription = if (showCalendarView) "Vista de lista" else "Vista de calendario"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showCalendarView) {
            CalendarView(
                completedWods = completedWods,
                selectedWods = selectedWods,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                onWodClick = { completingWod = it },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            if (selectedWods.isEmpty() && completedWods.isEmpty()) {
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
                            "No tienes actividades planificadas",
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
                            onDelete = { 
                            // Llamar directamente, el ViewModel maneja las coroutines internamente
                            viewModel.deleteWodFromCalendar(wod)
                        },
                            onComplete = { completingWod = wod }
                        )
                    }
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
private fun CalendarView(
    completedWods: List<Wod>,
    selectedWods: List<Wod>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onWodClick: (Wod) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val currentMonth = remember { today.withDayOfMonth(1) }
    var displayedMonth by remember { mutableStateOf(currentMonth) }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Header del mes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
            }
            
            Text(
                text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es"))),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
            }
        }
        
        // Días de la semana
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Calendario
        CalendarGrid(
            month = displayedMonth,
            completedWods = completedWods,
            selectedWods = selectedWods,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            onWodClick = onWodClick,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Detalles del día seleccionado
        selectedDate?.let { date ->
            DayDetails(
                date = date,
                completedWods = completedWods.filter { it.fecha == date },
                selectedWods = selectedWods.filter { it.fecha == date },
                onWodClick = onWodClick,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    month: LocalDate,
    completedWods: List<Wod>,
    selectedWods: List<Wod>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onWodClick: (Wod) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = month.withDayOfMonth(1)
    val lastDayOfMonth = month.withDayOfMonth(month.lengthOfMonth())

    // Usar WeekFields con Monday como primer día de la semana
    val weekFields = WeekFields.of(Locale("es", "ES"))
    val firstMonday = firstDayOfMonth.with(weekFields.dayOfWeek(), 1L)
    val lastSunday = lastDayOfMonth.with(weekFields.dayOfWeek(), 7L)

    val daysInMonth = generateSequence(firstMonday) { it.plusDays(1) }
        .takeWhile { !it.isAfter(lastSunday) }
        .toList()
    val today = LocalDate.now()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(daysInMonth) { date ->
            val isCurrentMonth = date.month == month.month
            val isToday = date == today
            val isSelected = date == selectedDate
            val hasCompleted = completedWods.any { it.fecha == date }
            val hasSelected = selectedWods.any { it.fecha == date }
            
            CalendarDay(
                date = date,
                isCurrentMonth = isCurrentMonth,
                isToday = isToday,
                isSelected = isSelected,
                hasCompleted = hasCompleted,
                hasSelected = hasSelected,
                onClick = { onDateSelected(date) },
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    hasCompleted: Boolean,
    hasSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            
            if (hasCompleted || hasSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (hasCompleted) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                    }
                    if (hasSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetails(
    date: LocalDate,
    completedWods: List<Wod>,
    selectedWods: List<Wod>,
    onWodClick: (Wod) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", Locale("es"))),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (completedWods.isEmpty() && selectedWods.isEmpty()) {
                Text(
                    text = "No hay actividades para este día",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                // Actividades completadas
                completedWods.forEach { wod ->
                    CompletedWodItem(
                        wod = wod,
                        onClick = { onWodClick(wod) }
                    )
                }
                
                // Actividades planificadas
                selectedWods.forEach { wod ->
                    PlannedWodItem(
                        wod = wod,
                        onClick = { onWodClick(wod) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedWodItem(
    wod: Wod,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wod.gimnasio,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Completada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun PlannedWodItem(
    wod: Wod,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wod.gimnasio,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = wod.hora?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Sin hora",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
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

