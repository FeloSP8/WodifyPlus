package com.example.wodifyplus.data.local

import androidx.room.*
import com.example.wodifyplus.data.local.entities.ActivityConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityConfigDao {
    @Query("SELECT * FROM activity_configs WHERE isEnabled = 1 ORDER BY isDefault DESC, name ASC")
    fun getAllActiveConfigs(): Flow<List<ActivityConfigEntity>>
    
    @Query("SELECT * FROM activity_configs ORDER BY isDefault DESC, name ASC")
    fun getAllConfigs(): Flow<List<ActivityConfigEntity>>
    
    @Query("SELECT * FROM activity_configs WHERE id = :id")
    suspend fun getConfigById(id: Int): ActivityConfigEntity?
    
    @Query("SELECT * FROM activity_configs WHERE name = :name")
    suspend fun getConfigByName(name: String): ActivityConfigEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ActivityConfigEntity): Long
    
    @Update
    suspend fun updateConfig(config: ActivityConfigEntity)
    
    @Delete
    suspend fun deleteConfig(config: ActivityConfigEntity)
    
    @Query("DELETE FROM activity_configs WHERE isDefault = 0")
    suspend fun deleteAllCustomConfigs()
}


