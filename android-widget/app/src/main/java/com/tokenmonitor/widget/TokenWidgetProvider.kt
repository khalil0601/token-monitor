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

            // Set click to open main activity
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("api_url", apiUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setTextViewText(R.id.widget_title, "⚡ Token Monitor")

            // Fetch data and update
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val data = ApiService.fetchUsage(apiUrl)
                    if (data != null) {
                        val sb = StringBuilder()
                        data.deepseek?.let { ds ->
                            if (ds.error == null && (ds.totalBalanceCny ?: 0.0) > 0) {
                                sb.append("● DS: ¥${"%.2f".format(ds.totalBalanceCny)}  余额\n")
                            }
                        }
                        data.anthropic?.let { ant ->
                            if (ant.error == null && (ant.remaining ?: 0) > 0) {
                                val pct = if (ant.usagePercent != null) " ${(100 - ant.usagePercent).toInt()}%" else ""
                                sb.append("● Ant: ${fmt(ant.remaining ?: 0)}$pct\n")
                            }
                        }
                        data.openai?.let { oai ->
                            if (oai.error == null && (oai.remaining ?: 0) > 0) {
                                val pct = if (oai.usagePercent != null) " ${(100 - oai.usagePercent).toInt()}%" else ""
                                sb.append("● OAI: ${fmt(oai.remaining ?: 0)}$pct\n")
                            }
                        }
                        if (sb.isEmpty()) {
                            sb.append("⚠️ 请配置 API Key")
                        }
                        views.setTextViewText(R.id.widget_content, sb.toString().trim())
                    } else {
                        views.setTextViewText(R.id.widget_content, "⏳ 加载中...")
                    }
                } catch (_: Exception) {
                    views.setTextViewText(R.id.widget_content, "❌ 连接失败")
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
