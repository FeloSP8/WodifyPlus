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
import java.time.format.DateTimeFormatterBuilder
import javax.inject.Inject

class WodRepository @Inject constructor(private val wodDao: WodDao) {

    companion object {
        // Acepta "HH:mm" y "HH:mm:ss" — ISO_LOCAL_TIME omite segundos cuando son 0
        private val TIME_FORMATTER = DateTimeFormatterBuilder()
            .appendPattern("HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .toFormatter()
    }

    // Conversión Entity -> Model
    private fun WodEntity.toWod(): Wod {
        return Wod(
            id = id,
            fecha = LocalDate.parse(fecha),
            diaSemana = diaSemana,
            gimnasio = gimnasio,
            contenido = contenido,
            contenidoHtml = contenidoHtml,
            hora = hora?.let { LocalTime.parse(it, TIME_FORMATTER) },
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
            hora = hora?.format(DateTimeFormatter.ofPattern("HH:mm")),
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
    
    suspend fun deleteWodsBySource(sources: List<String>) {
        wodDao.deleteWodsBySource(sources)
    }
    
    suspend fun updateOrInsertCustomWods(wods: List<Wod>) {
        wods.forEach { wod ->
            val existingWod = wodDao.getWodByGimnasioAndDate(
                wod.gimnasio, 
                wod.fecha.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
            
            if (existingWod != null) {
                // Actualizar WOD existente, preservando datos de selección
                val updatedWod = existingWod.copy(
                    contenido = wod.contenido,
                    contenidoHtml = wod.contenidoHtml
                )
                wodDao.updateWod(updatedWod)
            } else {
                // Insertar nuevo WOD
                wodDao.insertWod(wod.toEntity())
            }
        }
    }
    
    /**
     * Actualiza o inserta WODs de fuentes externas (scraped), preservando:
     * - WODs completados (completada = true) con todos sus datos
     * - WODs seleccionados (seleccionado = true) con hora y notificaciones
     * - Solo actualiza el contenido de WODs que no están completados ni seleccionados
     */
    suspend fun updateOrInsertScrapedWods(wods: List<Wod>, sources: List<String>) {
        // Primero, borrar solo los WODs que NO están seleccionados NI completados
        wodDao.deleteUnselectedUncompletedWodsBySource(sources)
        
        // Luego, hacer merge inteligente de los nuevos WODs
        wods.forEach { newWod ->
            val existingWod = wodDao.getWodByGimnasioAndDate(
                newWod.gimnasio,
                newWod.fecha.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
            
            if (existingWod != null) {
                // WOD existente: preservar datos importantes
                val updatedWod = existingWod.copy(
                    contenido = newWod.contenido,
                    contenidoHtml = newWod.contenidoHtml,
                    diaSemana = newWod.diaSemana
                    // Preservar: seleccionado, completada, hora, notificacionActiva,
                    // fechaCompletado, caloriasQuemadas, distanciaKm, duracionMinutos, notas
                )
                wodDao.updateWod(updatedWod)
            } else {
                // Nuevo WOD: insertar normalmente
                wodDao.insertWod(newWod.toEntity())
            }
        }
    }
}

