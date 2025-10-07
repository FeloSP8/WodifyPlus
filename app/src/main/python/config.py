import os
from dotenv import load_dotenv
import sys

def log_message(message):
    """Loggea a consola o a Logcat si está en Android."""
    try:
        # Intentar importar Log de Android
        from android.util import Log
        Log.d("WodifyConfig", str(message))
    except ImportError:
        # Si no está en Android, imprimir a consola
        print(f"[ConfigLog] {message}")

log_message("Cargando variables de entorno desde .env...")
env_path = None
try:
    # Intentar determinar la ruta RELATIVA al script actual en Android
    from com.chaquo.python import Python # Solo para detectar Android
    # __file__ nos da la ruta del script config.py DENTRO del entorno Chaquopy
    script_dir = os.path.dirname(__file__)
    env_path = os.path.join(script_dir, '.env')
    log_message(f"Ruta calculada para .env en Android: {env_path}")
    if not os.path.exists(env_path):
        log_message("¡Alerta! .env no encontrado en la ruta calculada.")
except ImportError:
    # Fuera de Android, buscar en el directorio actual (comportamiento por defecto)
    log_message("Ejecutando fuera de Android, load_dotenv buscará localmente.")
    env_path = None # Dejar que load_dotenv lo busque solo

# Cargar .env usando la ruta calculada si existe, o dejar que lo busque por defecto
loaded = load_dotenv(dotenv_path=env_path) if env_path else load_dotenv()

# --- Añadir log para ver si la carga fue exitosa ---
if loaded:
    log_message(".env cargado exitosamente.")
else:
    log_message("Fallo al cargar .env (¿existe en la ruta correcta?).")
# --- Fin del log de éxito/fallo ---

# Configuración del correo electrónico (sin logs detallados)
EMAIL_CONFIG = {
    "remitente": os.getenv("EMAIL_REMITENTE"),
    "contraseña": os.getenv("EMAIL_CONTRASENA"),
    "destinatario": os.getenv("EMAIL_DESTINATARIO"),
    "servidor_smtp": os.getenv("EMAIL_SERVIDOR_SMTP", "smtp.gmail.com"),
    "puerto_smtp": int(os.getenv("EMAIL_PUERTO_SMTP", 587)),
    "asunto": os.getenv("EMAIL_ASUNTO", "WODs de la semana")
}

# Configuración para CrossFitDB (sin logs detallados)
CROSSFITDB_CONFIG = {
    "username": os.getenv("CFDB_USERNAME"),
    "password": os.getenv("CFDB_PASSWORD"),
    "id_user": os.getenv("CFDB_ID_USER"),
    "id_application": os.getenv("CFDB_ID_APPLICATION")
}