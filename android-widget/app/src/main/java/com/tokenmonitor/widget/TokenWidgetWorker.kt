package com.tokenmonitor.widget

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.*
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class TokenWidgetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("TokenWidget", "Starting widget update...")
            val widget = TokenWidget()
            widget.updateAll(applicationContext)
            Log.d("TokenWidget", "Widget update complete")
            Result.success()
        } catch (e: Exception) {
            Log.e("TokenWidget", "Update failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "token_widget_update"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<TokenWidgetWorker>(
                15, TimeUnit.MINUTES // Update every 15 minutes
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
