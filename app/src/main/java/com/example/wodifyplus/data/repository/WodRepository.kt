package com.example.wodifyplus.data.repository

import com.example.wodifyplus.data.local.WodDao
import com.example.wodifyplus.data.local.entities.WodEntity
import com.example.wodifyplus.data.models.Wod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WodRepository(private val wodDao: WodDao) {

    // Conversión Entity -> Model
    private fun WodEntity.toWod(): Wod {
        return Wod(
            id = id,
            fecha = LocalDate.parse(fecha),
            diaSemana = diaSemana,
            gimnasio = gimnasio,
            contenido = contenido,
            contenidoHtml = contenidoHtml,
            hora = hora?.let { LocalTime.parse(it) },
            notificacionActiva = notificacionActiva,
            seleccionado = seleccionado,
            completada = completada,
            fechaCompletado = fechaCompletado?.let { LocalDateTime.parse(it) },
            caloriasQuemadas = caloriasQuemadas,
            distanciaKm = distanciaKm,
            duracionMinutos = duracionMinutos,
            notas = notas
        )
    }

    // Conversión Model -> Entity
    private fun Wod.toEntity(): WodEntity {
        return WodEntity(
            id = id,
            fecha = fecha.format(DateTimeFormatter.ISO_LOCAL_DATE),
            diaSemana = diaSemana,
            gimnasio = gimnasio,
            contenido = contenido,
            contenidoHtml = contenidoHtml,
            hora = hora?.format(DateTimeFormatter.ISO_LOCAL_TIME),
            notificacionActiva = notificacionActiva,
            seleccionado = seleccionado,
            completada = completada,
            fechaCompletado = fechaCompletado?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            caloriasQuemadas = caloriasQuemadas,
            distanciaKm = distanciaKm,
            duracionMinutos = duracionMinutos,
            notas = notas
        )
    }

    // Operaciones
    val allWods: Flow<List<Wod>> = wodDao.getAllWods().map { entities ->
        entities.map { it.toWod() }
    }

    val selectedWods: Flow<List<Wod>> = wodDao.getSelectedWods().map { entities ->
        entities.map { it.toWod() }
    }
    
    val completedWods: Flow<List<Wod>> = wodDao.getCompletedWods().map { entities ->
        entities.map { it.toWod() }
    }
    
    fun getCompletedWodsBetween(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Wod>> {
        val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val endStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        return wodDao.getCompletedWodsBetween(startStr, endStr).map { entities ->
            entities.map { it.toWod() }
        }
    }

    fun getWodsByDate(date: LocalDate): Flow<List<Wod>> {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return wodDao.getWodsByDate(dateStr).map { entities ->
            entities.map { it.toWod() }
        }
    }

    suspend fun getWodById(id: Int): Wod? {
        return wodDao.getWodById(id)?.toWod()
    }

    suspend fun insertWod(wod: Wod): Long {
        return wodDao.insertWod(wod.toEntity())
    }

    suspend fun insertWods(wods: List<Wod>) {
        wodDao.insertWods(wods.map { it.toEntity() })
    }

    suspend fun updateWod(wod: Wod) {
        wodDao.updateWod(wod.toEntity())
    }

    suspend fun deleteWod(wod: Wod) {
        wodDao.deleteWod(wod.toEntity())
    }

    suspend fun deleteAllWods() {
        wodDao.deleteAllWods()
    }

    suspend fun deleteOldWods(beforeDate: LocalDate) {
        val dateStr = beforeDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        wodDao.deleteOldWods(dateStr)
    }
}

