# Carpeta Python - WodifyPlus

## ⚠️ IMPORTANTE

Esta carpeta debe contener los archivos Python del proyecto original **Wodify**.

## 📁 Estructura Requerida:

Copia desde tu proyecto Wodify la carpeta `python/` completa:

```
python/
├── .env              # ⚠️ CON TUS CREDENCIALES (no en git)
├── config.py         # Configuración
├── crossfitdb.py     # Scraper CrossFit DB
├── n8.py             # Scraper N8
└── wod_scraper.py    # Script principal
```

## 🔐 Archivo .env

Crea el archivo `.env` con tus credenciales:

```
CROSSFITDB_USER=tu_usuario
CROSSFITDB_PASS=tu_contraseña
N8_USER=tu_usuario
N8_PASS=tu_contraseña
```

## ✅ Verificación

Después de copiar los archivos:

1. Verifica que todos los archivos estén presentes
2. Verifica que `.env` tenga tus credenciales
3. El `.gitignore` ya está configurado para NO subir `.env`

---

**Nota**: Esta carpeta es la que Android lee como `assets.srcDirs("src/main/python")` en el build.gradle
