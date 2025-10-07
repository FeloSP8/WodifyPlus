package com.example.wodifyplus.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wodifyplus.data.local.WodDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WodReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val wodId = inputData.getInt("WOD_ID", -1)
            if (wodId == -1) return@withContext Result.failure()

            // Obtener WOD de la BD
            val wodDao = WodDatabase.getDatabase(applicationContext).wodDao()
            val wod = wodDao.getWodById(wodId) ?: return@withContext Result.failure()

            // Enviar notificación
            NotificationHelper.sendWodReminder(
                context = applicationContext,
                wodId = wod.id,
                title = "🏋️ Es hora de entrenar!",
                content = "${wod.diaSemana} - ${wod.gimnasio}"
            )

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

