package com.example.wodifyplus.data.parser

import com.example.wodifyplus.data.models.Wod
import org.json.JSONObject
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WodParser @Inject constructor() {

    fun parseWods(result: String): List<Wod> {
        val wods = mutableListOf<Wod>()
        try {
            // Extraer el JSON del resultado
            val jsonStartMarker = "JSON_DATA_START"
            val jsonEndMarker = "JSON_DATA_END"
            
            val jsonStart = result.indexOf(jsonStartMarker)
            val jsonEnd = result.indexOf(jsonEndMarker)
            
            if (jsonStart == -1 || jsonEnd == -1) {
                android.util.Log.e("WodParser", "No se encontraron marcadores JSON")
                return emptyList()
            }
            
            val jsonString = result.substring(jsonStart + jsonStartMarker.length, jsonEnd).trim()
            
            try {
                val jsonObject = JSONObject(jsonString)
                
                // Parsear WODs de N8
                if (jsonObject.has("wods_n8")) {
                    val wodsN8Array = jsonObject.getJSONArray("wods_n8")
                    for (i in 0 until wodsN8Array.length()) {
                        val wodObj = wodsN8Array.getJSONObject(i)
                        parseWodFromJson(wodObj, "N8")?.let { wods.add(it) }
                    }
                }
                
                // Parsear WODs de CrossFit DB
                if (jsonObject.has("wods_crossfitdb")) {
                    val wodsCrossfit = jsonObject.getJSONArray("wods_crossfitdb")
                    for (i in 0 until wodsCrossfit.length()) {
                        val wodObj = wodsCrossfit.getJSONObject(i)
                        parseWodFromJson(wodObj, "CrossFit DB")?.let { wods.add(it) }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WodParser", "Error parseando JSON: ${e.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("WodParser", "Error en parseWods", e)
        }
        return wods
    }

    private fun parseWodFromJson(jsonObject: JSONObject, gimnasioName: String): Wod? {
        try {
            val fechaStr = when {
                jsonObject.has("fecha_iso") -> jsonObject.getString("fecha_iso")
                jsonObject.has("fecha") -> jsonObject.getString("fecha")
                else -> return null
            }
            
            val fecha = try {
                val fechaLimpia = fechaStr.substringBefore(" ").substringBefore("T")
                LocalDate.parse(fechaLimpia)
            } catch (e: Exception) {
                LocalDate.now()
            }
            
            val diaSemana = jsonObject.optString("dia_semana", "")
            val contenido = jsonObject.optString("contenido", "")
            val contenidoHtml = jsonObject.optString("contenido_html", "")
            
            return Wod(
                fecha = fecha,
                diaSemana = diaSemana,
                gimnasio = gimnasioName,
                contenido = contenido,
                contenidoHtml = contenidoHtml
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
