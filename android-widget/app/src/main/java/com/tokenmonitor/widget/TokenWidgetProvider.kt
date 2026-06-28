package com.tokenmonitor.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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

            // Tap to open app (shows full dashboard)
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("api_url", apiUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Fetch data
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val data = ApiService.fetchUsage(apiUrl)
                    val sb = StringBuilder()
                    if (data != null) {
                        data.deepseek?.let { ds ->
                            if (ds.error == null && (ds.totalBalanceCny ?: 0.0) > 0) {
                                sb.append("● DS: ¥${"%.2f".format(ds.totalBalanceCny)}\n")
                            }
                        }
                        data.anthropic?.let { ant ->
                            if (ant.error == null && (ant.remaining ?: 0) > 0) {
                                sb.append("● Ant: ${fmt(ant.remaining ?: 0)}\n")
                            }
                        }
                        data.openai?.let { oai ->
                            if (oai.error == null && (oai.remaining ?: 0) > 0) {
                                sb.append("● OAI: ${fmt(oai.remaining ?: 0)}\n")
                            }
                        }
                    }
                    if (sb.isEmpty()) sb.append("⚠️ 未配置")
                    views.setTextViewText(R.id.widget_title, "⚡ Token")
                    views.setTextViewText(R.id.widget_content, sb.toString().trim())
                } catch (_: Exception) {
                    views.setTextViewText(R.id.widget_title, "⚡ Token")
                    views.setTextViewText(R.id.widget_content, "❌ 离线")
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun fmt(n: Long) = when {
            n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
            n >= 1_000 -> "%.1fK".format(n / 1_000.0)
            else -> n.toString()
        }
    }
}
