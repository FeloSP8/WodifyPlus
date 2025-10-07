# 🚀 WodifyPlus - Parte 3: Notificaciones y Configuración Final

## 11. Sistema de Notificaciones

### Archivo: `notifications/NotificationHelper.kt`

```kotlin
package com.example.wodifyplus.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.wodifyplus.MainActivity
import com.example.wodifyplus.R

object NotificationHelper {
    private const val CHANNEL_ID = "wod_reminders"
    private const val CHANNEL_NAME = "Recordatorios de WOD"
    private const val CHANNEL_DESCRIPTION = "Notificaciones para recordar los entrenamientos"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendWodReminder(
        context: Context,
        wodId: Int,
        title: String,
        content: String
    ) {
        // Verificar permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Intent para abrir la app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            wodId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Cambiar por tu icono
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Mostrar notificación
        with(NotificationManagerCompat.from(context)) {
            notify(wodId, notification)
        }
    }
}
```

### Archivo: `notifications/WodReminderWorker.kt`

```kotlin
package com.example.wodifyplus.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wodifyplus.data.local.WodDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WodReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val wodId = inputData.getInt("WOD_ID", -1)
            if (wodId == -1) return@withContext Result.failure()

            // Obtener WOD de la BD
            val wodDao = WodDatabase.getDatabase(applicationContext).wodDao()
            val wod = wodDao.getWodById(wodId) ?: return@withContext Result.failure()

            // Enviar notificación
            NotificationHelper.sendWodReminder(
                context = applicationContext,
                wodId = wod.id,
                title = "🏋️ Es hora de entrenar!",
                content = "${wod.diaSemana} - ${wod.gimnasio}"
            )

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
```

---

## 12. Migrar Scrapers Python

### Paso 1: Copiar carpeta Python

Desde tu proyecto **Wodify** original, copia la carpeta completa:

```
Wodify/app/src/main/python/
```

Y pégala en:

```
WodifyPlus/app/src/main/python/
```

### Paso 2: Verificar estructura

Deberías tener:

```
WodifyPlus/app/src/main/python/
├── .env (NO incluir en git)
├── config.py
├── crossfitdb.py
├── n8.py
└── wod_scraper.py
```

### Paso 3: Añadir a .gitignore

Edita `.gitignore` en la raíz del proyecto y añade:

```
# Python
app/src/main/python/.env
app/src/main/python/__pycache__/
*.pyc
```

---

## 13. MainActivity y Application

### Archivo: `MainActivity.kt`

```kotlin
package com.example.wodifyplus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.wodifyplus.notifications.NotificationHelper
import com.example.wodifyplus.ui.components.BottomNavBar
import com.example.wodifyplus.ui.navigation.NavGraph
import com.example.wodifyplus.ui.theme.WodifyPlusTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge to edge
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        // Inicializar Python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Crear canal de notificaciones
        NotificationHelper.createNotificationChannel(this)

        // Solicitar permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permiso ya concedido
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        setContent {
            WodifyPlusTheme {
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavBar(navController = navController)
                    }
                ) { paddingValues ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
```

### Archivo: `WodifyApp.kt` (Application Class)

```kotlin
package com.example.wodifyplus

import android.app.Application

class WodifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializaciones globales si es necesario
    }
}
```

---

## 14. AndroidManifest.xml

Edita `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permisos -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

    <application
        android:name=".WodifyApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.WodifyPlus"
        android:usesCleartextTraffic="true"
        tools:targetApi="31">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.WodifyPlus">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- WorkManager Worker -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup" />
        </provider>
    </application>

</manifest>
```

---

## 15. Tema (Theme)

### Archivo: `ui/theme/Color.kt`

```kotlin
package com.example.wodifyplus.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
```

### Archivo: `ui/theme/Theme.kt`

```kotlin
package com.example.wodifyplus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun WodifyPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### Archivo: `ui/theme/Type.kt`

```kotlin
package com.example.wodifyplus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
```

---

## 16. Archivos de Recursos

### Archivo: `res/values/strings.xml`

```xml
<resources>
    <string name="app_name">WodifyPlus</string>
</resources>
```

### Archivo: `res/values/themes.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.WodifyPlus" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

---

## 17. Checklist de Implementación

### ✅ Configuración Inicial

- [ ] Crear proyecto en Android Studio
- [ ] Reemplazar `build.gradle.kts` con la configuración completa
- [ ] Sync Gradle
- [ ] Verificar que no hay errores de compilación

### ✅ Estructura de Datos

- [ ] Crear paquete `data/models/` y añadir `Wod.kt`
- [ ] Crear paquete `data/local/entities/` y añadir `WodEntity.kt`
- [ ] Crear `data/local/WodDao.kt`
- [ ] Crear `data/local/WodDatabase.kt`
- [ ] Crear `data/repository/WodRepository.kt`

### ✅ ViewModels

- [ ] Crear `ui/screens/home/HomeViewModel.kt`
- [ ] Crear `ui/screens/selection/SelectionViewModel.kt`
- [ ] Crear `ui/screens/calendar/CalendarViewModel.kt`

### ✅ Navegación

- [ ] Crear `ui/navigation/NavGraph.kt`

### ✅ Pantallas

- [ ] Crear `ui/screens/home/HomeScreen.kt`
- [ ] Crear `ui/screens/selection/SelectionScreen.kt`
- [ ] Crear `ui/screens/calendar/CalendarScreen.kt`
- [ ] Crear `ui/screens/settings/SettingsScreen.kt`

### ✅ Componentes

- [ ] Crear `ui/components/WodComparisonCard.kt`
- [ ] Crear `ui/components/TimePickerDialog.kt`
- [ ] Crear `ui/components/BottomNavBar.kt`

### ✅ Notificaciones

- [ ] Crear `notifications/NotificationHelper.kt`
- [ ] Crear `notifications/WodReminderWorker.kt`

### ✅ MainActivity y Configuración

- [ ] Actualizar `MainActivity.kt`
- [ ] Crear `WodifyApp.kt`
- [ ] Actualizar `AndroidManifest.xml`

### ✅ Tema

- [ ] Verificar `ui/theme/Color.kt`
- [ ] Verificar `ui/theme/Theme.kt`
- [ ] Verificar `ui/theme/Type.kt`

### ✅ Python

- [ ] Copiar carpeta `python/` desde Wodify
- [ ] Crear archivo `.env` con credenciales
- [ ] Añadir `.env` al `.gitignore`

### ✅ Final

- [ ] Build > Clean Project
- [ ] Build > Rebuild Project
- [ ] Ejecutar en dispositivo/emulador
- [ ] Probar flujo completo

---

## 18. Probar la Aplicación

### Primera Ejecución

1. **Conecta tu dispositivo** o inicia un emulador
2. **Run** (▶️) o `Shift + F10`
3. La app debería compilar y ejecutarse

### Flujo de Prueba

1. **Pantalla Home**:

   - Pulsa "Obtener WODs"
   - Debería mostrar mensaje de éxito
   - Pulsa "Seleccionar WODs"

2. **Pantalla Selection**:

   - Verás WODs agrupados por día
   - Selecciona un WOD para cada día
   - Elige hora de entrenamiento
   - Pulsa ✓ para ir al calendario

3. **Pantalla Calendar**:

   - Verás tu agenda semanal
   - Edita horas
   - Elimina días si quieres

4. **Bottom Navigation**:
   - Navega entre Home, Calendar y Settings

---

## 19. Troubleshooting

### Error: "Python not started"

**Solución**: Verifica que la inicialización de Python está en `MainActivity.onCreate()`

### Error: "Room database compilation error"

**Solución**: Asegúrate de tener `ksp` plugin en `build.gradle.kts`

### Error: "Cannot resolve symbol 'R'"

**Solución**: Build > Clean Project > Rebuild Project

### La app crashea al abrir

**Solución**: Revisa Logcat para ver el error específico

---

## 20. Próximos Pasos

Una vez que tengas la app funcionando:

1. **Mejorar el Parser**: Adaptar `HomeViewModel` para parsear correctamente el resultado de Python
2. **Programar Notificaciones**: Usar WorkManager para programar notificaciones
3. **Añadir Configuración**: Implementar opciones en SettingsScreen
4. **Mejorar UI**: Añadir animaciones y transiciones
5. **Añadir Tests**: Implementar tests unitarios

---

## 21. Recursos

- [Room Database](https://developer.android.com/training/data-storage/room)
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)

---

**¡Listo!** Ahora tienes la guía completa para implementar WodifyPlus. Sigue cada paso y tendrás una app completamente funcional.

Si encuentras algún error o necesitas ayuda con algún paso específico, avísame.

**¡A programar! 🚀**
