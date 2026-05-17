package com.example.wodifyplus.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.wodifyplus.data.models.Wod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun exportWodsToCsv(wods: List<Wod>): File? = withContext(Dispatchers.IO) {
        try {
            val fileName = "wodify_plus_export_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            
            file.printWriter().use { out ->
                // Header
                out.println("Fecha,Día,Gimnasio,Contenido,Completada,Fecha Completado,Calorías,Distancia (km),Duración (min),Notas")
                
                // Rows
                wods.forEach { wod ->
                    val row = StringBuilder()
                    row.append(escapeCsv(wod.fecha.toString())).append(",")
                    row.append(escapeCsv(wod.diaSemana)).append(",")
                    row.append(escapeCsv(wod.gimnasio)).append(",")
                    row.append(escapeCsv(wod.contenido.replace("\n", " "))).append(",")
                    row.append(wod.completada).append(",")
                    row.append(escapeCsv(wod.fechaCompletado?.toString() ?: "")).append(",")
                    row.append(wod.caloriasQuemadas ?: "").append(",")
                    row.append(wod.distanciaKm ?: "").append(",")
                    row.append(wod.duracionMinutos ?: "").append(",")
                    row.append(escapeCsv(wod.notas ?: ""))
                    out.println(row.toString())
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent, "Exportar datos")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
