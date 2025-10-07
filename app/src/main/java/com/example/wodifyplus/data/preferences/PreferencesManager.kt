package com.example.wodifyplus.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val PREFERRED_HOUR = intPreferencesKey("preferred_hour")
        private val PREFERRED_MINUTE = intPreferencesKey("preferred_minute")
        private val NOTIFICATION_MINUTES_BEFORE = intPreferencesKey("notification_minutes_before")
    }
    
    val preferredHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PREFERRED_HOUR] ?: 18 // Default: 18:00
    }
    
    val preferredMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PREFERRED_MINUTE] ?: 0
    }
    
    val notificationMinutesBefore: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_MINUTES_BEFORE] ?: 60 // Default: 1 hora antes
    }
    
    suspend fun setPreferredTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_HOUR] = hour
            preferences[PREFERRED_MINUTE] = minute
        }
    }
    
    suspend fun setNotificationMinutesBefore(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_MINUTES_BEFORE] = minutes
        }
    }
}

