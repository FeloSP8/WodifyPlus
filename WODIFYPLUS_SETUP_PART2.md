# 🚀 WodifyPlus - Parte 2: UI y Navegación

## 8. Navegación

### Archivo: `ui/navigation/NavGraph.kt`

```kotlin
package com.example.wodifyplus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.wodifyplus.ui.screens.calendar.CalendarScreen
import com.example.wodifyplus.ui.screens.home.HomeScreen
import com.example.wodifyplus.ui.screens.selection.SelectionScreen
import com.example.wodifyplus.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Selection : Screen("selection")
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSelection = {
                    navController.navigate(Screen.Selection.route)
                }
            )
        }

        composable(Screen.Selection.route) {
            SelectionScreen(
                onNavigateToCalendar = {
                    navController.navigate(Screen.Calendar.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
```

---

## 9. Pantallas UI

### Archivo: `ui/screens/home/HomeScreen.kt`

```kotlin
package com.example.wodifyplus.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    onNavigateToSelection: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título
        Text(
            text = "WODIFY PLUS",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "v5.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Planifica tu semana de CrossFit",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Estado de la UI
        when (val state = uiState) {
            is HomeUiState.Idle -> {
                IdleContent(
                    onFetchWods = { viewModel.fetchWods() }
                )
            }
            is HomeUiState.Loading -> {
                LoadingContent()
            }
            is HomeUiState.Success -> {
                SuccessContent(
                    message = state.message,
                    onNavigateToSelection = onNavigateToSelection,
                    onFetchAgain = { viewModel.fetchWods() }
                )
            }
            is HomeUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.fetchWods() }
                )
            }
        }
    }
}

@Composable
private fun IdleContent(onFetchWods: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Obtén los WODs de la semana",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Descarga los entrenamientos de CrossFit DB y Box N8 para planificar tu semana",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onFetchWods,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Obtener WODs")
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
private fun SuccessContent(
    message: String,
    onNavigateToSelection: () -> Unit,
    onFetchAgain: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "✅ WODs Obtenidos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onNavigateToSelection,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(56.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Text("Seleccionar WODs")
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
        onClick = onFetchAgain,
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text("Actualizar WODs")
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
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
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onRetry,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text("Reintentar")
    }
}
```

### Archivo: `ui/screens/selection/SelectionScreen.kt`

```kotlin
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.ui.components.TimePickerDialog
import com.example.wodifyplus.ui.components.WodComparisonCard
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(
    onNavigateToCalendar: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SelectionViewModel = viewModel()
) {
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
        TimePickerDialog(
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
```

### Archivo: `ui/screens/calendar/CalendarScreen.kt`

```kotlin
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.ui.components.TimePickerDialog
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val selectedWods by viewModel.selectedWods.collectAsState()
    var editingWod by remember { mutableStateOf<Wod?>(null) }

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
                        onDelete = { viewModel.deleteWodFromCalendar(wod) }
                    )
                }
            }
        }
    }

    // Time Picker Dialog para editar hora
    editingWod?.let { wod ->
        TimePickerDialog(
            initialTime = wod.hora,
            onDismiss = { editingWod = null },
            onTimeSelected = { time ->
                viewModel.updateWodTime(wod, time)
                editingWod = null
            }
        )
    }
}

@Composable
private fun CalendarWodCard(
    wod: Wod,
    onEditTime: () -> Unit,
    onDelete: () -> Unit
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
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Contenido del WOD (resumido)
            Text(
                text = wod.contenido.take(200) + if (wod.contenido.length > 200) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
```

### Archivo: `ui/screens/settings/SettingsScreen.kt`

```kotlin
package com.example.wodifyplus.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
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
                .padding(16.dp)
        ) {
            Text(
                "Próximamente: Configuración avanzada",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
```

---

## 10. Componentes Reutilizables

### Archivo: `ui/components/WodComparisonCard.kt`

```kotlin
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
```

### Archivo: `ui/components/TimePickerDialog.kt`

```kotlin
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
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime?) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialTime?.hour ?: 18) }
    var selectedMinute by remember { mutableStateOf(initialTime?.minute ?: 0) }

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
                        steps = 22,
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
                        steps = 58,
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
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
```

### Archivo: `ui/components/BottomNavBar.kt`

```kotlin
package com.example.wodifyplus.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.wodifyplus.ui.navigation.Screen

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem(Screen.Home.route, Icons.Filled.Home, "Inicio")
    object Calendar : BottomNavItem(Screen.Calendar.route, Icons.Filled.CalendarMonth, "Calendario")
    object Settings : BottomNavItem(Screen.Settings.route, Icons.Filled.Settings, "Ajustes")
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Calendar,
        BottomNavItem.Settings
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
```

---

(Continúa en WODIFYPLUS_SETUP_PART3.md...)
