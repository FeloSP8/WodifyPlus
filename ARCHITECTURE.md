# Arquitectura

- Presentación: Jetpack Compose + ViewModel (AndroidViewModel cuando requiere contexto)
- Datos: Room (DAO/Entities), Repository como única fuente de acceso
- Preferencias: DataStore Preferences (hora preferida, minutos de notificación)
- Background: WorkManager para recordatorios
- Python: Chaquopy ejecuta scrapers y devuelve JSON parseado con `org.json`
- Widget: `AppWidgetProvider` actualiza con RemoteViews, logs con tag `WodWidget`

## Flujos principales

- Home obtiene WODs → parsea JSON → inserta en Room (borrando previos) → navega a Selección
- Selección filtra por actividades habilitadas/día, programa/cancela notificaciones
- Calendario permite editar hora, eliminar del día, completar con métricas
- Stats consume `repository.getCompletedWodsBetween(...)` y produce modelos para Vico

## Entidades clave

- `Wod`: datos + estado de selección/tiempo/completado/metrics
- `ActivityConfigEntity`: configuración por actividad (días, hora preferida, enabled)

## Notas

- Siempre que cambian WODs seleccionados/hora/completado → `WodWidgetProvider.updateAllWidgets`
- Default configs (CrossFit DB/N8) garantizadas al iniciar Ajustes
