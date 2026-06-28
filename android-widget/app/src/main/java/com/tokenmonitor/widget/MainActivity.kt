package com.tokenmonitor.widget

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import kotlinx.coroutines.*

class MainActivity : Activity() {

    private lateinit var contentLayout: LinearLayout
    private lateinit var timeText: TextView
    private lateinit var statusDot: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiUrl = intent?.getStringExtra("api_url")
            ?: getSharedPreferences("token_widget_prefs", MODE_PRIVATE)
                .getString("api_url", "https://khalil0601.github.io/token-monitor")
            ?: "https://khalil0601.github.io/token-monitor"

        val rootLayout = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0f172a"))
            setPadding(0, 40, 0, 40)
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 24)
        }

        // Header
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(TextView(this).apply {
            text = "⚡ Token Monitor"
            textSize = 22f; setTextColor(Color.parseColor("#e2e8f0"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val refreshBtn = TextView(this).apply {
            text = "🔄"; textSize = 22f; setPadding(16, 0, 0, 0)
            setOnClickListener { refreshData(apiUrl) }
        }
        headerRow.addView(refreshBtn)
        mainLayout.addView(headerRow)

        // Status line
        val statusRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 16) }
        statusDot = TextView(this).apply {
            text = "●"; textSize = 8f
            setTextColor(Color.parseColor("#64748b"))
            setPadding(0, 0, 6, 0)
        }
        statusRow.addView(statusDot)
        timeText = TextView(this).apply {
            text = "加载中..."; textSize = 11f
            setTextColor(Color.parseColor("#94a3b8"))
        }
        statusRow.addView(timeText)
        mainLayout.addView(statusRow)

        // Content area
        contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        mainLayout.addView(contentLayout)

        // URL info at bottom
        mainLayout.addView(TextView(this).apply {
            text = "\n后端: $apiUrl"
            textSize = 9f; setTextColor(Color.parseColor("#334155"))
            setPadding(0, 24, 0, 0)
        })

        rootLayout.addView(mainLayout)
        setContentView(rootLayout)

        // Initial load
        refreshData(apiUrl)
    }

    private fun refreshData(apiUrl: String) {
        statusDot.setTextColor(Color.parseColor("#f59e0b"))
        timeText.text = "更新中..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = ApiService.fetchUsage(apiUrl)
                withContext(Dispatchers.Main) {
                    contentLayout.removeAllViews()

                    if (data != null) {
                        var hasData = false

                        // DeepSeek card
                        data.deepseek?.let { ds ->
                            if (ds.error == null && (ds.totalBalanceCny ?: 0.0) > 0) {
                                hasData = true
                                addCard("🔵 DeepSeek", "#4f46e5", listOf(
                                    "余额" to "¥${"%.2f".format(ds.totalBalanceCny)}",
                                    "充值" to "¥${"%.2f".format(ds.toppedUpCny ?: 0.0)}",
                                    "赠送" to "¥${"%.2f".format(ds.grantedCny ?: 0.0)}",
                                    "预估可用" to "${if ((ds.estimatedTokens ?: 0) >= 1_000_000) "%.1fM".format((ds.estimatedTokens ?: 0) / 1_000_000.0) else (ds.estimatedTokens ?: 0).toString()} tokens"
                                ))
                            }
                        }

                        // Anthropic card
                        data.anthropic?.let { ant ->
                            if (ant.error == null && (ant.remaining ?: 0) > 0) {
                                hasData = true
                                val remain = ant.remaining ?: 0
                                val limit = ant.monthlyLimit ?: 0
                                val pct = if (ant.usagePercent != null) "%.1f%%".format(ant.usagePercent) else "--"
                                addCard("🟠 Anthropic", "#d97706", listOf(
                                    "已用" to "${fmt(remain)} tokens",
                                    "限额" to "${fmt(limit)} tokens",
                                    "使用率" to pct,
                                    "预估费用" to "$${"%.2f".format(ant.estimatedCostUsd ?: 0.0)}"
                                ))
                            }
                        }

                        // OpenAI card
                        data.openai?.let { oai ->
                            if (oai.error == null && (oai.remaining ?: 0) > 0) {
                                hasData = true
                                val remain = oai.remaining ?: 0
                                val limit = oai.monthlyLimit ?: 0
                                val pct = if (oai.usagePercent != null) "%.1f%%".format(oai.usagePercent) else "--"
                                addCard("🟢 OpenAI", "#10a37f", listOf(
                                    "已用" to "${fmt(remain)} tokens",
                                    "限额" to "${fmt(limit)} tokens",
                                    "使用率" to pct,
                                    "预估费用" to "$${"%.2f".format(oai.estimatedCostUsd ?: 0.0)}"
                                ))
                            }
                        }

                        if (!hasData) {
                            contentLayout.addView(emptyText("⚠️ 请配置 API Key"))
                        }

                        statusDot.setTextColor(Color.parseColor("#22c55e"))
                        val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        timeText.text = "上次更新: $now"
                    } else {
                        contentLayout.addView(emptyText("❌ 连接失败"))
                        statusDot.setTextColor(Color.parseColor("#ef4444"))
                        timeText.text = "连接失败"
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    contentLayout.removeAllViews()
                    contentLayout.addView(emptyText("❌ 网络错误"))
                    statusDot.setTextColor(Color.parseColor("#ef4444"))
                    timeText.text = "网络错误"
                }
            }
        }
    }

    private fun addCard(title: String, colorHex: String, items: List<Pair<String, String>>) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1e293b"))
            setPadding(16, 14, 16, 14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }

        card.addView(TextView(this).apply {
            text = title
            textSize = 14f; setTextColor(Color.parseColor("#e2e8f0"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        })

        for ((label, value) in items) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
            }
            row.addView(TextView(this).apply {
                text = label; textSize = 12f
                setTextColor(Color.parseColor("#94a3b8"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = value; textSize = 13f
                setTextColor(Color.parseColor(colorHex))
                setTypeface(null, android.graphics.Typeface.BOLD)
            })
            card.addView(row)
        }

        contentLayout.addView(card)
    }

    private fun emptyText(msg: String): TextView {
        return TextView(this).apply {
            text = msg; textSize = 14f
            setTextColor(Color.parseColor("#94a3b8"))
            setPadding(0, 40, 0, 40)
            gravity = Gravity.CENTER
        }
    }

    private fun fmt(n: Long) = when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
        n >= 1_000 -> "%.1fK".format(n / 1_000.0)
        else -> n.toString()
    }
}
