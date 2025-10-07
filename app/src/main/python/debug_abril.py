#!/usr/bin/env python3
"""
Script para depuración específica de los WODs de abril
Ejecuta wodify.py con el parámetro debug_abril activado
"""

import os
import sys
import re
import json
import traceback
from datetime import datetime

# Añadir directorio actual al path para poder importar los módulos
script_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.append(script_dir)

# Imprimir banner 
print("=" * 80)
print("MODO DEBUG ABRIL - FORZANDO PROCESAMIENTO DE WODS DE ABRIL".center(80))
print("=" * 80)
print(f"Fecha de ejecución: {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
print(f"Directorio de trabajo: {script_dir}")
print("\nEste script está diseñado para forzar la detección de WODs de abril")
print("Se ignorarán los límites de fecha habituales y se procesarán todos los WODs de abril")
print("=" * 80 + "\n")

try:
    # Prueba de detección de fechas con abreviaturas
    print("\nPRUEBA DE DETECCIÓN DE FECHAS CON ABREVIATURAS:")
    
    # Importar n8 para probar funciones directamente
    from n8 import extraer_fecha_del_contenido
    
    casos_prueba = [
        "WOD 3 de abr",
        "WOD 15 de abr de 2023",
        "3 abr",
        "WOD abr 10",
        "WOD del 10/4/2023",
        "WOD mar 28"
    ]
    
    print("\nProbando extracción de fechas con abreviaturas:")
    for caso in casos_prueba:
        fecha_legible, fecha_iso, dia_semana = extraer_fecha_del_contenido(caso)
        print(f"✓ '{caso}' → {fecha_legible} ({fecha_iso})")
    
    # Importar módulos locales
    import wodify
    
    print("\nIniciando procesamiento con debug_abril=True...")
    
    # Llamar a la función con debug_abril=True
    resultado = wodify.enviar_wods(include_weekends=True, debug_abril=True)
    
    print("\n" + "=" * 80)
    print("RESULTADO DE LA EJECUCIÓN".center(80))
    print("=" * 80 + "\n")
    print(resultado)
    
    print("\nComprueba el archivo /sdcard/wods.html para ver los WODs procesados")
    print("\nSi no se encontraron WODs de abril, verifica:")
    print("1. Que haya contenido con 'abril' o 'abr' en el texto del WOD")
    print("2. Que los patrones regex estén capturando correctamente las fechas")
    print("3. Revisa los logs para ver si hay errores en la extracción de fechas")
    print("=" * 80)
    
except Exception as e:
    print("\n" + "=" * 80)
    print("ERROR DURANTE LA EJECUCIÓN".center(80))
    print("=" * 80 + "\n")
    print(f"Error: {str(e)}")
    print("\nDetalles del error:")
    traceback.print_exc()
    print("=" * 80) 