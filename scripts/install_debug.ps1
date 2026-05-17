# Script para instalar WodifyPlus Debug APK
# Uso: .\scripts\install_debug.ps1

Write-Host "🚀 Instalando WodifyPlus Debug..." -ForegroundColor Green

$apkPath = "app\build\outputs\apk\debug\app-debug.apk"

# Verificar que el APK existe
if (-not (Test-Path $apkPath)) {
    Write-Host "❌ Error: No se encontró el APK en $apkPath" -ForegroundColor Red
    Write-Host "💡 Ejecuta primero: .\gradlew assembleDebug" -ForegroundColor Yellow
    exit 1
}

# Verificar dispositivos
$devices = adb devices | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Host "❌ Error: No hay dispositivos Android conectados" -ForegroundColor Red
    Write-Host "💡 Conecta tu dispositivo y habilita 'Depuración USB'" -ForegroundColor Yellow
    exit 1
}

Write-Host "📱 Dispositivos conectados:" -ForegroundColor Cyan
$devices | ForEach-Object { Write-Host "  - $($_.Line)" -ForegroundColor Gray }

# Desinstalar versión anterior
Write-Host "`n🔄 Desinstalando versión anterior..." -ForegroundColor Yellow
adb uninstall com.example.wodifyplus 2>$null

# Instalar nueva versión
Write-Host "📦 Instalando APK Debug..." -ForegroundColor Yellow
adb install $apkPath

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ ¡WodifyPlus Debug instalado correctamente!" -ForegroundColor Green
    Write-Host "🎉 Ahora debería usar toda la pantalla y mostrar actividades del domingo" -ForegroundColor Cyan
    
    # Abrir la app
    Write-Host "`n🚀 Abriendo WodifyPlus..." -ForegroundColor Cyan
    adb shell am start -n com.example.wodifyplus/.MainActivity
    
    Write-Host "`n📋 Para ver logs de debugging:" -ForegroundColor Cyan
    Write-Host "  adb logcat | grep WodifyPlus" -ForegroundColor White
    Write-Host "  adb logcat | grep SelectionViewModel" -ForegroundColor White
} else {
    Write-Host "`n❌ Error al instalar el APK" -ForegroundColor Red
    Write-Host "💡 Verifica que el dispositivo tenga espacio suficiente" -ForegroundColor Yellow
    exit 1
}







