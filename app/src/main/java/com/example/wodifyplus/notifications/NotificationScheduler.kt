package com.example.wodifyplus.notifications

import android.content.Context
import androidx.work.*
import com.example.wodifyplus.data.models.Wod
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    
    fun scheduleWodReminder(context: Context, wod: Wod, minutesBefore: Int) {
        // Si no tiene hora, no programar
        val hora = wod.hora ?: return
        
        // Calcular el momento de la notificación
        val wodDateTime = LocalDateTime.of(wod.fecha, hora)
        val notificationTime = wodDateTime.minusMinutes(minutesBefore.toLong())
        val now = LocalDateTime.now()
        
        // Solo programar si es en el futuro
        if (notificationTime.isAfter(now)) {
            val delayMillis = Duration.between(now, notificationTime).toMillis()
            
            val data = Data.Builder()
                .putInt("WOD_ID", wod.id)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<WodReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("wod_reminder_${wod.id}")
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                "wod_reminder_${wod.id}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
    
    fun cancelWodReminder(context: Context, wodId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("wod_reminder_$wodId")
    }
    
    fun rescheduleAllReminders(context: Context, wods: List<Wod>, minutesBefore: Int) {
        wods.forEach { wod ->
            if (wod.seleccionado && wod.hora != null && !wod.completada) {
                scheduleWodReminder(context, wod, minutesBefore)
            }
        }
    }
}


