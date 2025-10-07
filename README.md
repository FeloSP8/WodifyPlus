<div align="center">

# WodifyPlus 🏋️‍♂️📱

Gestiona tus entrenamientos semanales desde Android con una UI moderna en Compose: obtén WODs desde scrapers Python, planifícalos por día y hora, recibe recordatorios, registra métricas y consulta estadísticas. Incluye widget 3×3 con la próxima actividad.

</div>

---

## ✨ Funcionalidades clave

- 🏠 Home con la **próxima actividad** destacada y acceso rápido a obtener WODs
- 🐍 Scrapers Python (CrossFit DB, N8) con **Chaquopy** ejecutados en la app
- ✅ Selección de WODs por día con **hora opcional** (se preselecciona tu hora preferida)
- ⚙️ **Actividades personalizadas** (Gimnasio, OCR, Running…) con días/horas y activar/desactivar
- 📅 Calendario: marcar como **completado** e introducir **kcal, distancia, duración, notas**
- 🔔 **Notificaciones configurables** (p.ej. 60 min antes) vía WorkManager
- 📊 Estadísticas con **Vico Charts**: totales, por día de la semana y por actividad
- 🧩 **Widget 3×3**: gimnasio, fecha/hora y contenido del WOD

> Todo funciona offline una vez obtenidos los WODs; los scrapers solo requieren conexión durante la descarga.

---

## 🖼️ Capturas (preview)

> Sustituye estas rutas por tus capturas reales si quieres publicarlas.

| Home                 | Selección                 | Calendario               | Widget                 |
| -------------------- | ------------------------- | ------------------------ | ---------------------- |
| docs/images/home.png | docs/images/selection.png | docs/images/calendar.png | docs/images/widget.png |

---

## 🚀 Empezar en 3 pasos

1. Clona y abre en Android Studio (Koala o superior)
2. Copia los scrapers a `app/src/main/python/`:
   - `wod_scraper.py`, `crossfitdb.py`, `n8.py`, `config.py`, y tu `.env` (no se sube a Git)
3. Ejecuta en un dispositivo/emulador (Run ▶️). Obtén WODs desde Home.

> Si te faltan dependencias Python, Gradle/Chaquopy las instalará automáticamente (dotenv, requests, bs4, lxml).

---

## 🔧 Configuración técnica

- Kotlin 2.0.21 · Compose BOM 2024.02 · Room 2.6.1 · WorkManager 2.9.0 · Vico 2.0.0‑alpha.28
- Chaquopy 15.x con Python 3.10
- Firma debug: `~/.android/debug.keystore` (configurada en `signingConfigs`)

### Estructura

```
app/
  src/main/java/com/example/wodifyplus/
    data/            # Room, modelos, repositorio
    ui/              # Compose: screens, components, navigation
    notifications/   # WorkManager
    widget/          # AppWidgetProvider (RemoteViews)
  src/main/python/   # Scrapers (no commit de .env)
```

### Variables de entorno (.env)

Guárdalas en `app/src/main/python/.env` (ejemplo):

```
CROSSFITDB_URL=...
N8_URL=...
AUTH_TOKEN=...
```

> Importante: `.env` está ignorado en `.gitignore` y no se sube al repo.

---

## 🧩 Widget 3×3

- Tamaño recomendado: 3×3 (≈ 250×150dp)
- Muestra: gimnasio, fecha/hora y contenido del WOD
- Se actualiza automáticamente al seleccionar/deseleccionar, cambiar hora o completar
- Guía de depuración y comandos ADB: `WIDGET_DEBUG.md`

---

## 🏗️ Arquitectura (resumen)

- UI: Jetpack Compose + ViewModel
- Persistencia: Room (DAO/Entities) y Repository
- Preferencias: DataStore (hora preferida, minutos de aviso)
- Tareas: WorkManager para recordatorios
- Scrapers: Chaquopy ejecuta Python y la app parsea JSON con `org.json`

Más detalles: `ARCHITECTURE.md`

---

## 🆘 Troubleshooting rápido

- No aparecen WODs: confirma que copiaste los `.py` y `.env` a `app/src/main/python/`
- N8 mal parseado: usamos `org.json.JSONObject` (robusto) en `HomeViewModel`
- Duplicados: borramos con `deleteAllWods()` antes de insertar
- Defaults de actividades: garantizados en Ajustes al iniciar
- Iconos Adaptive en SDK < 26: se sirven desde `mipmap-anydpi-v26`
- Widget no carga: Logcat filtro `WodWidget` y guía en `WIDGET_DEBUG.md`

---

## 📌 Roadmap breve

- [ ] Exportar/Importar datos de actividades completadas
- [ ] Sincronización opcional con nube
- [ ] Más fuentes de scrapers y selector de origen
- [ ] Tests instrumentados de navegación y DB

---

## 🤝 Contribuir

Consulta `CONTRIBUTING.md`. Pull Requests y feedback son bienvenidos.

---

## 🔐 Permisos

- `INTERNET`, `ACCESS_NETWORK_STATE`
- `POST_NOTIFICATIONS` (Android 13+)
- `SCHEDULE_EXACT_ALARM` (según OEM)

---

## 📄 Licencia

MIT
