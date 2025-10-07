# Contribuir a WodifyPlus

## Requisitos

- Android Studio actualizado
- Kotlin 2.0.x, Gradle 8.11.1
- Dispositivo/emulador Android 7.0+

## Flujo de trabajo

1. Crea una rama desde `main`: `feat/...`, `fix/...`, `docs/...`
2. Asegúrate de compilar: `./gradlew assembleDebug`
3. Pasa linters/format si aplica
4. Abre PR con descripción clara y capturas si UI

## Estilo de código

- Kotlin claro y legible; nombres descriptivos
- Evitar `any`, preferir tipos explícitos en APIs públicas
- Manejar errores sin silenciarlos
- Comentarios solo para decisiones no obvias

## Commits

- Mensajes concisos: `feat(widget): añadir 3x3 con contenido completo`
- Un cambio lógico por commit

## Documentación

- Actualiza `README.md`, `CHANGELOG.md` y `WIDGET_DEBUG.md` cuando proceda
