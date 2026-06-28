package com.tokenmonitor.widget

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiUrl = intent?.getStringExtra("api_url")
            ?: getSharedPreferences("token_widget_prefs", MODE_PRIVATE)
                .getString("api_url", "https://creation-subprime-underwear.ngrok-free.dev")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xFF0f172a.toInt())
        }

        layout.addView(TextView(this).apply {
            text = "⚡ Token Monitor"
            textSize = 22f; setTextColor(0xFFe2e8f0.toInt())
        })
        layout.addView(TextView(this).apply {
            text = "API 余额监控"
            textSize = 14f; setTextColor(0xFF94a3b8.toInt())
            setPadding(0, 0, 0, 32)
        })
        layout.addView(TextView(this).apply {
            text = "后端: $apiUrl"
            textSize = 11f; setTextColor(0xFF64748b.toInt())
        })
        layout.addView(Button(this).apply {
            text = "在浏览器中打开"
            setBackgroundColor(0xFF4f46e5.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apiUrl))) }
        })
        layout.addView(TextView(this).apply {
            text = "\n\n📲 使用方法：\n1. 回到桌面\n2. 长按空白处\n3. 添加小组件\n4. 找到「Token Monitor」\n5. 拖到桌面"
            textSize = 13f; setTextColor(0xFF94a3b8.toInt())
        })

        setContentView(layout)
    }
}
