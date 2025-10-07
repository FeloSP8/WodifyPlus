import requests
from datetime import datetime, timedelta
import json
import re
from bs4 import BeautifulSoup
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import os
import sys
import time
from dotenv import load_dotenv

# Cargar variables de entorno
load_dotenv()

# Lista de palabras que siempre deben aparecer en mayúsculas
# Añade aquí las palabras que quieras en MAYÚSCULAS (sin importar su longitud)
PALABRAS_MAYUSCULAS = [
    "wod",
    "amrap",
    "emom",
    "rx",
    "tabata",
    "du",
    "ygig",
    "c2b",
    "t2b",
    "sc",
    "kbsr",
]

# Lista de palabras que identifican un tipo de entrenamiento 
# y deben tratarse como subsecciones especiales
TIPOS_ENTRENAMIENTO = [
    "amrap",
    "emom",
    "tabata",
    "for time",
    "etabata"
]

# Lista expandida de tipos de entrenamiento que deben detectarse como categorías principales
CATEGORIAS_PRINCIPALES = [
    "EMOM", "AMRAP", "TABATA", "FOR TIME", "ETABATA", "STRENGTH", "METCON", "SKILL"
]

# Importar configuración de email desde archivo externo
try:
    from config import EMAIL_CONFIG, CROSSFITDB_CONFIG
    print("Configuración de email cargada correctamente")
except ImportError as e:
    print(f"ERROR: No se encuentra el archivo config.py. Error: {e}")
    print("Por favor, copia config.example.py a config.py y configura tus datos de email")
    print("Consulta el README para más información")
    sys.exit(1)

# Función para limpiar texto HTML preservando estructura
def limpiar_html(texto):
    """Limpia el HTML del texto"""
    try:
        # Reemplazar <br> y <br/> por saltos de línea
        texto = re.sub(r'<br\s*/?>', '\n', texto, flags=re.IGNORECASE)
        
        # Crear objeto BeautifulSoup
        soup = BeautifulSoup(texto, "html.parser")
        
        # Obtener texto sin HTML
        texto_limpio = soup.get_text()
        
        # Limpiar espacios y líneas en blanco extras
        texto_limpio = re.sub(r'\n{3,}', '\n\n', texto_limpio)
        texto_limpio = re.sub(r' {2,}', ' ', texto_limpio)
        texto_limpio = texto_limpio.strip()
        
        # Limpiar comillas de "Team of X"
        texto_limpio = re.sub(r'["""]Team of (\d+)["""]', r'Team of \1', texto_limpio)
        
        return texto_limpio
    except Exception as e:
        print(f"Error al limpiar HTML: {str(e)}")
        return texto

# Función para aplicar formato al texto
def aplicar_formato(texto, dia_semana, fecha):
    """Aplica formato al texto del WOD"""
    # Limpiar espacios y líneas en blanco extras
    texto = re.sub(r'\n{3,}', '\n\n', texto)
    texto = re.sub(r' {2,}', ' ', texto)
    texto = texto.strip()
    
    # Convertir primera letra de cada línea a mayúscula
    lineas = texto.split('\n')
    texto_formateado = []
    
    # Variables para control de formato
    primera_linea = True
    
    for i, linea in enumerate(lineas):
        linea = linea.strip()
        if not linea:
            texto_formateado.append('')
            continue
            
        # Saltar la primera línea que contiene "WOD" + fecha
        if primera_linea and linea.lower().startswith('wod '):
            primera_linea = False
            continue
            
        # Si es un título (comienza con letra/número + paréntesis/punto)
        if re.match(r'^[A-Za-z0-9][\)\.]\s*', linea):
            texto_formateado.append(f"\n{linea.upper()}")
            continue
            
        # Si es "Team of X", ponerlo como título con formato especial
        if linea.lower().startswith('team of'):
            # Quitar las comillas si las tiene
            linea = linea.replace('"', '').replace('"', '')
            partes = linea.split('of')
            if len(partes) > 1:
                texto_formateado.append(f"\nTEAM OF {partes[1].strip().upper()}\n")
            else:
                texto_formateado.append(f"\n{linea.upper()}\n")
            continue
            
        # Si contiene metros y run/syn, es un subtítulo
        if re.search(r'\d+\s*m\s+(?:run|syn)', linea.lower()):
            texto_formateado.append(f"⚡ {linea.upper()}")
            continue
            
        # Si comienza con número y RDS/YGIG, es un subtítulo
        if re.match(r'^\d+\s+(?:rds|ygig)', linea.lower()):
            texto_formateado.append(f"🔄 {linea.upper()}")
            continue
            
        # Si es un tipo de entrenamiento (EMOM, AMRAP, etc)
        if any(linea.upper().startswith(tipo) for tipo in TIPOS_ENTRENAMIENTO):
            texto_formateado.append(f"⏱️ {linea.upper()}")
            continue
            
        # Si es un ejercicio (cualquier otra línea), añadirlo con sangría
        texto_formateado.append(f"        {linea}")
    
    # Unir las líneas y aplicar formato final
    texto = '\n'.join(texto_formateado)
    
    # Asegurar que ciertas palabras estén en mayúsculas
    for palabra in PALABRAS_MAYUSCULAS:
        texto = re.sub(rf'\b{palabra}\b', palabra.upper(), texto, flags=re.IGNORECASE)
    
    # Asegurar que las unidades estén en minúsculas
    texto = re.sub(r'\b(KG|CAL|M|MIN|SEC)\b', lambda m: m.group(1).lower(), texto, flags=re.IGNORECASE)
    
    # Asegurar que los símbolos de tiempo estén correctos
    texto = texto.replace("'", "'")
    texto = texto.replace('"', '"')
    
    # Limpiar líneas en blanco excesivas
    texto = re.sub(r'\n{3,}', '\n\n', texto)
    
    return texto

# Función para generar un correo con formato HTML elegante
def formatear_wod_para_correo(contenido):
    """
    Formatea el contenido de un WOD para presentarlo de forma elegante en HTML
    Detecta secciones, listas y tablas, y les da formato apropiado
    """
    # Dividir el contenido en líneas para procesarlo línea por línea
    lineas = contenido.split('\n')
    html_resultado = []
    
    # Variables para controlar el estado del procesamiento
    en_seccion = False
    en_lista = False
    seccion_actual = None
    tipo_entrenamiento_actual = None
    
    for i, linea in enumerate(lineas):
        linea = linea.strip()
        
        # Si la línea está vacía
        if not linea:
            if en_lista:
                html_resultado.append("</ul>")
                en_lista = False
            # No añadimos <br> extras para controlar mejor el espaciado
            continue
        
        # Verificar si la línea es una categoría principal (EMOM, AMRAP, STRENGTH, etc.)
        es_categoria_principal = False
        linea_upper = linea.upper()
        
        for categoria in CATEGORIAS_PRINCIPALES:
            # Si la línea comienza o contiene exactamente la categoría
            if linea_upper.startswith(categoria) or linea_upper == categoria:
                es_categoria_principal = True
                html_resultado.append(f'<div style="color: #000000; font-weight: 700; background-color: #f5f5f5; padding: 8px 12px; margin: 10px 0; border-left: 2px solid #2980b9;">{linea_upper}</div>')
                break
                
        if es_categoria_principal:
            continue
        
        # Detectar secciones (A), B), C), etc.)
        match_seccion = re.match(r'^([A-Za-z])[)\.]\s*(.*)$', linea)
        if match_seccion:
            # Si estábamos en una lista, cerrarla
            if en_lista:
                html_resultado.append("</ul>")
                en_lista = False
                
            letra, resto = match_seccion.groups()
            seccion_actual = letra.upper()
            en_seccion = True
            tipo_entrenamiento_actual = None  # Reiniciamos el tipo de entrenamiento al cambiar de sección
            
            html_resultado.append(f'<div class="section-header">{seccion_actual}) {resto}</div>')
            continue
        
        # Verificar si la línea es un tipo de entrenamiento (dentro de una sección)
        es_tipo_entrenamiento = False
        for tipo in TIPOS_ENTRENAMIENTO:
            if tipo.upper() in linea.upper() and (linea.upper().startswith(tipo.upper()) or len(linea.split()) <= 5):
                es_tipo_entrenamiento = True
                tipo_entrenamiento_actual = tipo.upper()
                html_resultado.append(f'<div class="workout-type">{linea}</div>')
                break
        
        if es_tipo_entrenamiento:
            continue
        
        # Detectar listas (comienzan con •, -, números, etc)
        es_lista = linea.startswith("  • ") or linea.startswith("• ") or re.match(r'^\d+[\.\)]', linea) or linea.startswith("-")
        
        if es_lista:
            # Si no estábamos en una lista, iniciarla
            if not en_lista:
                html_resultado.append('<ul class="wod-list">')
                en_lista = True
            
            # Limpiar el marcador de lista y añadir el item con formato
            item_texto = re.sub(r'^(\s*)(•|-|\d+[\.\)])\s*', '', linea)
            html_resultado.append(f'<li>{item_texto}</li>')
            continue
        
        # Si estamos dentro de una sección y la línea está indentada o sigue a un tipo de entrenamiento
        # tratarla como subsección
        if (en_seccion and linea.startswith("    ")) or tipo_entrenamiento_actual:
            texto_subseccion = linea.strip()
            
            # Si estamos después de un tipo de entrenamiento, aplicar clase especial
            if tipo_entrenamiento_actual:
                html_resultado.append(f'<div class="workout-details">{texto_subseccion}</div>')
            else:
                html_resultado.append(f'<div class="subsection">{texto_subseccion}</div>')
            continue
        
        # Si no es ninguno de los casos especiales, es un párrafo normal
        html_resultado.append(f'<div style="margin-left: 20px; padding: 5px 0; color: #34495e;">{linea}</div>')
    
    # Cerrar cualquier lista abierta
    if en_lista:
        html_resultado.append("</ul>")
    
    return "\n".join(html_resultado)

# Función para enviar un correo con los WODs
def enviar_correo_con_wods(todos_wods, lunes_fmt, viernes_fmt):
    # Si no hay WODs, retornar sin enviar correo
    if not todos_wods:
        print("No hay WODs para enviar por correo.")
        return False
        
    try:
        # Crear el mensaje
        mensaje = MIMEMultipart()
        mensaje["From"] = EMAIL_CONFIG["remitente"]
        mensaje["To"] = EMAIL_CONFIG["destinatario"]
        mensaje["Subject"] = "N8 - " + f"{EMAIL_CONFIG['asunto']} ({lunes_fmt} - {viernes_fmt})"
        
        # Crear el cuerpo del mensaje con HTML mejorado
        cuerpo = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>WODs de la semana</title>
            <style>
                @import url('https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;700&display=swap');
                
                body {{
                    font-family: 'Roboto', Helvetica, Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 800px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f5f7fa;
                }}
                
                h1 {{
                    color: #2c3e50;
                    font-size: 28px;
                    text-align: center;
                    font-weight: 700;
                    margin-bottom: 30px;
                }}
                
                h2 {{
                    margin: 0;
                    padding: 15px 20px;
                    color: white;
                    font-size: 18px;
                    font-weight: 600;
                    background-color: #2980b9;
                    border-radius: 8px 8px 0 0;
                    letter-spacing: 0.5px;
                }}
                
                .wod-card {{
                    background-color: white;
                    border-radius: 8px;
                    box-shadow: 0 4px 6px rgba(0,0,0,0.08);
                    margin-bottom: 30px;
                    overflow: hidden;
                }}
                
                .wod-content {{
                    padding: 25px;
                }}
                
                .section-header {{
                    font-weight: 700;
                    font-size: 16px;
                    color: #2c3e50;
                    background-color: #ecf0f1;
                    padding: 8px 12px;
                    margin: 15px 0 10px 0;
                    border-radius: 4px;
                    border-left: 4px solid #3498db;
                }}
                
                .workout-type {{
                    font-weight: 700;
                    font-size: 15px;
                    color: white;
                    background-color: #e74c3c;
                    padding: 6px 10px;
                    margin: 12px 0 8px 15px;
                    border-radius: 3px;
                    display: inline-block;
                }}
                
                .workout-details {{
                    margin: 5px 0 5px 25px;
                    color: #34495e;
                    font-weight: 500;
                    font-size: 15px;
                }}
                
                .subsection {{
                    margin: 8px 0 8px 20px;
                    color: #34495e;
                    font-weight: 500;
                }}
                
                .wod-list {{
                    list-style-type: none;
                    padding-left: 10px;
                    margin: 10px 0 15px 15px;
                }}
                
                .wod-list li {{
                    position: relative;
                    padding-left: 20px;
                    margin-bottom: 8px;
                    color: #34495e;
                }}
                
                .wod-list li:before {{
                    content: "•";
                    position: absolute;
                    left: 0;
                    color: #3498db;
                    font-weight: bold;
                }}
                
                .wod-paragraph {{
                    margin: 10px 0;
                    color: #34495e;
                }}
                
                .footer {{
                    text-align: center;
                    margin-top: 40px;
                    font-size: 13px;
                    color: #7f8c8d;
                }}
                
                .logo {{
                    text-align: center;
                    margin-bottom: 20px;
                }}
                
                .logo span {{
                    font-size: 18px;
                    font-weight: 700;
                    color: #2980b9;
                    letter-spacing: 2px;
                }}
            </style>
        </head>
        <body>
            <div class="logo">
                <span>WOD SCRAPER</span>
            </div>
            <h1>WODs de la semana ({lunes_fmt} - {viernes_fmt})</h1>
        """
        
        if todos_wods:
            for wod in todos_wods:
                titulo = wod["fecha_formateada"]
                if wod["dia_semana"]:
                    titulo = f"{wod['dia_semana']} {titulo}"
                
                # Añadir el título del WOD
                cuerpo += f'<div class="wod-card">\n<h2>WOD DEL {titulo}</h2>\n<div class="wod-content">'
                
                # Formatear el contenido para HTML con nuestro nuevo formateador
                contenido = wod["contenido"]
                contenido_html = formatear_wod_para_correo(contenido)
                
                # Añadir el contenido dentro de una tarjeta
                cuerpo += f"{contenido_html}</div>\n</div>"
        else:
            cuerpo += '<div class="wod-card">\n<h2>Sin WODs disponibles</h2>\n<div class="wod-content"><p>No se encontraron WODs para esta semana.</p></div>\n</div>'
        
        # Añadir pie de página
        cuerpo += """
            <div class="footer">
                <p>Generado automáticamente — WOD Scraper 2.1</p>
            </div>
        </body>
        </html>
        """
        
        # Añadir el cuerpo HTML al mensaje
        mensaje.attach(MIMEText(cuerpo, "html"))
        
        # Conectar al servidor SMTP
        servidor = smtplib.SMTP(EMAIL_CONFIG["servidor_smtp"], EMAIL_CONFIG["puerto_smtp"])
        servidor.starttls()  # Iniciar TLS
        servidor.login(EMAIL_CONFIG["remitente"], EMAIL_CONFIG["contraseña"])
        
        # Enviar el correo
        texto = mensaje.as_string()
        servidor.sendmail(EMAIL_CONFIG["remitente"], EMAIL_CONFIG["destinatario"], texto)
        
        # Cerrar la conexión
        servidor.quit()
        
        print(f"Correo enviado correctamente a {EMAIL_CONFIG['destinatario']}")
        return True
    except Exception as e:
        print(f"Error al enviar el correo: {e}")
        return False

# Función para obtener fecha en formato legible
def formatear_fecha(fecha_str):
    try:
        # Convertir YYYY-MM-DD a DD/MM/YYYY
        año, mes, dia = fecha_str.split('-')
        return f"{dia}/{mes}/{año}"
    except:
        return fecha_str

# Función para extraer fecha del contenido del WOD
def extraer_fecha_del_contenido(contenido):
    # Primero intentamos el patrón de WOD con fecha
    patron_wod = r'wod\s+(?:(\w+)\s+)?(\d+)\s+de\s+(\w+)(?:\s+de\s+(\d{4}))?'
    match = re.search(patron_wod, contenido, re.IGNORECASE)
    if match:
        dia_semana, dia, mes, año = match.groups()
        # Si no hay año especificado, usar el actual
        if not año:
            año = str(datetime.now().year)
        # Mapear nombres de meses a números (incluyendo abreviaturas de 3 letras)
        meses = {
            'enero': '01', 'ene': '01', 'ener': '01',
            'febrero': '02', 'feb': '02', 'febr': '02',
            'marzo': '03', 'mar': '03', 'marz': '03',
            'abril': '04', 'abr': '04', 'abri': '04',
            'mayo': '05', 'may': '05',
            'junio': '06', 'jun': '06', 'juni': '06',
            'julio': '07', 'jul': '07', 'juli': '07',
            'agosto': '08', 'ago': '08', 'agos': '08', 'agost': '08',
            'septiembre': '09', 'sep': '09', 'sept': '09', 'septi': '09',
            'octubre': '10', 'oct': '10', 'octu': '10', 'octub': '10',
            'noviembre': '11', 'nov': '11', 'novi': '11', 'novie': '11',
            'diciembre': '12', 'dic': '12', 'dici': '12', 'dicie': '12'
        }
        mes_num = meses.get(mes.lower(), '01')  # default a enero si no se encuentra
        dia_zfill = dia.zfill(2)
        
        # Capitalizar el nombre del mes para el formato de fecha legible
        mes_capitalizado = mes.capitalize()
        return f"{dia}/{mes_capitalizado}/{año}", f"{año}-{mes_num}-{dia_zfill}", dia_semana

    # Si no encontramos el patrón WOD, buscamos SABAPARTNER o FUNDAY
    # y usamos la fecha del sistema para ese día
    hoy = datetime.now()
    
    # Buscar SABAPARTNER
    if re.search(r'^sabapartner\b', contenido, re.IGNORECASE):
        # Encontrar el próximo sábado
        dias_hasta_sabado = (5 - hoy.weekday()) % 7
        fecha = hoy + timedelta(days=dias_hasta_sabado)
        return (fecha.strftime("%d/%B/%Y"), 
                fecha.strftime("%Y-%m-%d"), 
                "Sábado")
    
    # Buscar FUNDAY
    if re.search(r'^funday\b', contenido, re.IGNORECASE):
        # Encontrar el próximo domingo
        dias_hasta_domingo = (6 - hoy.weekday()) % 7
        fecha = hoy + timedelta(days=dias_hasta_domingo)
        return (fecha.strftime("%d/%B/%Y"), 
                fecha.strftime("%Y-%m-%d"), 
                "Domingo")

    return "", "", ""

# Función para obtener el rango de la semana actual
def obtener_rango_semana_actual(include_weekends=False):
    hoy = datetime.now()
    
    # Si es domingo, obtener el rango de la próxima semana
    if hoy.weekday() == 6:  # 6 = domingo
        inicio = hoy + timedelta(days=1)  # El siguiente lunes
    else:
        # Para otros días, comenzar desde el día actual (a las 00:00)
        inicio = hoy.replace(hour=0, minute=0, second=0, microsecond=0)
    
    # Calcular el próximo sábado o domingo según corresponda
    if include_weekends:
        dias_hasta_domingo = 6 - inicio.weekday()
        if dias_hasta_domingo < 0:  # Si ya pasó el domingo de esta semana
            dias_hasta_domingo += 7  # Ir al próximo domingo
        fin = inicio + timedelta(days=dias_hasta_domingo)
    else:
        dias_hasta_sabado = 5 - inicio.weekday()
        if dias_hasta_sabado < 0:  # Si ya pasó el sábado de esta semana
            dias_hasta_sabado += 7  # Ir al próximo sábado
        fin = inicio + timedelta(days=dias_hasta_sabado)
    
    return inicio, fin

# Función para determinar si una fecha (día/mes) está en la semana actual
def es_fecha_de_esta_semana(dia, mes, include_weekends=False):
    try:
        dia = int(dia)
        mes_num = {
            'enero': 1, 'ene': 1, 
            'febrero': 2, 'feb': 2, 
            'marzo': 3, 'mar': 3, 
            'abril': 4, 'abr': 4,
            'mayo': 5, 'may': 5, 
            'junio': 6, 'jun': 6, 
            'julio': 7, 'jul': 7, 
            'agosto': 8, 'ago': 8,
            'septiembre': 9, 'sep': 9, 
            'octubre': 10, 'oct': 10, 
            'noviembre': 11, 'nov': 11, 
            'diciembre': 12, 'dic': 12
        }.get(mes.lower(), 0)
        
        if mes_num == 0:
            return False
        
        # Obtener el lunes y el último día de la semana (viernes o domingo)
        lunes, ultimo_dia = obtener_rango_semana_actual(include_weekends)
        año_actual = datetime.now().year
        
        # Crear la fecha del WOD
        fecha_wod = datetime(año_actual, mes_num, dia)
        
        # Verificar si está en el rango de la semana
        return lunes.date() <= fecha_wod.date() <= ultimo_dia.date()
    except (ValueError, AttributeError):
        return False

# Función para asignar un valor numérico a cada día para ordenamiento
def valor_ordenamiento(dia_semana):
    orden_dias = {
        'Lunes': 1, 
        'Martes': 2, 
        'Miércoles': 3, 'Miercoles': 3,
        'Jueves': 4, 
        'Viernes': 5,
        'Sábado': 6, 'Sabado': 6,
        'Domingo': 7
    }
    return orden_dias.get(dia_semana, 9)  # 9 para días no identificados

# URL de la API con parámetros
API_URL = "https://boxn8.aimharder.com/api/activity?timeLineFormat=0&timeLineContent=7&userID=217851&_=1742756755105"

# Headers para la petición
headers = {
    'Accept': '*/*',
    'Accept-Encoding': 'gzip, deflate, br',
    'Accept-Language': 'es-ES,es;q=0.9',
    'Connection': 'keep-alive',
    'Cookie': 'PHPSESSID=0hom37gkerrjnqup472i22rj24',
    'Host': 'boxn8.aimharder.com',
    'Referer': 'https://boxn8.aimharder.com/',
    'sec-ch-ua': '"Not_A Brand";v="8", "Chromium";v="120", "Google Chrome";v="120"',
    'sec-ch-ua-mobile': '?0',
    'sec-ch-ua-platform': '"Windows"',
    'Sec-Fetch-Dest': 'empty',
    'Sec-Fetch-Mode': 'cors',
    'Sec-Fetch-Site': 'same-origin',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'X-Requested-With': 'XMLHttpRequest'
}

def es_fecha_posterior_o_igual_a_hoy(fecha_iso):
    try:
        fecha_wod = datetime.strptime(fecha_iso, "%Y-%m-%d").date()
        return fecha_wod >= datetime.now().date()
    except (ValueError, AttributeError):
        return False

# Función para parsear la fecha de la API
def parsear_fecha_api(fecha_str, when_str=None):
    """Convierte fechas como '28 Mar' a objetos datetime"""
    try:
        # Mapeo de meses abreviados en inglés
        meses = {
            'Jan': 1, 'Feb': 2, 'Mar': 3, 'Apr': 4,
            'May': 5, 'Jun': 6, 'Jul': 7, 'Aug': 8,
            'Sep': 9, 'Oct': 10, 'Nov': 11, 'Dec': 12,
            # Añadir abreviaturas en español (por si acaso)
            'Ene': 1, 'Feb': 2, 'Mar': 3, 'Abr': 4,
            'May': 5, 'Jun': 6, 'Jul': 7, 'Ago': 8,
            'Sep': 9, 'Oct': 10, 'Nov': 11, 'Dic': 12
        }
        
        # Extraer mes y día
        partes = fecha_str.split()
        if len(partes) != 2:
            return None
            
        # El formato es "28 Mar", no "Mar 28th"
        dia_str = partes[0]
        mes_str = partes[1]
        
        if not dia_str.isdigit():
            return None
            
        # Convertir a números
        mes = meses.get(mes_str)
        if not mes:
            # Probar con versiones en minúscula o capitalizada
            mes = meses.get(mes_str.capitalize())
            
        if not mes:
            return None
            
        dia = int(dia_str)
        
        # Determinar el año correcto
        hoy = datetime.now()
        año = hoy.year
        
        # Ajustar el año si es necesario
        if when_str and len(when_str) >= 8:
            try:
                # Intentar extraer el año directamente del campo "when"
                año_from_when = int(when_str[:4])
                if 2000 <= año_from_when <= 2100:  # Validar que sea un año razonable
                    año = año_from_when
            except ValueError:
                pass
        
        # Crear fecha tentativa
        fecha_tentativa = datetime(año, mes, dia)
        
        # ===== LÓGICA MEJORADA PARA CAMBIOS DE MES =====
        # Obtener mes actual y calcular diferencia de meses
        mes_actual = hoy.month
        
        # Caso 1: Mes en API es el siguiente al mes actual 
        # (ej. Ahora: mar-31, API: abr-1)
        if mes == mes_actual + 1 or (mes_actual == 12 and mes == 1):
            # Si estamos en los últimos 7 días del mes actual
            if hoy.day >= 25 or (mes_actual in [4, 6, 9, 11] and hoy.day >= 23) or (mes_actual == 2 and hoy.day >= 22):
                # La fecha está bien (año actual)
                pass
            # Si no estamos a fin de mes, podría ser del año pasado
            else:
                if (hoy - fecha_tentativa).days > 300:  # Si la diferencia es casi un año
                    fecha_tentativa = datetime(año + 1, mes, dia)
        
        # Caso 2: Mes en API es el anterior al mes actual 
        # (ej. Ahora: abr-1, API: mar-31)
        elif mes == mes_actual - 1 or (mes_actual == 1 and mes == 12):
            # Si estamos en los primeros 7 días del mes actual
            if hoy.day <= 7:
                # La fecha está bien (año actual)
                pass
            # Si no estamos a principios de mes, podría ser del año siguiente
            else:
                if (fecha_tentativa - hoy).days < -300:  # Si la diferencia es casi un año
                    fecha_tentativa = datetime(año + 1, mes, dia)
        
        # Caso 3: El mes de la API es mucho mayor que el mes actual
        # (ej. Ahora: ene, API: oct-dic) - probablemente año anterior
        elif mes > mes_actual + 2 and mes_actual < 10:
            fecha_tentativa = datetime(año - 1, mes, dia)
        
        # Caso 4: El mes de la API es mucho menor que el mes actual
        # (ej. Ahora: dic, API: ene-feb) - probablemente año siguiente
        elif mes < mes_actual - 2 and mes_actual > 2:
            fecha_tentativa = datetime(año + 1, mes, dia)
            
        # ===== CASOS ESPECÍFICOS PARA MARZO/ABRIL =====
        # Caso especial para la transición marzo-abril que parece problemática
        if (mes_actual == 3 and mes == 4) or (mes_actual == 4 and mes == 3):
            # Si es abril en la API pero estamos en marzo
            if mes == 4 and mes_actual == 3:
                # Siempre considerar que es el próximo mes (abril del año actual)
                fecha_tentativa = datetime(año, mes, dia)
            # Si es marzo en la API pero estamos en abril
            elif mes == 3 and mes_actual == 4:
                # Si estamos en los primeros días de abril, considerar que marzo es del mismo año
                if hoy.day <= 10:
                    fecha_tentativa = datetime(año, mes, dia)
                # Si no, podría ser marzo del próximo año
                else:
                    # Si es un día alto de marzo (> 25), es más probable que sea del año actual
                    if dia >= 25:
                        fecha_tentativa = datetime(año, mes, dia)
                    else:
                        fecha_tentativa = datetime(año + 1, mes, dia)
        
        return fecha_tentativa
    except (ValueError, AttributeError, KeyError) as e:
        print(f"Error al parsear fecha: {str(e)}")
        return None

def login_aimharder(mail, pw, log_func=print):
    """
    Realiza login en aimharder.com y devuelve una sesión autenticada con las cookies necesarias.
    """
    login_url = "https://aimharder.com/login"
    payload = {
        "loginfingerprint": "2j6b4pq9hvvugw220ahs776i34r08yft3zenjt404m2om7nrcb",  # Puede que necesite ser dinámico, pero probamos fijo
        "loginiframe": "0",
        "mail": mail,
        "pw": pw,
        "login": "Log in"
    }
    headers_login = {
        "User-Agent": "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
        "Content-Type": "application/x-www-form-urlencoded",
        "Origin": "https://aimharder.com",
        "Referer": "https://aimharder.com/login",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "Accept-Language": "es-ES,es;q=0.9,en;q=0.8",
    }
    session = requests.Session()
    resp = session.post(login_url, data=payload, headers=headers_login, allow_redirects=True)
    log_func(f"[LOGIN] Status code: {resp.status_code}")
    log_func(f"[LOGIN] Set-Cookie: {resp.headers.get('set-cookie')}")
    # Verificar si la cookie amhrdrauth está en la sesión
    if 'amhrdrauth' in session.cookies.get_dict():
        log_func("[LOGIN] Autenticación exitosa, cookie amhrdrauth presente.")
    else:
        log_func("[LOGIN] Advertencia: No se encontró cookie amhrdrauth. Puede que el login haya fallado.")
    return session

def main(debug_abril=False, log_func=print):
    """
    Función principal que obtiene los WODs de N8
    :param debug_abril: Si es True, fuerzamos a procesar fechas de abril para debug
    :param log_func: Función para loguear mensajes.
    :return: Lista de WODs formateados o None en caso de error
    """
    try:
        log_func("📡 Autenticando en AimHarder...")
        mail = os.getenv("AIMHARDER_MAIL")
        pw = os.getenv("AIMHARDER_PW")
        if not mail or not pw:
            log_func("❌ ERROR: Faltan AIMHARDER_MAIL y AIMHARDER_PW en .env")
            return None
        session = login_aimharder(mail, pw, log_func)

        log_func("📡 Conectando a N8...")
        timestamp = int(time.time() * 1000)
        url = f"https://boxn8.aimharder.com/api/activity?timeLineFormat=0&timeLineContent=7&userID=217851&_={timestamp}"
        
        # --- Log de cookies ANTES de la petición ---
        log_func(f"[DEBUG N8] Cookies en sesión ANTES de GET: {session.cookies.get_dict()}")
        
        # --- RESTAURAR HEADERS ESPECÍFICOS PARA N8 --- 
        headers_api_n8 = {
            'accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7',
            'accept-encoding': 'gzip, deflate, br, zstd',
            'accept-language': 'en,es;q=0.9,fr;q=0.8,es-ES;q=0.7',
            'user-agent': 'Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36',
            'upgrade-insecure-requests': '1',
            'sec-ch-ua': '"Google Chrome";v="135", "Not-A.Brand";v="8", "Chromium";v="135"',
            'sec-ch-ua-mobile': '?1',
            'sec-ch-ua-platform': '"Android"',
            'sec-fetch-dest': 'document',
            'sec-fetch-mode': 'navigate',
            'sec-fetch-site': 'none',
            'sec-fetch-user': '?1',
            # Añadir Referer si es necesario, aunque para GET directo no suele serlo
            # 'Referer': 'https://boxn8.aimharder.com/' 
        }
        # --- FIN RESTAURAR HEADERS --- 
        
        # --- Usar los headers específicos para N8 --- 
        response = session.get(url, headers=headers_api_n8, timeout=10)
        log_func(f"[DEBUG N8] Status Code: {response.status_code}")
        # --- Fin logs re-añadidos ---
          
        try:
            data = response.json()
            log_func("✅ Conexión N8 establecida")
            
            # --- CALCULAR RANGO SEMANAL ---
            hoy = datetime.now()
            
            if hoy.weekday() == 6:  # Si es domingo (6)
                # Buscar semana siguiente: lunes a sábado
                lunes_siguiente = hoy + timedelta(days=1)
                sabado_siguiente = lunes_siguiente + timedelta(days=5)
                inicio = lunes_siguiente.replace(hour=0, minute=0, second=0, microsecond=0)
                fin = sabado_siguiente.replace(hour=23, minute=59, second=59, microsecond=999999)
            else:
                # Buscar desde hoy hasta sábado de esta semana
                dias_hasta_sabado = 5 - hoy.weekday()  # Sábado = 5
                sabado_semana = hoy + timedelta(days=dias_hasta_sabado)
                inicio = hoy.replace(hour=0, minute=0, second=0, microsecond=0)
                fin = sabado_semana.replace(hour=23, minute=59, second=59, microsecond=999999)
            
            # --- FIN CÁLCULO RANGO ---
            
            # ... (modo debug abril)
            if debug_abril:
                log_func("⚠️ MODO DEBUG ABRIL ACTIVADO: Procesando todas las fechas de abril")
                inicio = datetime(hoy.year, 4, 1) 
                fin = datetime(hoy.year, 4, 30) 

            inicio_date = inicio.date()
            fin_date = fin.date()
            inicio_fmt = inicio.strftime("%d/%m/%Y")
            fin_fmt = fin.strftime("%d/%m/%Y")
            log_func(f"🗓️ Buscando WODs N8: {inicio_fmt} al {fin_fmt}")

            # Extraer todos los WODs
            todos_wods = []
            
            # Ahora procesamos los elementos para extraer los WODs
            log_func("\n===== PROCESANDO WODS =====")
            for elemento in data.get("elements", []):
                if "TIPOWODs" in elemento and elemento["TIPOWODs"]:
                    # Obtener la fecha del elemento
                    fecha_api_day = elemento.get("day") # Campo prioritario
                    when = elemento.get("when", "")
                    notes_break = elemento.get("notesBreak", "")
                    
                    fecha_dt = None
                    fecha_origen = "Desconocido"
                    fecha_encontrada = False # Variable para saber si logramos parsear

                    # --- PRIORIDAD 1: Intentar parsear el campo "day" --- 
                    if fecha_api_day:
                        log_func(f"Intentando parsear fecha desde campo 'day': {fecha_api_day}")
                        fecha_dt = parsear_fecha_api(fecha_api_day, when)
                        if fecha_dt:
                            fecha_origen = f"API field 'day' ({fecha_api_day})"
                            fecha_encontrada = True
                        else:
                            log_func(f"⚠️ No se pudo parsear fecha desde 'day': {fecha_api_day}")
                    else:
                        log_func("⚠️ Campo 'day' no encontrado en el elemento.")

                    # --- PRIORIDAD 2: Si falla "day", intentar regex en notesBreak --- 
                    if not fecha_encontrada and notes_break:
                        log_func("Intentando extraer fecha desde 'notesBreak' con regex...")
                        patrones_fecha = [
                            r'(?i)wod\s+(\d+)\s+de\s+(\w+)(?:\s+de\s+(\d{4}))?',
                            r'(?i)(\d+)\s+de\s+(\w+)(?:\s+de\s+(\d{4}))?',
                            r'(?i)(\w+)\s+(\d+)',
                            r'(?i)wod\s+(?:del?\s+)?(\w+)',
                            r'(?i)wod\s+(\w+)\s+(\d+)',
                            r'(?i)wod\s+del?\s+(\d+)/(\d+)(?:/(\d{4}))?',
                            r'(?i)wod\s+(\d+)\s+(?:de\s+)?(ene|feb|mar|abr|may|jun|jul|ago|sep|oct|nov|dic)',
                            r'(?i)(\d+)\s+(?:de\s+)?(ene|feb|mar|abr|may|jun|jul|ago|sep|oct|nov|dic)'
                        ]
                        
                        fecha_dt_regex = None # Variable temporal para el resultado del regex
                        for i, patron in enumerate(patrones_fecha):
                            match = re.search(patron, notes_break)
                            if match:
                                grupos = match.groups()
                                
                                # Extraer información según el patrón
                                if i == 2 or i == 4:  # Patrón para "Abril 3" o "Wod Abril 3"
                                    mes, dia = grupos[0], grupos[1] if len(grupos) > 1 else None
                                    año = str(datetime.now().year)
                                    if not dia and mes.isdigit() and 1 <= int(mes) <= 30:
                                        dia = mes
                                        mes = "abril"
                                elif i == 3:  # Patrón para "Wod abril" (sin día)
                                    # Buscar un número cercano después de "wod abril"
                                    mes = grupos[0]
                                    if mes.lower() in ["abril", "abr"]:
                                        # Buscar un número en el texto
                                        numeros = re.findall(r'\b(\d{1,2})\b', notes_break)
                                        dia = None
                                        for num in numeros:
                                            if 1 <= int(num) <= 30:
                                                dia = num
                                                break
                                        if not dia:
                                            dia = "15"  # Default al 15 de abril si no hay número
                                    año = str(datetime.now().year)
                                elif i == 5:  # Patrón para fechas numéricas "Wod del 3/4/2023"
                                    dia, mes_num, año = grupos
                                    try:
                                        mes_num = int(mes_num)
                                        if not año:
                                            año = str(datetime.now().year)
                                        
                                        # Mapear número de mes a nombre
                                        meses_num_a_nombre = {
                                            1: 'Enero', 2: 'Febrero', 3: 'Marzo', 4: 'Abril',
                                            5: 'Mayo', 6: 'Junio', 7: 'Julio', 8: 'Agosto',
                                            9: 'Septiembre', 10: 'Octubre', 11: 'Noviembre', 12: 'Diciembre'
                                        }
                                        mes = meses_num_a_nombre.get(mes_num, "")
                                    except (ValueError, TypeError):
                                        continue
                                else:  # Patrones para "Wod 3 de Abril" o "3 de Abril"
                                    if len(grupos) == 3:
                                        dia, mes, año = grupos
                                    else:
                                        continue
                                        
                                    if not año:
                                        año = str(datetime.now().year)
                                
                                # Mapear nombres de meses a números (incluyendo abreviaturas de 3 letras)
                                meses = {
                                    'enero': 1, 'ene': 1, 
                                    'febrero': 2, 'feb': 2, 
                                    'marzo': 3, 'mar': 3, 
                                    'abril': 4, 'abr': 4,
                                    'mayo': 5, 'may': 5, 
                                    'junio': 6, 'jun': 6, 
                                    'julio': 7, 'jul': 7, 
                                    'agosto': 8, 'ago': 8,
                                    'septiembre': 9, 'sep': 9, 
                                    'octubre': 10, 'oct': 10, 
                                    'noviembre': 11, 'nov': 11, 
                                    'diciembre': 12, 'dic': 12
                                }
                                
                                # Intentar detectar el mes por nombre
                                mes_num = None
                                if isinstance(mes, str):
                                    mes_lower = mes.lower()
                                    # Buscar coincidencia parcial para manejar variaciones
                                    for nombre_mes, numero in meses.items():
                                        if mes_lower in nombre_mes or nombre_mes in mes_lower:
                                            mes_num = numero
                                            break
                                elif isinstance(mes, int):
                                    mes_num = mes
                                
                                if mes_num and dia:
                                    try:
                                        dia_num = int(dia)
                                        if 1 <= dia_num <= 31:  # Validar día
                                            fecha_dt = datetime(int(año), mes_num, dia_num)
                                            fecha_encontrada = True
                                            break
                                    except ValueError as e:
                                        pass
                        
                        if fecha_encontrada:
                            fecha_dt = fecha_dt_regex # Asignar el resultado del regex a fecha_dt final
                            log_func(f"Fecha encontrada con Regex: {fecha_dt.strftime('%Y-%m-%d')}")
                            
                        # --- PRIORIDAD 3: Si regex también falla, buscar SABAPARTNER/FUNDAY --- 
                        if not fecha_encontrada: 
                            log_func("Intentando detectar SABAPARTNER/FUNDAY en 'notesBreak'...")
                            notes_lower = notes_break.lower()
                            hoy_dt = datetime.now()
                            
                            if "sabapartner" in notes_lower:
                                dias_hasta_sabado = (5 - hoy_dt.weekday() + 7) % 7
                                fecha_dt = (hoy_dt + timedelta(days=dias_hasta_sabado)).replace(hour=0, minute=0, second=0, microsecond=0)
                                fecha_origen = "notesBreak (SABAPARTNER)"
                                log_func(f"ℹ️ Fecha derivada de SABAPARTNER: {fecha_dt.strftime('%Y-%m-%d')}")
                                fecha_encontrada = True # Marcar como encontrada
                                
                            elif "funday" in notes_lower:
                                dias_hasta_domingo = (6 - hoy_dt.weekday() + 7) % 7
                                fecha_dt = (hoy_dt + timedelta(days=dias_hasta_domingo)).replace(hour=0, minute=0, second=0, microsecond=0)
                                fecha_origen = "notesBreak (FUNDAY)"
                                log_func(f"ℹ️ Fecha derivada de FUNDAY: {fecha_dt.strftime('%Y-%m-%d')}")
                                fecha_encontrada = True # Marcar como encontrada
                                
                        if not fecha_encontrada and not fecha_dt: # Asegurar que no logueamos si ya encontramos por regex
                            log_func("⚠️ No se encontró fecha con regex ni SABAPARTNER/FUNDAY en 'notesBreak'.")
                    
                    # Si todavía no hay fecha y estamos en modo debug_abril
                    if not fecha_dt and notes_break and debug_abril and ("abril" in notes_break.lower() or "abr" in notes_break.lower()):
                        fecha_dt = datetime(datetime.now().year, 4, 15)
                        
                    # Si no pudimos extraer de notesBreak, intentar con day/when
                    if not fecha_dt and fecha_api_day:
                        fecha_dt = parsear_fecha_api(fecha_api_day, when)
                        if fecha_dt:
                             fecha_origen = f"API day/when ({fecha_api_day})"
                    
                    if not fecha_dt:
                        log_func(f"❌ No se pudo determinar fecha para elemento ID: {elemento.get('id')}. Saltando.")
                        continue

                    # Verificar si la fecha está en el rango de la semana actual o siguiente
                    fecha_dt_date = fecha_dt.date()
                    
                    # --- FILTRO ESTRICTO: SOLO FECHAS DENTRO DEL RANGO (INICIO A FIN) --- 
                    if not (inicio_date <= fecha_dt_date <= fin_date):
                        # Si no está en el rango, la saltamos directamente
                        continue 
                    # --- FIN FILTRO ESTRICTO ---
                    
                    # --- NUEVO: Filtro por tipo de WOD según día de la semana (revisando TIPOWODs) ---
                    dia_semana_num = fecha_dt.weekday() # Lunes=0, Domingo=6
                    wod_valido_para_dia = False
                    tipo_esperado = "Desconocido"
                    contenido_wod_seleccionado = None # Guardar el contenido del WOD que cumple
                    clase_wod_original = elemento.get("wodClass", "") # Clase general del elemento
                    
                    # Determinar qué tipo de WOD buscar según el día
                    if 0 <= dia_semana_num <= 4: tipo_esperado = "WOD inicial"
                    elif dia_semana_num == 5: tipo_esperado = "SABAPARTNER"
                    elif dia_semana_num == 6: tipo_esperado = "FUNDAY"
                        
                    log_func(f"-- Evaluando Filtro Día/Tipo para ID {elemento.get('id')} ({fecha_dt_date.strftime('%A %d/%m')}) --")
                    log_func(f"   Tipo Esperado: {tipo_esperado}")

                    # Iterar por los TIPOWODs dentro del elemento
                    for i, tipo_wod in enumerate(elemento.get("TIPOWODs", [])):
                        notes_interno = tipo_wod.get("notes", "")
                        notes_interno_lower = notes_interno.lower()
                        
                        log_func(f"   -> Evaluando TIPOWODs[{i}] notes: '{notes_interno[:60]}...'")
                        
                        # Aplicar regla según el día
                        if 0 <= dia_semana_num <= 4: # Lunes a Viernes
                            if re.match(r'^wod($|\s)', notes_interno_lower):
                                wod_valido_para_dia = True
                        elif dia_semana_num == 5: # Sábado
                            if "sabapartner" in notes_interno_lower:
                                wod_valido_para_dia = True
                        elif dia_semana_num == 6: # Domingo
                            if "funday" in notes_interno_lower:
                                wod_valido_para_dia = True
                        
                        # Si encontramos uno válido, guardamos su contenido y salimos del bucle interno
                        if wod_valido_para_dia:
                            contenido_wod_seleccionado = notes_interno
                            log_func(f"      -> ¡Coincide! Se usará este contenido.")
                            break # Procesamos solo el primer TIPOWOD que coincida

                    log_func(f"   Resultado Filtro: {'PASA' if wod_valido_para_dia else 'FALLA'}")
                        
                    if not wod_valido_para_dia:
                        continue # Saltar este elemento si ningún TIPOWOD cumple el filtro del día
                    # --- FIN NUEVO FILTRO ---
                        
                    # Si pasa ambos filtros (rango y tipo), procesar EL CONTENIDO SELECCIONADO
                    log_func(f"✅ Procesando WOD ID {elemento.get('id')} para {fecha_dt.strftime('%A %d/%m')} (Origen: {fecha_origen}, Tipo: {tipo_esperado})")

                    # Asegurarse de que tenemos contenido seleccionado
                    if not contenido_wod_seleccionado:
                         log_func(f"   -> ERROR INTERNO: wod_valido_para_dia=True pero no hay contenido_wod_seleccionado.")
                         continue
                         
                    # Limpiar el HTML y formatear el contenido SELECCIONADO
                    wod_limpio = limpiar_html(contenido_wod_seleccionado) 
                    
                    if not wod_limpio.strip():
                        log_func(f"   -> ERROR: Contenido seleccionado está vacío después de limpiar.")
                        continue

                    # Asignar día de la semana y formato de fecha
                    dias_semana_es = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"]
                    weekday_num = fecha_dt.weekday()
                    if 0 <= weekday_num <= 6:
                        dia_semana_str = dias_semana_es[weekday_num]
                    else:
                        log_func(f"   -> ERROR: weekday() devolvió {weekday_num}, fuera del rango 0-6")
                        dia_semana_str = "Desconocido"
                    fecha_formateada_str = fecha_dt.strftime("%d/%m/%Y")
                    fecha_iso_str = fecha_dt.strftime("%Y-%m-%d")

                    # Formatear el WOD
                    wod_formateado = aplicar_formato(wod_limpio, dia_semana_str, fecha_formateada_str)
                    todos_wods.append({
                        "fecha": fecha_dt,
                        "fecha_iso": fecha_iso_str,
                        "fecha_formateada": fecha_formateada_str,
                        "dia_semana": dia_semana_str,
                        "contenido": wod_formateado,
                        "contenido_html": formatear_wod_para_correo(wod_formateado),
                        "valor_orden": valor_ordenamiento(dia_semana_str),
                        "gimnasio": "N8",
                        "titulo": f"WOD DEL {dia_semana_str} {fecha_formateada_str}",
                        "clase": clase_wod_original # Mantener la clase general original
                    })

                    log_func(f"   -> Añadido WOD: {clase_wod_original} - {contenido_wod_seleccionado[:30]}...")

            # Ordenar por día de la semana
            todos_wods.sort(key=lambda x: x["fecha"])

            # Mostrar resultados finales
            if todos_wods:
                log_func(f"✅ {len(todos_wods)} WODs de N8 encontrados")
                return todos_wods
            else:
                log_func("ℹ️ No se encontraron WODs de N8")
                return None

        except json.JSONDecodeError as e:
            log_func(f"❌ Error: La respuesta de N8 no es JSON válido: {str(e)}")
            return None

    except requests.exceptions.ConnectionError as e:
        log_func(f"❌ Error de conexión con N8: {str(e)}")
        if "NameResolutionError" in str(e):
            return None
        return None
    except Exception as e:
        log_func(f"❌ Error general en N8: {str(e)}")
        return None

if __name__ == "__main__":
    print(main())