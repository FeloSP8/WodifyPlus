import argparse
import traceback
import sys
import os

# Añadir directorio actual al path para poder importar los módulos
script_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.append(script_dir)

# Importar módulos locales
import n8
import crossfitdb

def enviar_wods(include_weekends=None, include_crossfitdb=None, debug_abril=False):
    """
    Función principal para obtener y enviar los WODs
    :param include_weekends: Si se deben incluir los fines de semana
    :param include_crossfitdb: Si se deben incluir los WODs de CrossFitDB
    :param debug_abril: Modo para forzar la procesamiento de fechas de abril
    :return: String con el resultado del proceso
    """
    try:
        wods_html = ""
        resultados = []
        hay_wods = False
        
        # Obtener WODs de N8
        print("📱 Obteniendo WODs de N8...")
        try:
            wods_n8 = n8.main(include_weekends=include_weekends, debug_abril=debug_abril)
            if wods_n8:
                hay_wods = True
                for wod in wods_n8:
                    wods_html += wod["contenido_html"] + "<br><br>"
                resultados.append(f"✅ Se encontraron {len(wods_n8)} WODs de N8")
            else:
                resultados.append("❌ No se encontraron WODs de N8")
        except Exception as e:
            print(f"Error al obtener WODs de N8: {str(e)}")
            traceback.print_exc()
            resultados.append(f"❌ Error al obtener WODs de N8: {str(e)}")
            
        # Obtener WODs de CrossFitDB (opcional)
        if include_crossfitdb:
            print("📱 Obteniendo WODs de CrossFitDB...")
            try:
                wods_crossfitdb = crossfitdb.main(include_weekends=include_weekends)
                if wods_crossfitdb:
                    hay_wods = True
                    for wod in wods_crossfitdb:
                        wods_html += wod["contenido_html"] + "<br><br>"
                    resultados.append(f"✅ Se encontraron {len(wods_crossfitdb)} WODs de CrossFitDB")
                else:
                    resultados.append("❌ No se encontraron WODs de CrossFitDB")
            except Exception as e:
                print(f"Error al obtener WODs de CrossFitDB: {str(e)}")
                traceback.print_exc()
                resultados.append(f"❌ Error al obtener WODs de CrossFitDB: {str(e)}")
        
        if hay_wods:
            # Crear mensaje con todos los WODs
            mensaje_html = f"""
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {{ font-family: Arial, sans-serif; }}
                    .wod-container {{ margin-bottom: 20px; }}
                </style>
            </head>
            <body>
                <div class="wod-container">
                    {wods_html}
                </div>
            </body>
            </html>
            """
            
            # Guardar en archivo para debug
            with open("/sdcard/wods.html", "w", encoding="utf-8") as f:
                f.write(mensaje_html)
            resultados.append("✅ Archivo HTML guardado en /sdcard/wods.html")
            
        return "\n".join(resultados)
    except Exception as e:
        print(f"Error general: {str(e)}")
        traceback.print_exc()
        return f"❌ Error general: {str(e)}"

def main():
    """
    Punto de entrada para el script
    """
    parser = argparse.ArgumentParser(description='Obtener y enviar WODs')
    parser.add_argument('--include-weekends', action='store_true', help='Incluir fines de semana')
    parser.add_argument('--include-crossfitdb', action='store_true', help='Incluir WODs de CrossFitDB')
    parser.add_argument('--debug-abril', action='store_true', help='Forzar procesamiento de fechas de abril')
    args = parser.parse_args()
    
    resultado = enviar_wods(
        include_weekends=args.include_weekends, 
        include_crossfitdb=args.include_crossfitdb,
        debug_abril=args.debug_abril
    )
    print(resultado) 