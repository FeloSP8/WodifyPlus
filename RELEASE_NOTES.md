# WodifyPlus v1.0.2 - Release Notes

## 📱 Información de la Release

- **Versión**: 1.0.2
- **Build**: Release
- **Tamaño**: ~64 MB
- **Fecha**: 09/10/2025 (13:14)
- **Arquitecturas**: ARM64, ARMv7, x86, x86_64

## 🚀 Características Principales

### ✅ Funcionalidades Completadas

- **Gestión de WODs**: Obtención automática desde CrossFit DB y N8
- **Planificación Semanal**: Selección de actividades día a día con hora opcional
- **Actividades Personalizadas**: CRUD completo con configuración de días y horarios
- **Notificaciones**: Sistema configurable con WorkManager
- **Widget 3x3**: Próxima actividad en pantalla de inicio (completamente clickeable)
- **Estadísticas**: Gráficos con Vico Charts (totales, por día, por actividad)
- **Calendario**: Vista semanal con opción de marcar como completada
- **Métricas**: Registro de calorías, distancia, duración y notas

### 🔧 Mejoras Técnicas

- **Optimización**: Minificación y compresión de recursos habilitada
- **Firma**: APK firmado para distribución
- **ProGuard**: Reglas optimizadas para mantener funcionalidad
- **Python Integration**: Chaquopy 15.0.1 con Python 3.10
- **Base de Datos**: Room con migración automática
- **UI**: Jetpack Compose con Material 3
- **Edge-to-Edge**: Pantalla completa con barra de estado visible
- **Debugging**: Logging detallado para troubleshooting

## 📋 Instalación

1. **Habilitar fuentes desconocidas** en tu dispositivo Android
2. **Instalar** `app-release.apk` desde la carpeta `app/build/outputs/apk/release/`
3. **Permisos**: La app solicitará permisos de notificaciones al iniciar

## 🎯 Uso Rápido

1. **Primera vez**: Ve a "Obtener WODs" para descargar actividades
2. **Configurar**: Ajustes → Gestionar actividades (activar/desactivar, configurar días)
3. **Seleccionar**: Ve a "Selección" para elegir actividades de la semana
4. **Widget**: Añade el widget "WodifyPlus" a tu pantalla de inicio
5. **Completar**: En "Calendario" marca actividades como completadas

## 🐛 Problemas Conocidos

- Algunos iconos Material muestran warnings de deprecación (no afectan funcionalidad)
- El widget puede tardar unos segundos en actualizarse tras cambios

## 📞 Soporte

- **Issues**: Reporta problemas en el repositorio GitHub
- **Logs**: Usa `adb logcat | grep WodifyPlus` para debugging
- **Widget Debug**: Consulta `WIDGET_DEBUG.md` para problemas del widget

## 🔄 Próximas Versiones

- [ ] Sincronización en la nube
- [ ] Temas personalizables
- [ ] Exportación de estadísticas
- [ ] Integración con Google Fit
- [ ] Modo offline mejorado

---

**¡Disfruta entrenando con WodifyPlus! 💪**
