# Script para instalar WodifyPlus v1.1.0 Release
# Ejecutar como administrador si es necesario

Write-Host "🚀 Instalando WodifyPlus v1.1.0 Release..." -ForegroundColor Green

# Verificar que el APK existe
$apkPath = "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "❌ Error: No se encontró el APK en $apkPath" -ForegroundColor Red
    Write-Host "   Ejecuta primero: .\gradlew assembleRelease" -ForegroundColor Yellow
    exit 1
}

# Verificar que ADB está disponible
try {
    $adbVersion = adb version 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "ADB no encontrado"
    }
    Write-Host "✅ ADB encontrado" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: ADB no está instalado o no está en el PATH" -ForegroundColor Red
    Write-Host "   Instala Android SDK Platform Tools" -ForegroundColor Yellow
    exit 1
}

# Verificar dispositivos conectados
Write-Host "🔍 Verificando dispositivos conectados..." -ForegroundColor Cyan
$devices = adb devices
if ($devices -match "device$") {
    Write-Host "✅ Dispositivo Android conectado" -ForegroundColor Green
} else {
    Write-Host "❌ Error: No hay dispositivos Android conectados" -ForegroundColor Red
    Write-Host "   Conecta tu dispositivo y habilita la depuración USB" -ForegroundColor Yellow
    exit 1
}

# Desinstalar versión anterior si existe
Write-Host "🗑️ Desinstalando versión anterior..." -ForegroundColor Yellow
adb uninstall com.example.wodifyplus 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Versión anterior desinstalada" -ForegroundColor Green
} else {
    Write-Host "ℹ️ No había versión anterior instalada" -ForegroundColor Blue
}

# Instalar nueva versión
Write-Host "📱 Instalando WodifyPlus v1.1.0..." -ForegroundColor Cyan
adb install $apkPath

if ($LASTEXITCODE -eq 0) {
    Write-Host "🎉 ¡Instalación exitosa!" -ForegroundColor Green
    Write-Host "   WodifyPlus v1.1.0 está listo para usar" -ForegroundColor Green
    
    # Abrir la app
    Write-Host "🚀 Abriendo la aplicación..." -ForegroundColor Cyan
    adb shell am start -n com.example.wodifyplus/.MainActivity
    
    Write-Host ""
    Write-Host "📋 Resumen de la instalación:" -ForegroundColor Blue
    Write-Host "   • Versión: 1.1.0 (Código: 3)" -ForegroundColor White
    Write-Host "   • Tamaño: 64.1 MB" -ForegroundColor White
    Write-Host "   • Errores críticos corregidos" -ForegroundColor White
    Write-Host "   • Mejoras en UI/UX" -ForegroundColor White
    Write-Host ""
    Write-Host "¡Disfruta de tu nueva versión de WodifyPlus! 💪" -ForegroundColor Green
} else {
    Write-Host "❌ Error durante la instalación" -ForegroundColor Red
    Write-Host "   Verifica que el dispositivo esté conectado y la depuración USB esté habilitada" -ForegroundColor Yellow
    exit 1
}