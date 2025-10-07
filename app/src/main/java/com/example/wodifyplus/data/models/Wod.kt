package com.example.wodifyplus.data.models

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Wod(
    val id: Int = 0,
    val fecha: LocalDate,
    val diaSemana: String,
    val gimnasio: String, // "CrossFit DB" o "N8"
    val contenido: String,
    val contenidoHtml: String = "",
    val hora: LocalTime? = null,
    val notificacionActiva: Boolean = false,
    val seleccionado: Boolean = false,
    val completada: Boolean = false,
    val fechaCompletado: LocalDateTime? = null,
    val caloriasQuemadas: Int? = null,
    val distanciaKm: Double? = null,
    val duracionMinutos: Int? = null,
    val notas: String? = null
)

