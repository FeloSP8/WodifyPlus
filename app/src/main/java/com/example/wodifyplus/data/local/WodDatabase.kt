package com.example.wodifyplus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.wodifyplus.data.local.entities.ActivityConfigEntity
import com.example.wodifyplus.data.local.entities.WodEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [WodEntity::class, ActivityConfigEntity::class],
    version = 3,
    exportSchema = false
)
abstract class WodDatabase : RoomDatabase() {
    abstract fun wodDao(): WodDao
    abstract fun activityConfigDao(): ActivityConfigDao

    companion object {
        @Volatile
        private var INSTANCE: WodDatabase? = null

        fun getDatabase(context: Context): WodDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WodDatabase::class.java,
                    "wodify_plus_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Insertar configuraciones por defecto
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateDefaultConfigs(database.activityConfigDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        private suspend fun populateDefaultConfigs(dao: ActivityConfigDao) {
            // CrossFit DB - Lunes a Sábado por defecto
            dao.insertConfig(
                ActivityConfigEntity(
                    name = "CrossFit DB",
                    monday = true,
                    tuesday = true,
                    wednesday = true,
                    thursday = true,
                    friday = true,
                    saturday = true,
                    sunday = false,
                    preferredHour = 18,
                    preferredMinute = 0,
                    isEnabled = true,
                    isDefault = true
                )
            )
            
            // N8 - Lunes a Sábado por defecto
            dao.insertConfig(
                ActivityConfigEntity(
                    name = "N8",
                    monday = true,
                    tuesday = true,
                    wednesday = true,
                    thursday = true,
                    friday = true,
                    saturday = true,
                    sunday = false,
                    preferredHour = 18,
                    preferredMinute = 0,
                    isEnabled = true,
                    isDefault = true
                )
            )
        }
    }
}

