# WodifyPlus

Aplicación Android (Jetpack Compose) para gestionar tus WODs y actividades semanales: obtención desde scrapers Python (Chaquopy), selección por días/horas, calendario, notificaciones, estadísticas y widget del próximo entrenamiento.

## Características

- Home con próxima actividad destacada y navegación rápida
- Scrapers Python (CrossFit DB, N8) ejecutados in‑app con Chaquopy
- Selección de WODs por día; hora opcional con hora preferida desde Ajustes
- Actividades personalizadas (Gimnasio, OCR, Running, etc.) con días/horas y activar/desactivar
- Calendario semanal, marcar actividad como completada con métricas (kcal, distancia, duración, notas)
- Notificaciones configurables (p.ej. 60 min antes) con WorkManager
- Pantalla de Estadísticas (Vico Charts): totales y desglose por día/actividad
- Widget 3×3 con próxima actividad y contenido del WOD

## Requisitos

- Android Studio Koala o superior
- Android SDK 24+
- Python runtime Chaquopy 3.10 (configurado vía Gradle)

## Instalación rápida

1. Clona el repo y abre en Android Studio
2. Crea `app/src/main/python/` y copia tus scrapers (`.env`, `config.py`, `wod_scraper.py`, `crossfitdb.py`, `n8.py`)
3. Ejecuta en un dispositivo/emulador: Run ▶️ (debug)

## Configuración clave

- Gradle: Kotlin 2.0.21, Compose BOM 2024.02, Room 2.6.1, WorkManager 2.9.0, Vico 2.0.0‑alpha.28
- Chaquopy (app/build.gradle.kts): instala `python-dotenv`, `requests`, `beautifulsoup4`, `lxml`
- Firma debug: usa `~/.android/debug.keystore` (configurada en signingConfigs)

## Estructura principal

```
app/
  src/main/java/com/example/wodifyplus/
    data/ (Room, modelos, repositorio)
    ui/ (Compose: screens, components, navigation)
    notifications/ (WorkManager)
    widget/ (AppWidgetProvider)
  src/main/python/ (scrapers)
```

## Widget

- Tamaño recomendado: 3×3 (250×150dp)
- Muestra: gimnasio, fecha/hora y contenido del WOD
- Se actualiza al seleccionar/deseleccionar/editar hora/completar actividades
- Guía de debug: ver `WIDGET_DEBUG.md`

## Troubleshooting

- No aparecen scrapers: copia los `.py` a `app/src/main/python/`
- JSON con N8 mal parseado: se usa `org.json.JSONObject` en `HomeViewModel`
- Duplicados: `deleteAllWods()` antes de insertar
- Defaults de actividades: `SettingsViewModel.ensureDefaultConfigs()`
- Adaptive icons SDK < 26: usar `mipmap-anydpi-v26`
- Widget no carga: Logcat filtro `WodWidget` y ver `WIDGET_DEBUG.md`

## Privacidad y permisos

- INTERNET, ACCESS_NETWORK_STATE
- POST_NOTIFICATIONS (Android 13+), SCHEDULE_EXACT_ALARM (opcional)

## Licencia

MIT
