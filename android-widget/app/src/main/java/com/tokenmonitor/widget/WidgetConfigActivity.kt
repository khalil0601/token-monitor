package com.tokenmonitor.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xFF0f172a.toInt())
        }

        val title = TextView(this).apply {
            text = "Token Monitor 小组件配置"
            textSize = 20f
            setTextColor(0xFFe2e8f0.toInt())
            setPadding(0, 0, 0, 16)
        }
        layout.addView(title)

        val hint = TextView(this).apply {
            text = "后端服务器地址："
            textSize = 14f
            setTextColor(0xFF94a3b8.toInt())
            setPadding(0, 16, 0, 8)
        }
        layout.addView(hint)

        val prefs = getSharedPreferences("token_widget_prefs", MODE_PRIVATE)
        val defaultUrl = prefs.getString("api_url", TokenWidget.BUILD_DEFAULT_URL)

        val urlInput = EditText(this).apply {
            setText(defaultUrl)
            textSize = 14f
            setTextColor(0xFFe2e8f0.toInt())
            setBackgroundColor(0xFF1e293b.toInt())
            setPadding(24, 16, 24, 16)
            hint = "https://your-server.com"
            setHintTextColor(0xFF475569.toInt())
        }
        layout.addView(urlInput)

        val saveBtn = Button(this).apply {
            text = "添加到桌面"
            setBackgroundColor(0xFF4f46e5.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                val url = urlInput.text.toString().trim()
                if (url.isNotEmpty()) {
                    prefs.edit().putString("api_url", url).apply()

                    // Launch coroutine for suspend widget update
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity)
                                .getGlanceIdBy(appWidgetId)
                            TokenWidget().update(this@WidgetConfigActivity, glanceId)
                        } catch (_: Exception) {
                            // Widget update is best-effort during config
                        }
                    }

                    val result = Intent().putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        appWidgetId
                    )
                    setResult(RESULT_OK, result)
                    finish()
                }
            }
        }
        layout.addView(saveBtn)

        setContentView(layout)
    }
}
