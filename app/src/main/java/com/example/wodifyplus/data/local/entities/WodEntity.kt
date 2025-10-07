package com.example.wodifyplus.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wods")
data class WodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fecha: String, // "2025-01-20"
    val diaSemana: String, // "Lunes"
    val gimnasio: String, // "CrossFit DB" o "N8"
    val contenido: String,
    val contenidoHtml: String = "",
    val hora: String? = null, // "18:30" o null
    val notificacionActiva: Boolean = false,
    val seleccionado: Boolean = false,
    val completada: Boolean = false,
    val fechaCompletado: String? = null, // ISO DateTime
    val caloriasQuemadas: Int? = null,
    val distanciaKm: Double? = null,
    val duracionMinutos: Int? = null,
    val notas: String? = null
)

