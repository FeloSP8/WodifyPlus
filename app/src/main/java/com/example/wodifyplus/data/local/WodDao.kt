package com.example.wodifyplus.data.local

import androidx.room.*
import com.example.wodifyplus.data.local.entities.WodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WodDao {
    @Query("SELECT * FROM wods ORDER BY fecha ASC")
    fun getAllWods(): Flow<List<WodEntity>>

    @Query("SELECT * FROM wods WHERE fecha = :fecha")
    fun getWodsByDate(fecha: String): Flow<List<WodEntity>>

    @Query("SELECT * FROM wods WHERE seleccionado = 1 ORDER BY fecha ASC")
    fun getSelectedWods(): Flow<List<WodEntity>>
    
    @Query("SELECT * FROM wods WHERE completada = 1 ORDER BY fechaCompletado DESC")
    fun getCompletedWods(): Flow<List<WodEntity>>
    
    @Query("SELECT * FROM wods WHERE completada = 1 AND fechaCompletado >= :fechaDesde AND fechaCompletado <= :fechaHasta ORDER BY fechaCompletado ASC")
    fun getCompletedWodsBetween(fechaDesde: String, fechaHasta: String): Flow<List<WodEntity>>

    @Query("SELECT * FROM wods WHERE id = :id")
    suspend fun getWodById(id: Int): WodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWod(wod: WodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWods(wods: List<WodEntity>)

    @Update
    suspend fun updateWod(wod: WodEntity)

    @Delete
    suspend fun deleteWod(wod: WodEntity)

    @Query("DELETE FROM wods")
    suspend fun deleteAllWods()

    @Query("DELETE FROM wods WHERE fecha < :fecha")
    suspend fun deleteOldWods(fecha: String)
}

