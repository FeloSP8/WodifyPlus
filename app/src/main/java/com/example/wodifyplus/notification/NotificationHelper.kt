package com.example.wodifyplus.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.wodifyplus.R

class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "wod_weekly_planner"
        private const val CHANNEL_NAME = "WOD Weekly Planner"
        private const val CHANNEL_DESCRIPTION = "Notificaciones de planificación semanal de WODs"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun createWodPlannedNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setContentTitle("Semana lista")
            .setContentText("Tus WODs han sido planificados para esta semana")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
    }
    
    fun createWodReminderNotification(gymName: String, time: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setContentTitle("¡WOD en $gymName!")
            .setContentText("Tu clase comienza a las $time")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
    }
}






