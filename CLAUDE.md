# CLAUDE.md

> **Propósito**: Este archivo define **cómo quiero que trabajes** (Claude/IA copiloto) dentro de este repositorio: criterios de calidad, estilo, límites, comandos útiles, checklist de entregables y formato de respuestas. Escríbeme en **español** y genera **código en inglés** salvo que indique lo contrario.

---

## 1) Contexto del proyecto

* **Stack principal**: Android nativo con **Kotlin** + **Jetpack Compose**. Room Database (SQLite local), Chaquopy (integración Python), WorkManager, Navigation Compose, Material 3.
* **Arquitectura**: MVVM (ViewModel + Repository + DAO)
* **Build system**: Gradle (Kotlin DSL `.kts`)
* **IDE**: Android Studio
* **Sistema operativo**: Windows 11. Terminal: **PowerShell**.
* **Versiones clave**:
  - `minSdk 24` (Android 7.0)
  - `targetSdk 35` (Android 15)
  - Kotlin `2.0.21`
  - Compose BOM `2024.02.00`
* **Preferencias**: Mantén logs de performance y métricas cuando existan. No elimines `Log.d` / `println` existentes salvo indicación.

> Si necesitas tocar código Python (Chaquopy), consulta primero. Si hay carpetas de otros proyectos (Flutter, backend), **no las toques** salvo petición explícita.

---

## 2) Forma de trabajar (muy importante)

1. **Primero, el plan**: Antes de tocar nada, devuelve un *Plan de Acción* breve: objetivos, archivos a modificar, riesgos, y pruebas.
2. **Proponer los cambios como patch**: Entrega los cambios como **diff unificado** (`git diff`) o bloques por archivo con rutas claras. No escondas cambios.
3. **No borres funcionalidad** existente salvo instrucción explícita. Si refactorizas, deja **compatibilidad**.
4. **MVP primero**: Entrega algo que **compile y corra** con los scripts del repo. Luego mejoras.
5. **Windows friendly**: Usa rutas y comandos compatibles con Windows/PowerShell. Si necesitas Bash, provee alternativa.
6. **Medible**: Si optimizas, añade `console.time`/`performance.mark` o métricas concretas.
7. **Doc breve**: Tras cambios, añade/actualiza README/ comentarios en código/ JSDoc, pero sin ensayos.

---

## 3) Estilo de código y convenciones

* **Kotlin**: Estilo oficial de Google/JetBrains. Usa `val` por defecto, evita `!!`, preferir `?.let` / elvis `?:`.
* **Compose**: Composables con `PascalCase`, parámetros con nombres descriptivos, evita side-effects en composición (usa `LaunchedEffect`, `DisposableEffect`).
* **Arquitectura**:
  - **ViewModel**: Exponer `StateFlow`/`Flow`, nunca mutar estado directamente desde UI
  - **Repository**: Intermediario entre DAO/datasource y ViewModel
  - **Entity/Model**: Separar `Entity` (Room) de `Model` (dominio) si es necesario
* **Estado**: State hoisting cuando sea posible. `remember` / `rememberSaveable` prudente.
* **Nombres**: Descriptivos en **inglés**. Archivos `PascalCase.kt`, paquetes `lowercase`.
* **Resources**: IDs descriptivos (`R.string.wod_title`, no `R.string.text1`). Externiza strings hardcodeados.
* **Tests**: Si el repo tiene testing (JUnit, Espresso), crea pruebas básicas de ViewModels/logic.
* **Commits**: Conventional Commits (`feat:`, `fix:`, `chore:`...). Mensajes claros.

---

## 4) Calidad y seguridad

* **No expongas secretos**: API keys en `local.properties` o BuildConfig, nunca en código. Usa `buildConfigField` o recursos cifrados.
* **Validación**: Valida input de usuario antes de insertar en Room. Sanitiza datos de red/scraping.
* **Permisos**: Solicita permisos en runtime cuando sea necesario (Android 6+). Explica en UI por qué necesitas el permiso.
* **Errores**: Manejo consistente (try-catch en ViewModels, Snackbar/Toast en UI). No crashes silenciosos.
* **Accesibilidad**: `contentDescription` en imágenes/iconos, `semantics` para lectores de pantalla, contrastes Material 3.

---

## 5) Scripts y ejecución

Los comandos principales de Gradle (ejecutar desde raíz del proyecto en PowerShell):

```powershell
# Limpiar build
.\gradlew clean

# Compilar debug APK
.\gradlew assembleDebug

# Compilar release APK (firmado)
.\gradlew assembleRelease

# Instalar en dispositivo/emulador
.\gradlew installDebug

# Ejecutar tests unitarios
.\gradlew test

# Ejecutar tests instrumentados (requiere dispositivo/emulador)
.\gradlew connectedAndroidTest

# Verificar código (lint)
.\gradlew lint

# Build completo + test
.\gradlew build
```

**Android Studio**: Usa los botones de "Run" (Shift+F10) y "Debug" (Shift+F9). Para generar APK release: `Build > Generate Signed Bundle / APK`.

**Emulador**: Configura un AVD (Android Virtual Device) desde `Device Manager` en Android Studio.

> Si necesitas ejecutar scripts Python (Chaquopy), verifica que estén en `src/main/python/` y accesibles desde Kotlin vía `Python.getInstance()`.

---

## 6) Estructura esperada (orientativa)

```
WodifyPlus/
  app/
    src/
      main/
        java/com/example/wodifyplus/
          data/
            local/
              entities/          # Room entities
              WodDao.kt
              WodDatabase.kt
            models/              # Domain models
            repository/          # Repositories
            preferences/         # DataStore
          ui/
            screens/             # Pantallas principales
            components/          # Componentes reutilizables
            theme/               # Theme, Color, Type
            navigation/          # NavGraph
          notifications/         # WorkManager + NotificationHelper
          widget/                # Widget provider
          MainActivity.kt
          WodifyApp.kt
        python/                  # Scripts Python (Chaquopy)
        res/
          drawable/
          layout/                # Layouts XML (si los hay)
          values/                # strings.xml, colors.xml, themes.xml
          xml/                   # Widget info, preferences
        AndroidManifest.xml
      test/                      # Unit tests
      androidTest/               # Instrumented tests
    build.gradle.kts
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  local.properties             # Git-ignored, para API keys locales
  README.md
  CLAUDE.md
```

> Si la estructura difiere, **no reorganices** a lo loco. Propón un plan incremental.

---

## 7) Qué espero en tus respuestas

Siempre que te pida algo técnico, responde en este formato:

1. **Resumen en 2-3 puntos** de lo que harás.
2. **Plan de archivos** a crear/editar.
3. **Patch/Diff** con cambios (o bloques por archivo).
4. **Cómo ejecutar** y **cómo probar** (comandos Windows/PowerShell).
5. **Riesgos/pendientes** (si los hay).

Ejemplo de bloque por archivo:

```kotlin
// app/src/main/java/com/example/wodifyplus/ui/components/ExampleButton.kt
@Composable
fun ExampleButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("Click me")
    }
}
```

Ejemplo de diff:

```diff
--- a/app/src/main/java/com/example/wodifyplus/ui/screens/home/HomeScreen.kt
+++ b/app/src/main/java/com/example/wodifyplus/ui/screens/home/HomeScreen.kt
@@
-import androidx.compose.runtime.remember
+import androidx.compose.runtime.collectAsState
```

---

## 8) Límites y decisiones por defecto

* Si hay ambigüedad, **elige el camino mínimo** que compile y sea fácil de revertir.
* **No cambies** librerías ni stack salvo que lo pida.
* **No elimines** mis logs (`console.time`, etc.).
* Si algo es *heavy* (migraciones, rediseños), entrega primero un **RFC** con pros/contras.

---

## 9) Performance y UX (reglas rápidas)

* **Compose**: Evita recomposiciones innecesarias (`remember`, `derivedStateOf`, keys estables en `LazyColumn`).
* **Room**: Usa `Flow` para observar cambios, evita queries en main thread (Room ya lo previene, pero ojo con lógica bloqueante).
* **Imágenes**: Usa `AsyncImage` (Coil) o similar para carga asíncrona. Comprime assets.
* **Listas grandes**: `LazyColumn` / `LazyRow` (ya son virtualizados). Keys únicas en items.
* **WorkManager**: Configura constraints (red, batería) para no desperdiciar recursos.
* Mantén **tiempos de interacción < 100ms** en acciones comunes (clicks, navegación).

---

## 10) Integraciones específicas del repositorio

* **Room Database**:
  - NO uses `fallbackToDestructiveMigration()` en producción salvo que lo pida explícitamente
  - Crea migraciones (`Migration(X, Y)`) cuando cambies esquemas
  - Versiona correctamente la BD en `@Database(version = X)`
* **Chaquopy (Python)**:
  - Scripts Python en `src/main/python/`
  - Inicializa con `Python.start(AndroidPlatform(context))`
  - Maneja excepciones de Python desde Kotlin (pueden ser diferentes)
* **WorkManager**:
  - Usa `WorkManager.getInstance(context).enqueueUniquePeriodicWork()` para evitar duplicados
  - Constraints claros (`setRequiredNetworkType`, `setRequiresBatteryNotLow`)
* **Widget**:
  - Actualiza vía `AppWidgetManager.updateAppWidget()`
  - Datos desde `RemoteViews`, no puedes usar Compose directamente (Glance si migras)

---

## 11) Checklist de entrega

* [ ] Cambios en **patch/diff** o bloques por archivo.
* [ ] Compila y corre con `.\gradlew assembleDebug` o desde Android Studio.
* [ ] **README** actualizado (sección *Uso*, *Build*, *Features*).
* [ ] **strings.xml** actualizado si añadiste textos (no hardcodees strings en Composables).
* [ ] **AndroidManifest.xml** actualizado si añadiste permisos, activities, services, etc.
* [ ] Tests mínimos si aplica (JUnit para logic, Espresso/Compose UI tests si es UI).
* [ ] Notas de riesgo o siguientes pasos (migraciones pendientes, permisos nuevos, etc.).

---

## 12) Ejemplos de tareas típicas

* Añadir pantalla en Compose con navegación + ViewModel + estado (por ejemplo, pantalla de configuración).
* Crear nueva tabla en Room (Entity + DAO + Repository + migración).
* Refactor de Composable con state hoisting, sin romper API existente.
* Implementar WorkManager para tarea periódica (scraping, sincronización, notificaciones).
* Añadir widget de Android con actualización desde ViewModel.
* Integrar script Python con Chaquopy para procesamiento de datos.

---

## 13) Cómo priorizar

1. Que compile y funcione.
2. Que sea entendible y fácil de mantener.
3. Que sea rápido.
4. Que esté bonito.

---

## 14) Comunicación

* Si algo **no cuadra** (versiones, scripts faltantes, estructura extraña), dilo en el *Plan* y sugiere la mínima corrección.
* Mensajes cortos, bullets claros, código primero.

---

### Anexos

**Secretos y configuración**: Usa `local.properties` (Git-ignored) para API keys locales, o `BuildConfig` generado desde Gradle. Nunca hardcodees secretos en código.

**Assets**: Si generas drawables/iconos, entrega vectores (XML) cuando sea posible. Nombres descriptivos en `snake_case` (ej. `ic_home_24dp.xml`).

**Strings**: Externiza todos los textos a `res/values/strings.xml`. Prepara estructura para i18n (crea `values-es`, `values-en` si planeas multiidioma).

**Permisos**: Documenta en README qué permisos necesita la app y por qué (transparencia para usuarios).

**Dependencias**: Actualiza con prudencia. Revisa changelogs antes de saltar versiones mayores.

---

> Con este documento, puedes empezar a colaborar siguiendo estas reglas adaptadas a **Android nativo con Kotlin + Jetpack Compose**. Si el proyecto crece a incluir otros módulos (backend, Flutter, etc.), adapta las secciones pertinentes manteniendo el mismo formato de respuesta y el enfoque incremental.
