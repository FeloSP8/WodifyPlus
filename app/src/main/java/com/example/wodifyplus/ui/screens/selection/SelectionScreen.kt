package com.example.wodifyplus.ui.screens.selection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.example.wodifyplus.ui.components.WodComparisonCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(
    onNavigateToCalendar: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SelectionViewModel = viewModel()
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val preferredHour by preferencesManager.preferredHour.collectAsState(initial = 18)
    val preferredMinute by preferencesManager.preferredMinute.collectAsState(initial = 0)
    
    val wodsByDate by viewModel.wodsByDate.collectAsState()

    var showTimePickerFor by remember { mutableStateOf<Wod?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadWods()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar WODs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Default.Check, contentDescription = "Ver Calendario")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (wodsByDate.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay WODs disponibles.\nVuelve a la pantalla anterior y obtén los WODs.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                wodsByDate.toSortedMap().forEach { (date, wods) ->
                    item {
                        DaySelectionSection(
                            date = date,
                            wods = wods,
                            onSelectWod = { wod ->
                                showTimePickerFor = wod
                            },
                            onDeselectWod = { wod ->
                                viewModel.deselectWod(wod)
                            }
                        )
                    }
                }
            }
        }
    }

    // Time Picker Dialog
    showTimePickerFor?.let { wod ->
        // Obtener hora preferida de la actividad específica, o usar la general
        val (activityHour, activityMinute) = viewModel.getPreferredTimeForActivity(wod.gimnasio) 
            ?: (preferredHour to preferredMinute)
        
        TimePickerDialog(
            preferredHour = activityHour,
            preferredMinute = activityMinute,
            onDismiss = { showTimePickerFor = null },
            onTimeSelected = { time ->
                viewModel.selectWod(wod, time)
                showTimePickerFor = null
            }
        )
    }
}

@Composable
private fun DaySelectionSection(
    date: LocalDate,
    wods: List<Wod>,
    onSelectWod: (Wod) -> Unit,
    onDeselectWod: (Wod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Encabezado del día
            Text(
                text = "${wods.firstOrNull()?.diaSemana ?: ""} - ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // WODs del día
            wods.forEach { wod ->
                WodComparisonCard(
                    wod = wod,
                    onSelect = { onSelectWod(wod) },
                    onDeselect = { onDeselectWod(wod) }
                )

                if (wod != wods.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

