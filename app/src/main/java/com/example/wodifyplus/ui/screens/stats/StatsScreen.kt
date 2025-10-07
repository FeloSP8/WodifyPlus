package com.example.wodifyplus.ui.screens.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val statsData by viewModel.statsData.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStats(StatsPeriod.WEEK)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selector de período
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { viewModel.loadStats(it) }
            )

            // Tarjetas de resumen
            StatsCards(statsData = statsData)

            // Gráfica de entrenamientos por día
            if (statsData.workoutsByDate.isNotEmpty()) {
                WorkoutsChart(statsData = statsData)
            }

            // Entrenamientos por actividad
            if (statsData.workoutsByActivity.isNotEmpty()) {
                ActivityBreakdown(statsData = statsData)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedPeriod == StatsPeriod.WEEK,
            onClick = { onPeriodSelected(StatsPeriod.WEEK) },
            label = { Text("Semana") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedPeriod == StatsPeriod.MONTH,
            onClick = { onPeriodSelected(StatsPeriod.MONTH) },
            label = { Text("Mes") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedPeriod == StatsPeriod.YEAR,
            onClick = { onPeriodSelected(StatsPeriod.YEAR) },
            label = { Text("Año") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatsCards(statsData: StatsData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            icon = Icons.Default.FitnessCenter,
            value = statsData.totalWorkouts.toString(),
            label = "Entrenamientos",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.LocalFireDepartment,
            value = statsData.totalCalories.toString(),
            label = "Calorías",
            modifier = Modifier.weight(1f)
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            icon = Icons.Default.DirectionsRun,
            value = String.format("%.1f", statsData.totalDistance),
            label = "Km",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.Timer,
            value = statsData.totalMinutes.toString(),
            label = "Minutos",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun WorkoutsChart(statsData: StatsData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Entrenamientos completados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val modelProducer = remember { CartesianChartModelProducer() }
            
            LaunchedEffect(statsData.workoutsByDate) {
                modelProducer.runTransaction {
                    columnSeries {
                        series(statsData.workoutsByDate.map { it.second })
                    }
                }
            }
            
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis()
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Composable
private fun ActivityBreakdown(statsData: StatsData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Por tipo de actividad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            statsData.workoutsByActivity.forEach { (activity, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(activity, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        "$count entrenamientos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (activity != statsData.workoutsByActivity.keys.last()) {
                    HorizontalDivider()
                }
            }
        }
    }
}


