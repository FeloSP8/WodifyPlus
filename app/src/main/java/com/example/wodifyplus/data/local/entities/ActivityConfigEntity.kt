package com.example.wodifyplus.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_configs")
data class ActivityConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String, // "CrossFit DB", "N8", "Gimnasio", "Club OCR", etc.
    val monday: Boolean = false,
    val tuesday: Boolean = false,
    val wednesday: Boolean = false,
    val thursday: Boolean = false,
    val friday: Boolean = false,
    val saturday: Boolean = false,
    val sunday: Boolean = false,
    val preferredHour: Int = 18,
    val preferredMinute: Int = 0,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false // CrossFit DB y N8 son por defecto
)


