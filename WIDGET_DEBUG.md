# 🐛 Cómo Debuggear el Widget en Android Studio

## Ver logs del widget con Logcat

1. **Abre Android Studio**
2. **Ve a View → Tool Windows → Logcat** (o Alt+6)
3. **Filtra por "WodWidget"** en el campo de búsqueda
4. **Instala la app** en tu dispositivo/emulador
5. **Añade el widget** a la pantalla de inicio

## Logs que verás:

```
WodWidget: Widget enabled
WodWidget: updateAllWidgets called
WodWidget: Updating 1 widgets
WodWidget: onUpdate called with 1 widgets
WodWidget: updateAppWidget called for id: 123
WodWidget: Found X selected wods
WodWidget: Next WOD: CrossFit DB - 2025-01-08
WodWidget: Widget updated successfully
```

## Si el widget NO carga:

### Problema 1: No hay logs

- El widget no se está registrando
- **Solución**: Desinstala la app completamente y reinstala

### Problema 2: "Found 0 selected wods"

- No tienes WODs seleccionados en el calendario
- **Solución**: Ve a la app → Obtén WODs → Selecciona actividades con hora

### Problema 3: "No upcoming activities found"

- Todos los WODs están en el pasado o completados
- **Solución**: Selecciona un WOD futuro con hora

### Problema 4: "Error al cargar datos"

- Hay un problema con la base de datos
- **Mira el stack trace** en Logcat para más detalles

## Forzar actualización del widget manualmente:

1. En la app, ve a **Selección** o **Calendario**
2. **Cambia la hora** de un WOD o **selecciona/deselecciona** uno
3. El widget debería actualizarse automáticamente
4. **Mira Logcat** para confirmar: `updateAllWidgets called`

## Comandos ADB útiles:

```bash
# Ver todos los widgets instalados
adb shell dumpsys appwidget

# Ver logs en tiempo real (solo widget)
adb logcat -s WodWidget:*

# Limpiar datos de la app (resetea todo)
adb shell pm clear com.example.wodifyplus
```

## Resetear widget completamente:

1. **Mantén presionado** el widget en la pantalla
2. **Arrastra** a "Eliminar" o "X"
3. **Añade el widget** de nuevo
4. **Mira Logcat** para ver "Widget enabled"

## Si sigue sin funcionar:

1. **Desinstala** la app completamente
2. **Limpia el build**: `.\gradlew clean`
3. **Reinstala**: `.\gradlew installDebug`
4. **Añade el widget** y mira Logcat desde el principio

---

**Nota**: El widget se actualiza automáticamente:

- Cada 30 minutos (configurado en `wod_widget_info.xml`)
- Cuando seleccionas/deseleccionas un WOD
- Cuando cambias la hora de un WOD
- Cuando completas una actividad
