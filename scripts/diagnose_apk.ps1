# Script para diagnosticar problemas de instalación de APK
# Uso: .\scripts\diagnose_apk.ps1

Write-Host "🔍 Diagnóstico de APK WodifyPlus" -ForegroundColor Cyan

# Verificar dispositivos
Write-Host "`n📱 Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = adb devices | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Host "❌ No hay dispositivos conectados" -ForegroundColor Red
    Write-Host "💡 Conecta tu dispositivo y habilita 'Depuración USB'" -ForegroundColor Yellow
    Write-Host "💡 También habilita 'Instalar vía USB' en opciones de desarrollador" -ForegroundColor Yellow
    exit 1
} else {
    Write-Host "✅ Dispositivos encontrados:" -ForegroundColor Green
    $devices | ForEach-Object { Write-Host "  - $($_.Line)" -ForegroundColor Gray }
}

# Verificar APKs disponibles
Write-Host "`n📦 Verificando APKs disponibles..." -ForegroundColor Yellow
$debugApk = "app\build\outputs\apk\debug\app-debug.apk"
$releaseApk = "app\build\outputs\apk\release\wodifyplus.apk"

if (Test-Path $debugApk) {
    $debugInfo = Get-ChildItem $debugApk
    Write-Host "✅ Debug APK encontrado:" -ForegroundColor Green
    Write-Host "  Archivo: $($debugInfo.Name)" -ForegroundColor Gray
    Write-Host "  Tamaño: $([math]::Round($debugInfo.Length/1MB,2)) MB" -ForegroundColor Gray
    Write-Host "  Fecha: $($debugInfo.LastWriteTime)" -ForegroundColor Gray
} else {
    Write-Host "❌ Debug APK no encontrado" -ForegroundColor Red
}

if (Test-Path $releaseApk) {
    $releaseInfo = Get-ChildItem $releaseApk
    Write-Host "✅ Release APK encontrado:" -ForegroundColor Green
    Write-Host "  Archivo: $($releaseInfo.Name)" -ForegroundColor Gray
    Write-Host "  Tamaño: $([math]::Round($releaseInfo.Length/1MB,2)) MB" -ForegroundColor Gray
    Write-Host "  Fecha: $($releaseInfo.LastWriteTime)" -ForegroundColor Gray
} else {
    Write-Host "❌ Release APK no encontrado" -ForegroundColor Red
}

# Verificar información del dispositivo
Write-Host "`n📱 Información del dispositivo:" -ForegroundColor Yellow
$androidVersion = adb shell getprop ro.build.version.release
$sdkVersion = adb shell getprop ro.build.version.sdk
$architecture = adb shell getprop ro.product.cpu.abi
$model = adb shell getprop ro.product.model

Write-Host "  Android: $androidVersion (API $sdkVersion)" -ForegroundColor Gray
Write-Host "  Arquitectura: $architecture" -ForegroundColor Gray
Write-Host "  Modelo: $model" -ForegroundColor Gray

# Verificar si la app ya está instalada
Write-Host "`n🔍 Verificando instalación actual..." -ForegroundColor Yellow
$installed = adb shell pm list packages | Select-String "com.example.wodifyplus"
if ($installed) {
    Write-Host "⚠️  La app ya está instalada" -ForegroundColor Yellow
    $version = adb shell dumpsys package com.example.wodifyplus | Select-String "versionName"
    if ($version) {
        Write-Host "  Versión actual: $($version.Line.Split('=')[1].Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "ℹ️  La app no está instalada" -ForegroundColor Blue
}

# Probar instalación con debug primero
Write-Host "`n🚀 Probando instalación con APK Debug..." -ForegroundColor Yellow
if (Test-Path $debugApk) {
    Write-Host "Desinstalando versión anterior..." -ForegroundColor Gray
    adb uninstall com.example.wodifyplus 2>$null
    
    Write-Host "Instalando APK Debug..." -ForegroundColor Gray
    adb install $debugApk
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ APK Debug instalado correctamente!" -ForegroundColor Green
        Write-Host "🎉 La app debería funcionar ahora" -ForegroundColor Cyan
        
        $openApp = Read-Host "`n¿Abrir la app ahora? (s/n)"
        if ($openApp -eq "s" -or $openApp -eq "S" -or $openApp -eq "y" -or $openApp -eq "Y") {
            Write-Host "🚀 Abriendo WodifyPlus..." -ForegroundColor Cyan
            adb shell am start -n com.example.wodifyplus/.MainActivity
        }
    } else {
        Write-Host "❌ Error al instalar APK Debug" -ForegroundColor Red
        Write-Host "💡 Posibles soluciones:" -ForegroundColor Yellow
        Write-Host "  1. Habilita 'Instalar vía USB' en opciones de desarrollador" -ForegroundColor White
        Write-Host "  2. Habilita 'Fuentes desconocidas' en configuración" -ForegroundColor White
        Write-Host "  3. Verifica que tienes espacio suficiente" -ForegroundColor White
        Write-Host "  4. Reinicia el dispositivo" -ForegroundColor White
    }
} else {
    Write-Host "❌ No se puede probar: APK Debug no encontrado" -ForegroundColor Red
}

Write-Host "`n📋 Resumen:" -ForegroundColor Cyan
Write-Host "  - Usa APK Debug para desarrollo y pruebas" -ForegroundColor White
Write-Host "  - Usa APK Release para distribución final" -ForegroundColor White
Write-Host "  - Si hay problemas, verifica permisos del dispositivo" -ForegroundColor White

