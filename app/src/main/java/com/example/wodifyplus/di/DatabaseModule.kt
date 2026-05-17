package com.example.wodifyplus.di

import android.content.Context
import com.example.wodifyplus.data.local.ActivityConfigDao
import com.example.wodifyplus.data.local.WodDao
import com.example.wodifyplus.data.local.WodDatabase
import com.example.wodifyplus.data.repository.WodRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WodDatabase {
        return WodDatabase.getDatabase(context)
    }

    @Provides
    fun provideWodDao(database: WodDatabase): WodDao {
        return database.wodDao()
    }

    @Provides
    fun provideActivityConfigDao(database: WodDatabase): ActivityConfigDao {
        return database.activityConfigDao()
    }

    @Provides
    @Singleton
    fun provideWodRepository(wodDao: WodDao): WodRepository {
        return WodRepository(wodDao)
    }
}
