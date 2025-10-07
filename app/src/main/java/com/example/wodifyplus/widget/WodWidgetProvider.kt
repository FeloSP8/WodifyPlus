package com.example.wodifyplus.widget

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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WodWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "WodWidget"
        
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
            
            // Usar runBlocking para obtener datos sincrónicamente
            runBlocking {
                try {
                    val repository = WodRepository(WodDatabase.getDatabase(context).wodDao())
                    
                    // Obtener el próximo WOD
                    val wods = repository.selectedWods.first()
                    Log.d(TAG, "Found ${wods.size} selected wods")
                    
                    val now = LocalDateTime.now()
                    val nextWod = wods
                        .filter { it.hora != null && !it.completada }
                        .mapNotNull { wod ->
                            wod.hora?.let { hora ->
                                val wodDateTime = wod.fecha.atTime(hora)
                                if (wodDateTime.isAfter(now)) wod to wodDateTime else null
                            }
                        }
                        .minByOrNull { it.second }
                        ?.first

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
                            views.setTextViewText(R.id.widget_hora, "")
                        }
                        views.setTextViewText(R.id.widget_contenido, nextWod.contenido)
                    } else {
                        Log.d(TAG, "No upcoming activities found")
                        // No hay próxima actividad
                        views.setViewVisibility(R.id.widget_content_layout, View.GONE)
                        views.setViewVisibility(R.id.widget_no_activity, View.VISIBLE)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading WOD data", e)
                    // Mostrar mensaje de error
                    views.setViewVisibility(R.id.widget_content_layout, View.GONE)
                    views.setViewVisibility(R.id.widget_no_activity, View.VISIBLE)
                    views.setTextViewText(R.id.widget_no_activity, "Error al cargar datos")
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
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

            // Actualizar el widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error updating widget", e)
        }
    }
}

