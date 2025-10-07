# 🚀 WodifyPlus - Guía Completa de Implementación

## 📋 Índice

1. [Crear Proyecto en Android Studio](#1-crear-proyecto-en-android-studio)
2. [Configurar Dependencias](#2-configurar-dependencias)
3. [Estructura de Carpetas](#3-estructura-de-carpetas)
4. [Modelos de Datos](#4-modelos-de-datos)
5. [Base de Datos Room](#5-base-de-datos-room)
6. [Repository](#6-repository)
7. [ViewModels](#7-viewmodels)
8. [Navegación](#8-navegación)
9. [Pantallas UI](#9-pantallas-ui)
10. [Componentes Reutilizables](#10-componentes-reutilizables)
11. [Notificaciones](#11-notificaciones)
12. [Migrar Scrapers Python](#12-migrar-scrapers-python)
13. [MainActivity](#13-mainactivity)
14. [Probar la App](#14-probar-la-app)

---

## 1. Crear Proyecto en Android Studio

### Pasos:

1. **Abrir Android Studio**
2. **File > New > New Project**
3. **Seleccionar**: "Empty Activity" (Compose)
4. **Configurar**:
   - **Name**: `WodifyPlus`
   - **Package**: `com.example.wodifyplus`
   - **Save location**: `C:\Users\Felipe\AndroidStudioProjects\WodifyPlus`
   - **Language**: Kotlin
   - **Minimum SDK**: API 24 (Android 7.0)
5. **Finish** y espera a que sincronice

---

## 2. Configurar Dependencias

### Archivo: `app/build.gradle.kts`

Reemplaza TODO el contenido con esto:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
    id("com.chaquo.python")
}

android {
    namespace = "com.example.wodifyplus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wodifyplus"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "5.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }

        chaquopy {
            defaultConfig {
                version = "3.10"
                pip {
                    install("python-dotenv")
                    install("requests")
                    install("beautifulsoup4")
                    install("lxml")
                }
            }
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            assets.srcDirs("src/main/python")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // DataStore (para configuración)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // WorkManager (para notificaciones)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Google Fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

### Archivo: `build.gradle.kts` (raíz del proyecto)

Asegúrate de que tenga esto al inicio:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.chaquo.python") version "15.0.1" apply false
}
```

Después de editar, **Sync Project with Gradle Files**

---

## 3. Estructura de Carpetas

Crea esta estructura en `app/src/main/java/com/example/wodifyplus/`:

```
wodifyplus/
├── MainActivity.kt
├── WodifyApp.kt
├── data/
│   ├── local/
│   │   ├── WodDatabase.kt
│   │   ├── WodDao.kt
│   │   └── entities/
│   │       └── WodEntity.kt
│   ├── repository/
│   │   └── WodRepository.kt
│   └── models/
│       └── Wod.kt
├── ui/
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   ├── selection/
│   │   │   ├── SelectionScreen.kt
│   │   │   └── SelectionViewModel.kt
│   │   ├── calendar/
│   │   │   ├── CalendarScreen.kt
│   │   │   └── CalendarViewModel.kt
│   │   └── settings/
│   │       └── SettingsScreen.kt
│   ├── components/
│   │   ├── WodCard.kt
│   │   ├── BottomNavBar.kt
│   │   ├── TimePickerDialog.kt
│   │   └── WodComparisonCard.kt
│   ├── navigation/
│   │   └── NavGraph.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── notifications/
    ├── NotificationHelper.kt
    └── WodReminderWorker.kt
```

---

## 4. Modelos de Datos

### Archivo: `data/models/Wod.kt`

```kotlin
package com.example.wodifyplus.data.models

import java.time.LocalDate
import java.time.LocalTime

data class Wod(
    val id: Int = 0,
    val fecha: LocalDate,
    val diaSemana: String,
    val gimnasio: String, // "CrossFit DB" o "N8"
    val contenido: String,
    val contenidoHtml: String = "",
    val hora: LocalTime? = null,
    val notificacionActiva: Boolean = false,
    val seleccionado: Boolean = false
)
```

---

## 5. Base de Datos Room

### Archivo: `data/local/entities/WodEntity.kt`

```kotlin
package com.example.wodifyplus.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wods")
data class WodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fecha: String, // "2025-01-20"
    val diaSemana: String, // "Lunes"
    val gimnasio: String, // "CrossFit DB" o "N8"
    val contenido: String,
    val contenidoHtml: String = "",
    val hora: String? = null, // "18:30" o null
    val notificacionActiva: Boolean = false,
    val seleccionado: Boolean = false
)
```

### Archivo: `data/local/WodDao.kt`

```kotlin
package com.example.wodifyplus.data.local

import androidx.room.*
import com.example.wodifyplus.data.local.entities.WodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WodDao {
    @Query("SELECT * FROM wods ORDER BY fecha ASC")
    fun getAllWods(): Flow<List<WodEntity>>

    @Query("SELECT * FROM wods WHERE fecha = :fecha")
    fun getWodsByDate(fecha: String): Flow<List<WodEntity>>

    @Query("SELECT * FROM wods WHERE seleccionado = 1 ORDER BY fecha ASC")
    fun getSelectedWods(): Flow<List<WodEntity>>

    @Query("SELECT * FROM wods WHERE id = :id")
    suspend fun getWodById(id: Int): WodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWod(wod: WodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWods(wods: List<WodEntity>)

    @Update
    suspend fun updateWod(wod: WodEntity)

    @Delete
    suspend fun deleteWod(wod: WodEntity)

    @Query("DELETE FROM wods")
    suspend fun deleteAllWods()

    @Query("DELETE FROM wods WHERE fecha < :fecha")
    suspend fun deleteOldWods(fecha: String)
}
```

### Archivo: `data/local/WodDatabase.kt`

```kotlin
package com.example.wodifyplus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.wodifyplus.data.local.entities.WodEntity

@Database(
    entities = [WodEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WodDatabase : RoomDatabase() {
    abstract fun wodDao(): WodDao

    companion object {
        @Volatile
        private var INSTANCE: WodDatabase? = null

        fun getDatabase(context: Context): WodDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WodDatabase::class.java,
                    "wodify_plus_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

---

## 6. Repository

### Archivo: `data/repository/WodRepository.kt`

```kotlin
package com.example.wodifyplus.data.repository

import com.example.wodifyplus.data.local.WodDao
import com.example.wodifyplus.data.local.entities.WodEntity
import com.example.wodifyplus.data.models.Wod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WodRepository(private val wodDao: WodDao) {

    // Conversión Entity -> Model
    private fun WodEntity.toWod(): Wod {
        return Wod(
            id = id,
            fecha = LocalDate.parse(fecha),
            diaSemana = diaSemana,
            gimnasio = gimnasio,
            contenido = contenido,
            contenidoHtml = contenidoHtml,
            hora = hora?.let { LocalTime.parse(it) },
            notificacionActiva = notificacionActiva,
            seleccionado = seleccionado
        )
    }

    // Conversión Model -> Entity
    private fun Wod.toEntity(): WodEntity {
        return WodEntity(
            id = id,
            fecha = fecha.format(DateTimeFormatter.ISO_LOCAL_DATE),
            diaSemana = diaSemana,
            gimnasio = gimnasio,
            contenido = contenido,
            contenidoHtml = contenidoHtml,
            hora = hora?.format(DateTimeFormatter.ISO_LOCAL_TIME),
            notificacionActiva = notificacionActiva,
            seleccionado = seleccionado
        )
    }

    // Operaciones
    val allWods: Flow<List<Wod>> = wodDao.getAllWods().map { entities ->
        entities.map { it.toWod() }
    }

    val selectedWods: Flow<List<Wod>> = wodDao.getSelectedWods().map { entities ->
        entities.map { it.toWod() }
    }

    fun getWodsByDate(date: LocalDate): Flow<List<Wod>> {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return wodDao.getWodsByDate(dateStr).map { entities ->
            entities.map { it.toWod() }
        }
    }

    suspend fun getWodById(id: Int): Wod? {
        return wodDao.getWodById(id)?.toWod()
    }

    suspend fun insertWod(wod: Wod): Long {
        return wodDao.insertWod(wod.toEntity())
    }

    suspend fun insertWods(wods: List<Wod>) {
        wodDao.insertWods(wods.map { it.toEntity() })
    }

    suspend fun updateWod(wod: Wod) {
        wodDao.updateWod(wod.toEntity())
    }

    suspend fun deleteWod(wod: Wod) {
        wodDao.deleteWod(wod.toEntity())
    }

    suspend fun deleteAllWods() {
        wodDao.deleteAllWods()
    }

    suspend fun deleteOldWods(beforeDate: LocalDate) {
        val dateStr = beforeDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        wodDao.deleteOldWods(dateStr)
    }
}
```

---

## 7. ViewModels

### Archivo: `ui/screens/home/HomeViewModel.kt`

```kotlin
package com.example.wodifyplus.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.wodifyplus.data.local.WodDatabase
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.data.repository.WodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WodRepository

    init {
        val wodDao = WodDatabase.getDatabase(application).wodDao()
        repository = WodRepository(wodDao)

        // Inicializar Python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(application))
        }
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun fetchWods() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            try {
                val result = withContext(Dispatchers.IO) {
                    val py = Python.getInstance()
                    val module = py.getModule("wod_scraper")
                    module.callAttr("main").toString()
                }

                // Parsear resultado y guardar en BD
                parseAndSaveWods(result)

                _uiState.value = HomeUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private suspend fun parseAndSaveWods(result: String) {
        // TODO: Parsear el resultado del scraper
        // Por ahora, crear WODs de prueba
        val wods = createSampleWods()
        repository.insertWods(wods)
    }

    private fun createSampleWods(): List<Wod> {
        val today = LocalDate.now()
        val wods = mutableListOf<Wod>()

        for (i in 0..6) {
            val date = today.plusDays(i.toLong())
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES"))

            // WOD de CrossFit DB
            wods.add(
                Wod(
                    fecha = date,
                    diaSemana = dayName.capitalize(),
                    gimnasio = "CrossFit DB",
                    contenido = "STRENGTH\n5x5 Back Squat\n\nMETCON\n21-15-9\nThrusters\nPull-ups",
                    contenidoHtml = ""
                )
            )

            // WOD de N8
            wods.add(
                Wod(
                    fecha = date,
                    diaSemana = dayName.capitalize(),
                    gimnasio = "N8",
                    contenido = "AMRAP 20'\n10 Box Jumps\n15 Wall Balls\n20 Double Unders",
                    contenidoHtml = ""
                )
            )
        }

        return wods
    }
}

sealed class HomeUiState {
    object Idle : HomeUiState()
    object Loading : HomeUiState()
    data class Success(val message: String) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
```

### Archivo: `ui/screens/selection/SelectionViewModel.kt`

```kotlin
package com.example.wodifyplus.ui.screens.selection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wodifyplus.data.local.WodDatabase
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.data.repository.WodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class SelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WodRepository

    init {
        val wodDao = WodDatabase.getDatabase(application).wodDao()
        repository = WodRepository(wodDao)
    }

    private val _wodsByDate = MutableStateFlow<Map<LocalDate, List<Wod>>>(emptyMap())
    val wodsByDate: StateFlow<Map<LocalDate, List<Wod>>> = _wodsByDate.asStateFlow()

    fun loadWods() {
        viewModelScope.launch {
            repository.allWods.collect { wods ->
                _wodsByDate.value = wods.groupBy { it.fecha }
            }
        }
    }

    fun selectWod(wod: Wod, hora: LocalTime?) {
        viewModelScope.launch {
            val updatedWod = wod.copy(
                seleccionado = true,
                hora = hora,
                notificacionActiva = hora != null
            )
            repository.updateWod(updatedWod)
        }
    }

    fun deselectWod(wod: Wod) {
        viewModelScope.launch {
            val updatedWod = wod.copy(
                seleccionado = false,
                hora = null,
                notificacionActiva = false
            )
            repository.updateWod(updatedWod)
        }
    }
}
```

### Archivo: `ui/screens/calendar/CalendarViewModel.kt`

```kotlin
package com.example.wodifyplus.ui.screens.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wodifyplus.data.local.WodDatabase
import com.example.wodifyplus.data.models.Wod
import com.example.wodifyplus.data.repository.WodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WodRepository

    init {
        val wodDao = WodDatabase.getDatabase(application).wodDao()
        repository = WodRepository(wodDao)
    }

    private val _selectedWods = MutableStateFlow<List<Wod>>(emptyList())
    val selectedWods: StateFlow<List<Wod>> = _selectedWods.asStateFlow()

    fun loadSelectedWods() {
        viewModelScope.launch {
            repository.selectedWods.collect { wods ->
                _selectedWods.value = wods
            }
        }
    }

    fun updateWodTime(wod: Wod, newTime: LocalTime?) {
        viewModelScope.launch {
            val updatedWod = wod.copy(
                hora = newTime,
                notificacionActiva = newTime != null
            )
            repository.updateWod(updatedWod)
        }
    }

    fun deleteWodFromCalendar(wod: Wod) {
        viewModelScope.launch {
            val updatedWod = wod.copy(
                seleccionado = false,
                hora = null,
                notificacionActiva = false
            )
            repository.updateWod(updatedWod)
        }
    }
}
```

---

(Continúa en el siguiente mensaje debido al límite de caracteres...)
