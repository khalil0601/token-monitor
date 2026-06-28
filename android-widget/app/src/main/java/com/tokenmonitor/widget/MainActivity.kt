package com.tokenmonitor.widget

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private lateinit var balanceText: TextView
    private lateinit var subText: TextView
    private lateinit var tokenText: TextView
    private lateinit var timeText: TextView
    private lateinit var statusDot: View
    private lateinit var refreshBtn: TextView
    private lateinit var cardView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiUrl = intent?.getStringExtra("api_url")
            ?: getSharedPreferences("token_widget_prefs", MODE_PRIVATE)
                .getString("api_url", "https://khalil0601.github.io/token-monitor")
            ?: "https://khalil0601.github.io/token-monitor"

        // Root with gradient background
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0a0a1a"))
            setPadding(24, 60, 24, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }

        header.addView(TextView(this).apply {
            text = "⚡ Token Monitor"
            textSize = 20f; setTextColor(Color.parseColor("#e2e8f0"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        refreshBtn = TextView(this).apply {
            text = "🔄"; textSize = 22f; setPadding(16, 4, 0, 4)
            setOnClickListener { refreshData(apiUrl) }
        }
        header.addView(refreshBtn)
        root.addView(header)

        // Status line
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }
        statusDot = View(this).apply {
            setBackgroundColor(Color.parseColor("#64748b"))
            layoutParams = LinearLayout.LayoutParams(6, 6).apply { rightMargin = 8 }
        }
        statusRow.addView(statusDot)
        timeText = TextView(this).apply {
            text = "加载中..."; textSize = 11f; setTextColor(Color.parseColor("#64748b"))
        }
        statusRow.addView(timeText)
        root.addView(statusRow)

        // Main card
        cardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#12122a"))
            setPadding(28, 28, 28, 24)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Rounded corners via background drawable
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#12122a"))
                cornerRadius = 24f
                setStroke(1, Color.parseColor("#1e1e40"))
            }
        }

        balanceText = TextView(this).apply {
            text = "¥ --"
            textSize = 48f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }
        cardView.addView(balanceText)

        subText = TextView(this).apply {
            text = "DeepSeek 余额"
            textSize = 13f
            setTextColor(Color.parseColor("#a5b4fc"))
            gravity = Gravity.CENTER
            letterSpacing = 0.05f
            setPadding(0, 0, 0, 20)
        }
        cardView.addView(subText)

        // Divider
        cardView.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#1e1e40"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { bottomMargin = 16 }
        })

        tokenText = TextView(this).apply {
            text = "≈ --  tokens"
            textSize = 12f
            setTextColor(Color.parseColor("#64748b"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 4)
        }
        cardView.addView(tokenText)

        root.addView(cardView)

        // Bottom info
        root.addView(TextView(this).apply {
            text = "数据每 5 分钟自动同步"
            textSize = 10f; setTextColor(Color.parseColor("#252540"))
            gravity = Gravity.CENTER; setPadding(0, 20, 0, 0)
        })

        val scrollView = ScrollView(this)
        scrollView.addView(root)
        setContentView(scrollView)

        refreshData(apiUrl)
    }

    private fun refreshData(apiUrl: String) {
        statusDot.setBackgroundColor(Color.parseColor("#f59e0b"))
        timeText.text = "更新中..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = ApiService.fetchUsage(apiUrl)
                withContext(Dispatchers.Main) {
                    val ds = data?.deepseek
                    if (ds != null && ds.error == null && (ds.totalBalanceCny ?: 0.0) > 0) {
                        val balance = ds.totalBalanceCny ?: 0.0
                        val topped = ds.toppedUpCny ?: 0.0
                        val granted = ds.grantedCny ?: 0.0
                        val tokens = ds.estimatedTokens ?: 0

                        balanceText.text = "¥" + "%.2f".format(balance)

                        val parts = mutableListOf<String>()
                        if (topped > 0) parts.add("充值 ¥" + "%.2f".format(topped))
                        if (granted > 0) parts.add("赠送 ¥" + "%.2f".format(granted))
                        subText.text = "DeepSeek 余额" + if (parts.isNotEmpty()) " · " + parts.joinToString(" · ") else ""

                        tokenText.text = if (tokens >= 1_000_000)
                            "≈ " + "%.1f".format(tokens / 1_000_000.0) + "M tokens" else "≈ " + tokens.toString() + " tokens"

                        statusDot.setBackgroundColor(Color.parseColor("#22c55e"))
                    } else {
                        balanceText.text = "¥ --"
                        subText.text = "未配置 API Key"
                        tokenText.text = ""
                        statusDot.setBackgroundColor(Color.parseColor("#64748b"))
                    }

                    val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    timeText.text = "上次更新: $now"
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    statusDot.setBackgroundColor(Color.parseColor("#ef4444"))
                    timeText.text = "网络错误"
                }
            }
        }
    }
}
