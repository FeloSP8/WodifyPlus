import os
import sys
from datetime import datetime, timedelta
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from config import EMAIL_CONFIG
import re

def log_message(message, tag="WodScraper"):
    """Loggea a consola o a Logcat si está en Android."""
    try:
        # Intentar importar Log de Android
        from android.util import Log
        Log.d(tag, str(message))
    except ImportError:
        # Si no está en Android, imprimir a consola
        print(f"[{tag}] {message}")

# Función para formatear ejercicios en CamelCase
def formatear_ejercicio(texto):
    """Convierte el texto de ejercicios a formato CamelCase profesional"""
    if not texto:
        return ""
    
    # Dividir en palabras
    palabras = texto.strip().split()
    resultado = []
    
    # Palabras que deben mantenerse en mayúsculas (abreviaturas, términos técnicos)
    mantener_mayusculas = ["KB", "DB", "RX", "AMRAP", "EMOM", "DU", "HSPU", "BMU", "TTB", "T2B", "C2B", "WB", "BOX", "YGIG", "SC", "KBSR"]
    
    for palabra in palabras:
        # Verificar si es una palabra que debe mantenerse en mayúsculas
        if palabra.upper() in mantener_mayusculas:
            resultado.append(palabra.upper())
        # Verificar si contiene RX o TC seguido de números/caracteres especiales
        elif palabra.upper().startswith("RX") or palabra.upper().startswith("TC"):
            resultado.append(palabra.upper())
        # Verificar si es una medida con número (10kg, 15m, 20cal, etc.)
        elif any(c.isdigit() for c in palabra):
            # Mantener números y convertir unidades a minúsculas
            resultado.append(palabra.lower())
        else:
            # Primera letra mayúscula, resto minúsculas
            resultado.append(palabra.capitalize())
    
    return " ".join(resultado)

def enviar_correo_unificado(wods_n8, wods_crossfitdb, lunes_fmt, viernes_fmt):
    """
    Envía un correo con los WODs de ambos gimnasios en un formato elegante,
    agrupados por día de la semana.
    """
    try:
        # Crear el mensaje
        mensaje = MIMEMultipart()
        mensaje["From"] = EMAIL_CONFIG["remitente"]
        mensaje["To"] = EMAIL_CONFIG["destinatario"]
        mensaje["Subject"] = f"WODs de la Semana - CrossFit DB y N8 🏋️‍♂️"

        # Plantilla HTML con estilos modernos
        html_template = """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <style>
                body {{
                    font-family: 'Segoe UI', Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 800px;
                    margin: 0 auto;
                }}
                .header {{
                    text-align: center;
                    padding: 20px;
                    background: linear-gradient(135deg, #6650a4 0%, #4a3b82 100%);
                    color: white;
                    border-radius: 10px;
                    margin-bottom: 30px;
                }}
                .day-section {{
                    margin-bottom: 40px;
                    border: 1px solid #e0e0e0;
                    border-radius: 10px;
                    overflow: hidden;
                }}
                .day-header {{
                    background-color: #6650a4;
                    color: white;
                    padding: 15px;
                    font-size: 1.2em;
                    font-weight: bold;
                    text-align: center;
                }}
                .gym-section {{
                    border-top: 1px solid #eee;
                    margin-bottom: 10px;
                }}
                .gym-header {{
                    background-color: #ede7f6; /* Morado más claro */
                    padding: 12px 15px;
                    font-size: 1.1em;
                    font-weight: bold;
                    color: #4a3b82; /* Mantener texto morado oscuro */
                    display: flex;
                    align-items: center;
                }}
                .wod-content {{
                    padding: 20px;
                    background-color: #fff;
                }}
                .workout-type {{
                    font-weight: bold;
                    color: #4a3b82;
                    margin: 10px 0;
                }}
                .workout-details {{
                    margin-left: 15px;
                    color: #555;
                }}
                .footer {{
                    text-align: center;
                    padding: 20px;
                    color: #666;
                    font-size: 0.9em;
                }}
                .version {{
                    text-align: center;
                    color: #999;
                    font-size: 0.8em;
                    margin-top: 10px;
                }}
                .no-wod {{
                    padding: 15px;
                    color: #777;
                    font-style: italic;
                    text-align: center;
                }}
                .logo-img {{
                    height: 30px;
                    vertical-align: middle;
                    margin-right: 10px;
                    display: inline-block;
                }}
            </style>
        </head>
        <body>
            <div class="header">
                <h1>
                    <img src="https://raw.githubusercontent.com/FeloSP8/wod-scraper-app/refs/heads/main/app/src/main/logo%20db%20negro.png" 
                         alt="CrossFit DB" 
                         style="height: 40px; vertical-align: middle; margin-right: 15px;">
                    WODs {lunes_fmt} - {viernes_fmt}
                    <img src="https://raw.githubusercontent.com/FeloSP8/wod-scraper-app/refs/heads/main/app/src/main/converted_image_transparent.png" 
                         alt="Box N8" 
                         style="height: 40px; vertical-align: middle; margin-left: 15px;">
                </h1>
                <p>CrossFit DB y Box N8</p>
            </div>
            
            {contenido}
            
            <div class="footer">
                Generado por WOD Scraper v4.1.0<br>
                {fecha_generacion}
            </div>
        </body>
        </html>
        """

        # Formatear el contenido del WOD para HTML
        def generar_html_wod(wod):
            if wod and 'contenido' in wod:
                # Si ya existe contenido_html, usarlo
                if 'contenido_html' in wod and wod['contenido_html']:
                    # Este contenido ya tiene formato HTML compatible
                    return wod['contenido_html']
                
                # Si no hay formato HTML, aplicar uno básico
                # Procesar por líneas para detectar tipos de entrenamiento
                lineas = wod['contenido'].split('\n')
                html_lines = []
                
                # Categorías principales a detectar y formatear
                categorias_principales = ["STRENGTH", "METCON", "EMOM", "AMRAP", "TABATA", "FOR TIME", "SKILL"]
                
                for linea in lineas:
                    linea = linea.strip()
                    if not linea:
                        continue
                    
                    # Verificar si es una categoría principal
                    es_categoria = False
                    for cat in categorias_principales:
                        if cat in linea.upper() and (linea.upper().startswith(cat) or linea.upper() == cat):
                            es_categoria = True
                            html_lines.append(f'<div style="color: #000000; font-weight: 700; background-color: #f5f5f5; padding: 8px 12px; margin: 10px 0; border-left: 2px solid #2980b9;">{linea.upper()}</div>')
                            break
                    
                    if not es_categoria:
                        # Si no es categoría, formatear como detalle con CamelCase
                        linea_formateada = formatear_ejercicio(linea)
                        html_lines.append(f'<div style="margin-left: 20px; padding: 5px 0; color: #34495e;">{linea_formateada}</div>')
                
                if html_lines:
                    return "\n".join(html_lines)
                else:
                    # Si no se pudo formatear, devolver el texto como párrafo simple
                    contenido = formatear_ejercicio(wod['contenido'].replace('\n', '<br>'))
                    return f'<div style="margin: 10px 0;">{contenido}</div>'
            
            return ""

        # Función específica para formatear los WODs de N8
        def generar_html_wod_n8(wod):
            if wod and 'contenido' in wod:
                # Procesar por líneas para detectar tipos de entrenamiento
                lineas = wod['contenido'].split('\n')
                html_lines = []
                
                # Categorías y patrones a detectar para N8
                patrones_categorias = [
                    r'^[A-Z]\)\s*(.*)',                 # A) cualquier cosa
                    r'^[A-Z]\.\)\s*(.*)',               # A.) cualquier cosa 
                    r'^(?:EMOM|AMRAP)\s*\d+[\'"]?',     # EMOM/AMRAP seguido de números y opcional '/"
                    r'^(?:TABATA|FOR TIME)',            # TABATA, FOR TIME exactos
                    r'^.*\bSKILL\b.*',                  # Cualquier cosa con SKILL
                    r'^STRETCH',                        # STRETCH exacto
                    r'^ROPE CLIMB',                     # ROPE CLIMB
                    r'^STRENGTH',                       # STRENGTH exacto
                    r'(?i)^["\']?team\s+of\s+\d+["\']?' # Team of X con comillas opcionales y case insensitive
                ]
                
                in_section = False  # Para llevar el seguimiento de si estamos dentro de una sección
                
                for linea in lineas:
                    linea = linea.strip()
                    if not linea:
                        continue
                    
                    # Comprobar si es una categoría/título de sección
                    es_categoria = False
                    for patron in patrones_categorias:
                        if re.search(patron, linea.upper()):
                            es_categoria = True
                            # Añadir div con margen antes de cada categoría
                            html_lines.append('<div style="margin-top: 15px;"></div>')
                            # Aplicar estilo de workout-type igual que CrossFitDB
                            html_lines.append(f'<div style="color: #000000; font-weight: 700; background-color: #f5f5f5; padding: 8px 12px; margin: 10px 0; border-left: 2px solid #2980b9; display: block;">{linea}</div>')
                            in_section = True
                            break
                    
                    if not es_categoria:
                        # Si no es categoría, formatear en CamelCase
                        linea_formateada = formatear_ejercicio(linea)
                        if in_section:
                            html_lines.append(f'<div class="workout-details" style="margin-left: 15px; color: #34495e; padding: 4px 0;">{linea_formateada}</div>')
                        else:
                            # Contenido normal sin sección
                            html_lines.append(f'<div style="margin-left: 10px; padding: 5px 0; color: #34495e;">{linea_formateada}</div>')
                
                if html_lines:
                    return "\n".join(html_lines)
                else:
                    # Si no se pudo formatear, devolver el texto como párrafo simple
                    contenido = formatear_ejercicio(wod['contenido'].replace('\n', '<br>'))
                    return f'<div style="margin: 10px 0;">{contenido}</div>'
            
            return ""

        # Preparar los WODs para agruparlos por día
        # Convertir listas None a listas vacías para evitar errores
        wods_n8 = [] if wods_n8 is None else wods_n8
        wods_crossfitdb = [] if wods_crossfitdb is None else wods_crossfitdb
        
        # Combinar los WODs de ambos gimnasios y ordenarlos por día
        todos_wods = wods_n8 + wods_crossfitdb
        
        # Agrupar por fecha
        wods_por_dia = {}
        for wod in todos_wods:
            fecha_clave = wod.get('fecha_iso', '')
            if fecha_clave not in wods_por_dia:
                wods_por_dia[fecha_clave] = {
                    'fecha': wod.get('fecha'),
                    'fecha_formateada': wod.get('fecha_formateada'),
                    'dia_semana': wod.get('dia_semana'),
                    'wods_por_gimnasio': {
                        'N8': None,
                        'CrossFitDB': None
                    }
                }
            
            gimnasio = wod.get('gimnasio')
            if gimnasio:
                wods_por_dia[fecha_clave]['wods_por_gimnasio'][gimnasio] = wod
        
        # Ordenar por fecha
        fechas_ordenadas = sorted(wods_por_dia.keys())
        
        # Construir el contenido HTML
        contenido_html = ""
        
        if fechas_ordenadas:
            for fecha_clave in fechas_ordenadas:
                dia_info = wods_por_dia[fecha_clave]
                
                # Formatea la fecha como DD/MM/YYYY
                fecha_obj = dia_info.get('fecha')
                fecha_mostrar = ""
                
                if fecha_obj:
                    # Si tenemos un objeto datetime, formatearlo correctamente
                    fecha_mostrar = fecha_obj.strftime("%d/%m/%Y")
                else:
                    # Si no, usar la fecha formateada disponible
                    fecha_mostrar = dia_info['fecha_formateada']
                
                # Sección para este día
                contenido_html += f"""
                <div class="day-section">
                    <div class="day-header">
                        {dia_info['dia_semana']} {fecha_mostrar}
                    </div>
                """
                
                # Contenido de CrossFitDB para este día
                contenido_html += """
                    <div class="gym-section">
                        <div class="gym-header">
                            <img src="https://raw.githubusercontent.com/FeloSP8/wod-scraper-app/refs/heads/main/app/src/main/logo%20db%20negro.png" 
                                 alt="CrossFit DB" class="logo-img">
                            <span>CrossFit DB</span>
                        </div>
                """
                
                wod_crossfitdb = dia_info['wods_por_gimnasio']['CrossFitDB']
                if wod_crossfitdb:
                    contenido_html += f"""
                        <div class="wod-content">
                            {generar_html_wod(wod_crossfitdb)}
                        </div>
                    """
                else:
                    contenido_html += """
                        <div class="no-wod">
                            No hay WOD disponible para este día
                        </div>
                    """
                
                contenido_html += "</div>"  # Fin de CrossFitDB
                
                # Contenido de N8 para este día
                contenido_html += """
                    <div class="gym-section">
                        <div class="gym-header">
                            <img src="https://raw.githubusercontent.com/FeloSP8/wod-scraper-app/refs/heads/main/app/src/main/converted_image_transparent.png" 
                                 alt="Box N8" class="logo-img">
                            <span>Box N8</span>
                        </div>
                """
                
                wod_n8 = dia_info['wods_por_gimnasio']['N8']
                if wod_n8:
                    contenido_html += f"""
                        <div class="wod-content">
                            {generar_html_wod_n8(wod_n8)}
                        </div>
                    """
                else:
                    contenido_html += """
                        <div class="no-wod">
                            No hay WOD disponible para este día
                        </div>
                    """
                
                contenido_html += "</div>"  # Fin de N8
                
                contenido_html += "</div>"  # Fin de este día
        else:
            contenido_html = """
            <div class="day-section">
                <div class="day-header">
                    Sin WODs disponibles
                </div>
                <div class="no-wod">
                    No se encontraron WODs para esta semana
                </div>
            </div>
            """

        # Formatear la fecha actual
        fecha_generacion = datetime.now().strftime("%d/%m/%Y %H:%M")

        # Reemplazar el contenido en la plantilla
        html_final = html_template.format(
            contenido=contenido_html,
            fecha_generacion=fecha_generacion,
            lunes_fmt=lunes_fmt,
            viernes_fmt=viernes_fmt
        )

        # Adjuntar el HTML al mensaje
        mensaje.attach(MIMEText(html_final, "html"))

        # Configurar el servidor SMTP y enviar el correo
        with smtplib.SMTP(EMAIL_CONFIG["servidor_smtp"], EMAIL_CONFIG["puerto_smtp"]) as servidor:
            servidor.starttls()
            servidor.login(EMAIL_CONFIG["remitente"], EMAIL_CONFIG["contraseña"])
            servidor.send_message(mensaje)

        return "✅ Correo enviado correctamente"

    except Exception as e:
        return f"❌ Error al enviar correo: {str(e)}"

# Función para formatear correctamente nombres de días y meses
def formatear_nombre_propio(texto):
    """Convierte texto a formato Capital Case (Primera letra mayúscula, resto minúscula)"""
    if not texto:
        return texto
    return texto[0].upper() + texto[1:].lower()

def main(include_weekends=None):
    result = "🏋️ WOD Scraper Unificado v3.0.2\n"
    result += "=" * 42 + "\n"

    try:
        # Intentar obtener el directorio de archivos de Android si estamos en la app
        try:
            from com.chaquo.python import Python
            app_files_dir = str(Python.getPlatform().getApplication().getFilesDir())
            result += f"📁 Ejecutando en Android: {app_files_dir}\n"
        except ImportError:
            # Si no estamos en Android, usar el directorio actual
            app_files_dir = os.getcwd()
            result += f"📁 Ejecutando en entorno local: {app_files_dir}\n"

        # Obtener rango de fechas para mostrar
        hoy = datetime.now()
        # Si es domingo, obtener el rango de la próxima semana
        if hoy.weekday() == 6:  # 6 = domingo
            lunes = hoy + timedelta(days=1)  # El siguiente lunes
        else:
            # Para otros días, obtener el lunes de la semana actual
            dias_hasta_lunes = hoy.weekday()
            lunes = hoy - timedelta(days=dias_hasta_lunes)
        
        # Siempre usar domingo (7 días)
        domingo = lunes + timedelta(days=6)
        viernes_fmt = domingo.strftime("%d/%m/%Y")
        lunes_fmt = lunes.strftime("%d/%m/%Y")
        result += f"🗓️ Buscando WODs: {lunes_fmt} al {viernes_fmt}\n\n"

        # Import the modules directly instead of using subprocess
        result += "📱 Obteniendo WODs de N8...\n"
        try:
            import n8
            wods_n8 = n8.main(log_func=lambda msg: log_message(msg, tag="WodN8"))
            if wods_n8:
                # Asegurar que los días y meses estén correctamente formateados
                for wod in wods_n8:
                    wod['dia_semana'] = formatear_nombre_propio(wod['dia_semana'])
                result += f"✅ Se encontraron {len(wods_n8)} WODs de N8\n"
            else:
                result += "⚠️ No se encontraron WODs de N8\n"
        except Exception as e:
            result += f"❌ Error al obtener WODs de N8: {str(e)}\n"
            wods_n8 = None

        result += "\n🌐 Obteniendo WODs de CrossfitDB...\n"
        try:
            import crossfitdb
            wods_crossfitdb = crossfitdb.main(semana=True, log_func=lambda msg: log_message(msg, tag="WodCFDB"))
            if wods_crossfitdb:
                # Asegurar que los días y meses estén correctamente formateados
                for wod in wods_crossfitdb:
                    wod['dia_semana'] = formatear_nombre_propio(wod['dia_semana'])
                result += f"✅ Se encontraron {len(wods_crossfitdb)} WODs de CrossFitDB\n"
            else:
                result += "⚠️ No se encontraron WODs de CrossFitDB\n"
        except Exception as e:
            result += f"❌ Error al obtener WODs de CrossFitDB: {str(e)}\n"
            wods_crossfitdb = None

        # Verificar si hay WODs disponibles
        tiene_wods = (wods_n8 is not None and len(wods_n8) > 0) or (wods_crossfitdb is not None and len(wods_crossfitdb) > 0)
        
        if tiene_wods:
            # NUEVO: Devolver JSON en lugar de enviar correo
            import json
            
            # Preparar datos para Kotlin/Android
            wods_json = {
                'wods_n8': wods_n8 if wods_n8 else [],
                'wods_crossfitdb': wods_crossfitdb if wods_crossfitdb else [],
                'fecha_inicio': lunes_fmt,
                'fecha_fin': viernes_fmt,
                'total_wods': (len(wods_n8) if wods_n8 else 0) + (len(wods_crossfitdb) if wods_crossfitdb else 0)
            }
            
            result += f"\n✅ WODs preparados para la app: {wods_json['total_wods']} WODs encontrados\n"
            result += f"\n📊 JSON_DATA_START\n{json.dumps(wods_json, default=str)}\nJSON_DATA_END\n"
        else:
            result += "\n⚠️ No hay WODs disponibles\n"

        # Analizar los resultados para dar un resumen más informativo
        tiene_error = "❌" in result
        
        if tiene_error and tiene_wods:
            result += "\n⚠️ Proceso completado con algunos errores"
        elif tiene_error:
            result += "\n❌ Proceso completado con errores"
        else:
            result += "\n✅ Proceso completado correctamente"
            
        return result

    except Exception as e:
        result += f"\n❌ Error general en el scraper: {str(e)}"
        return result

if __name__ == "__main__":
    print(main())