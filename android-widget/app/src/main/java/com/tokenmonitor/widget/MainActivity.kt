package com.tokenmonitor.widget

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.glance.appwidget.GlanceAppWidgetManager

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiUrl = intent?.getStringExtra("api_url")
            ?: getSharedPreferences("token_widget_prefs", MODE_PRIVATE)
                .getString("api_url", TokenWidget.BUILD_DEFAULT_URL)
            ?: TokenWidget.BUILD_DEFAULT_URL

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xFF0f172a.toInt())
        }

        val title = TextView(this).apply {
            text = "⚡ Token Monitor"
            textSize = 24f
            setTextColor(0xFFe2e8f0.toInt())
            setPadding(0, 0, 0, 8)
        }
        layout.addView(title)

        val subtitle = TextView(this).apply {
            text = "API 余额监控"
            textSize = 14f
            setTextColor(0xFF94a3b8.toInt())
            setPadding(0, 0, 0, 32)
        }
        layout.addView(subtitle)

        // Show server URL
        val urlLabel = TextView(this).apply {
            text = "当前后端: $apiUrl"
            textSize = 12f
            setTextColor(0xFF64748b.toInt())
            setPadding(0, 0, 0, 16)
        }
        layout.addView(urlLabel)

        // Open in browser button
        val openBtn = Button(this).apply {
            text = "在浏览器中打开"
            setBackgroundColor(0xFF4f46e5.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apiUrl))
                startActivity(intent)
            }
        }
        layout.addView(openBtn)

        // Add widget instructions
        val instructions = TextView(this).apply {
            text = """

使用方法：
1. 回到桌面，长按空白区域
2. 选择「添加小组件」
3. 找到「Token Monitor」
4. 拖到桌面即可

小组件每15分钟自动刷新一次。
点击小组件可打开此页面。
            """.trimIndent()
            textSize = 13f
            setTextColor(0xFF94a3b8.toInt())
            setPadding(0, 32, 0, 0)
        }
        layout.addView(instructions)

        setContentView(layout)
    }
}
