package com.example.wodifyplus.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.wodifyplus.MainActivity
import com.example.wodifyplus.R
import com.example.wodifyplus.data.local.WodDatabase
import com.example.wodifyplus.data.repository.WodRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId

class WodWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "WodWidget"
        private const val REQUEST_CODE_UPDATE = 1001
        private const val ACTION_SCHEDULED_UPDATE = "com.example.wodifyplus.widget.ACTION_SCHEDULED_UPDATE"
        
        /**
         * Actualizar todos los widgets desde cualquier parte de la app
         */
        fun updateAllWidgets(context: Context) {
            Log.d(TAG, "updateAllWidgets called")
            val intent = Intent(context, WodWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, WodWidgetProvider::class.java)
            )
            Log.d(TAG, "Updating ${ids.size} widgets")
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_SCHEDULED_UPDATE,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.d(TAG, "Received time/date change (${intent.action}), refreshing widget")
                updateAllWidgets(context)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called with ${appWidgetIds.size} widgets")
        // Actualizar cada instancia del widget
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        Log.d(TAG, "Widget enabled")
        updateAllWidgets(context)
    }

    override fun onDisabled(context: Context) {
        Log.d(TAG, "Widget disabled")
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.d(TAG, "updateAppWidget called for id: $appWidgetId")
        
        try {
            // Crear RemoteViews primero con estado por defecto
            val views = RemoteViews(context.packageName, R.layout.widget_wod)
            var nextTrigger: LocalDateTime? = null
            
            // Usar runBlocking para obtener datos sincrónicamente
            runBlocking {
                val now = LocalDateTime.now()
                try {
                    val repository = WodRepository(WodDatabase.getDatabase(context).wodDao())
                    
                    // Obtener el próximo WOD
                    val wods = repository.selectedWods.first()
                    Log.d(TAG, "Found ${wods.size} selected wods")
                    
                    Log.d(TAG, "Current time: $now, Selected wods: ${wods.size}")

                    // Regla: mostrar el WOD cuya fecha+hora > ahora (el más próximo aún no iniciado).
                    // Una vez llegada su hora desaparece, completado o no.
                    // Sin hora: visible todo el día (atStartOfDay como referencia).
                    val nextWodPair = wods
                        .mapNotNull { wod ->
                            val wodDateTime = if (wod.hora != null) {
                                wod.fecha.atTime(wod.hora)
                            } else {
                                wod.fecha.atStartOfDay()
                            }
                            if (wodDateTime.isAfter(now)) wod to wodDateTime else null
                        }
                        .minByOrNull { it.second }
                        .also { Log.d(TAG, "Next WOD pair: ${it?.first?.gimnasio} ${it?.second}") }

                    val nextWod = nextWodPair?.first

                    if (nextWod != null) {
                        Log.d(TAG, "Next WOD: ${nextWod.gimnasio} - ${nextWod.fecha}")
                        
                        // Mostrar contenido
                        views.setViewVisibility(R.id.widget_content_layout, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_no_activity, View.GONE)

                        // Configurar datos
                        views.setTextViewText(R.id.widget_gimnasio, nextWod.gimnasio)
                        views.setTextViewText(
                            R.id.widget_fecha,
                            "${nextWod.diaSemana} ${nextWod.fecha.format(DateTimeFormatter.ofPattern("dd/MM"))}"
                        )
                        nextWod.hora?.let { hora ->
                            views.setTextViewText(
                                R.id.widget_hora,
                                hora.format(DateTimeFormatter.ofPattern("HH:mm"))
                            )
                        } ?: run {
                            views.setTextViewText(R.id.widget_hora, context.getString(R.string.widget_no_time))
                        }
                        views.setTextViewText(R.id.widget_contenido, nextWod.contenido)

                        // Programar próxima actualización:
                        // - Si el WOD mostrado aún no llegó → actualizar 5 min después de su hora
                        // - Si ya pasó su hora (pendiente no completado) → actualizar mañana por si cambia algo
                        val wodDateTime = nextWodPair?.second
                        nextTrigger = if (wodDateTime != null && wodDateTime.isAfter(now)) {
                            wodDateTime.plusMinutes(5)
                        } else {
                            LocalDate.now().plusDays(1).atStartOfDay().plusMinutes(5)
                        }
                    } else {
                        Log.d(TAG, "No upcoming activities found")
                        // No hay próxima actividad
                        views.setViewVisibility(R.id.widget_content_layout, View.GONE)
                        views.setViewVisibility(R.id.widget_no_activity, View.VISIBLE)
                        // Programar actualización para el día siguiente por si llegan nuevos datos
                        nextTrigger = LocalDate.now().plusDays(1).atStartOfDay().plusMinutes(5)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading WOD data", e)
                    // Mostrar mensaje de error
                    views.setViewVisibility(R.id.widget_content_layout, View.GONE)
                    views.setViewVisibility(R.id.widget_no_activity, View.VISIBLE)
                    views.setTextViewText(R.id.widget_no_activity, "Error al cargar datos")
                    // Reintentar en una hora
                    nextTrigger = now.plusHours(1)
                }
            }

            // Intent para abrir la app al tocar el widget
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Hacer clickeable tanto el título como el contenido
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_content_layout, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_no_activity, pendingIntent)

            // Actualizar el widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget updated successfully")

            scheduleNextUpdate(context, nextTrigger)
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error updating widget", e)
        }
    }

    private fun scheduleNextUpdate(context: Context, triggerDateTime: LocalDateTime?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_UPDATE,
            Intent(context, WodWidgetProvider::class.java).apply {
                action = ACTION_SCHEDULED_UPDATE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancelar cualquier actualización pendiente
        alarmManager.cancel(pendingIntent)

        if (triggerDateTime != null) {
            val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (triggerMillis > System.currentTimeMillis()) {
                Log.d(TAG, "Scheduling next update for $triggerDateTime")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                Log.d(TAG, "Skipping schedule, trigger time $triggerDateTime is in the past")
            }
        } else {
            Log.d(TAG, "No trigger time provided, widget will rely on manual updates")
        }
    }
}

