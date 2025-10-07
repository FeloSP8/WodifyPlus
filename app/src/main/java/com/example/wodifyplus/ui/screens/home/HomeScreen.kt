package com.example.wodifyplus.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onNavigateToSelection: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
    nextActivityViewModel: NextActivityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val nextActivity by nextActivityViewModel.nextActivity.collectAsState()

    // Navegar automáticamente a selección cuando se obtengan los WODs
    LaunchedEffect(uiState) {
        if (uiState is HomeUiState.Success) {
            onNavigateToSelection()
        }
    }
    
    // Key para forzar recarga cuando volvemos a Home
    val refreshKey = remember { mutableStateOf(0) }
    
    // Recargar próxima actividad cada vez que se entra en Home
    LaunchedEffect(refreshKey.value) {
        nextActivityViewModel.loadNextActivity()
        // Resetear estado si no estamos en idle
        if (uiState !is HomeUiState.Idle && uiState !is HomeUiState.Loading) {
            viewModel.resetState()
        }
    }
    
    // Incrementar key cuando la navegación vuelve a esta pantalla
    DisposableEffect(Unit) {
        onDispose {
            refreshKey.value++
        }
    }

    Scaffold(
        floatingActionButton = {
            if (uiState !is HomeUiState.Loading) {
                FloatingActionButton(
                    onClick = { viewModel.fetchWods() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Obtener WODs")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header compacto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WODIFY PLUS",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "v6.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        
            // Próxima actividad - COMPLETA SIN CORTES
            nextActivity?.let { wod ->
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Tu próxima actividad",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NextActivityCard(wod)
                }
            } ?: run {
                // No hay próxima actividad
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "No tienes actividades planificadas",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Pulsa el botón + para obtener los WODs de la semana",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Estado de la UI
            when (val state = uiState) {
                is HomeUiState.Idle -> {
                    // Ya no mostramos nada aquí, el FAB se encarga
                }
                is HomeUiState.Loading -> {
                    LoadingContent()
                }
                is HomeUiState.Success -> {
                    // Auto-navega, no hace falta UI
                }
                is HomeUiState.Error -> {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ErrorContent(message = state.message)
                    }
                }
            }
        }
    }
}


@Composable
private fun LoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 64.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Obteniendo WODs...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ErrorContent(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "❌ Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Pulsa el botón + para reintentar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun NextActivityCard(wod: com.example.wodifyplus.data.models.Wod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gimnasio
            Text(
                wod.gimnasio,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Fecha y hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "${wod.diaSemana} ${wod.fecha.format(DateTimeFormatter.ofPattern("dd/MM"))}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                wod.hora?.let { hora ->
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                hora.format(DateTimeFormatter.ofPattern("HH:mm")),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // Contenido COMPLETO - sin límites
            Text(
                wod.contenido,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
        }
    }
}

