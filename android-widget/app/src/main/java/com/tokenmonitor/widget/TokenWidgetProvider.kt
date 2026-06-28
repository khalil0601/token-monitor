package com.tokenmonitor.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import kotlinx.coroutines.*

class TokenWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val prefs = context.getSharedPreferences("token_widget_prefs", Context.MODE_PRIVATE)
            val apiUrl = prefs.getString("api_url", "https://khalil0601.github.io/token-monitor")
                ?: "https://khalil0601.github.io/token-monitor"

            // Tap opens app
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("api_url", apiUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pi = PendingIntent.getActivity(context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pi)

            // Show cached balance if available, then fetch fresh
            val cached = prefs.getString("cached_balance", null)
            if (cached != null) {
                views.setTextViewText(R.id.widget_balance, cached)
            } else {
                views.setTextViewText(R.id.widget_balance, "¥--")
            }
            views.setTextViewText(R.id.widget_label, "DeepSeek")
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Fetch fresh data
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val data = ApiService.fetchUsage(apiUrl)
                    val ds = data?.deepseek
                    if (ds != null && ds.error == null && (ds.totalBalanceCny ?: 0.0) > 0) {
                        val balance = "¥" + "%.2f".format(ds.totalBalanceCny)
                        views.setTextViewText(R.id.widget_balance, balance)
                        prefs.edit().putString("cached_balance", balance).apply()
                    } else {
                        views.setTextViewText(R.id.widget_balance, "¥--")
                    }
                } catch (_: Exception) {
                    // Keep cached value, don't overwrite with error
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
