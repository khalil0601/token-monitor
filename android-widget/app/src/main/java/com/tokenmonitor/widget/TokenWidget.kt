package com.tokenmonitor.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class TokenWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("token_widget_prefs", Context.MODE_PRIVATE)
        val apiUrl = prefs.getString("api_url", BUILD_DEFAULT_URL) ?: BUILD_DEFAULT_URL

        val data = try {
            ApiService.fetchUsage(apiUrl)
        } catch (_: Exception) {
            null
        }

        // Pre-compute display strings
        val dsBalance = if (data?.deepseek != null && data.deepseek.error == null && (data.deepseek.totalBalanceCny ?: 0.0) > 0)
            "¥" + "%.2f".format(data.deepseek.totalBalanceCny) else ""
        val dsSub = if (dsBalance.isNotEmpty()) "余额" else ""

        val antRemaining = data?.anthropic?.remaining
        val antLimit = data?.anthropic?.monthlyLimit
        val antPct = data?.anthropic?.usagePercent
        val antValue = if (antRemaining != null && antRemaining > 0)
            fmt(antRemaining) + if (antPct != null) " " + (100 - antPct).toInt().toString() + "%" else "" else ""
        val antSub = if (antRemaining != null && antRemaining > 0) "剩余 / " + fmt(antLimit ?: 0) else ""

        val oaiRemaining = data?.openai?.remaining
        val oaiLimit = data?.openai?.monthlyLimit
        val oaiPct = data?.openai?.usagePercent
        val oaiValue = if (oaiRemaining != null && oaiRemaining > 0)
            fmt(oaiRemaining) + if (oaiPct != null) " " + (100 - oaiPct).toInt().toString() + "%" else "" else ""
        val oaiSub = if (oaiRemaining != null && oaiRemaining > 0) "剩余 / " + fmt(oaiLimit ?: 0) else ""

        val showAny = dsBalance.isNotEmpty() || antValue.isNotEmpty() || oaiValue.isNotEmpty()

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(Color.parseColor("#1e293b")))
                        .cornerRadius(16)
                        .padding(12)
                        .clickable(
                            onClick = actionStartActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    putExtra("api_url", apiUrl)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        )
                ) {
                    // Header
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = "⚡ Token Monitor",
                            style = TextStyle(
                                color = ColorProvider(Color.parseColor("#e2e8f0")),
                                fontSize = 14,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6))

                    // DeepSeek
                    if (dsBalance.isNotEmpty()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = "●",
                                style = TextStyle(
                                    color = ColorProvider(Color.parseColor("#4f46e5")),
                                    fontSize = 8
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(4))
                            Text(
                                text = "DeepSeek",
                                style = TextStyle(
                                    color = ColorProvider(Color.parseColor("#94a3b8")),
                                    fontSize = 11
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Column(horizontalAlignment = Alignment.Horizontal.End) {
                                Text(
                                    text = dsBalance,
                                    style = TextStyle(
                                        color = ColorProvider(Color.parseColor("#e2e8f0")),
                                        fontSize = 12,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = dsSub,
                                    style = TextStyle(
                                        color = ColorProvider(Color.parseColor("#64748b")),
                                        fontSize = 9
                                    )
                                )
                            }
                        }
                    }

                    // Anthropic
                    if (antValue.isNotEmpty()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = "●",
                                style = TextStyle(
                                    color = ColorProvider(Color.parseColor("#d97706")),
                                    fontSize = 8
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(4))
                            Text(
                                text = "Anthropic",
                                style = TextStyle(
                                    color = ColorProvider(Color.parseColor("#94a3b8")),
                                    fontSize = 11
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Column(horizontalAlignment = Alignment.Horizontal.End) {
                                Text(
                                    text = antValue,
                                    style = TextStyle(
                                        color = ColorProvider(Color.parseColor("#e2e8f0")),
                                        fontSize = 12,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = antSub,
                                    style = TextStyle(
                                        color = ColorProvider(Color.parseColor("#64748b")),
                                        fontSize = 9
                                    )
                                )
                            }
                        }
                    }

                    // OpenAI
                    if (oaiValue.isNotEmpty()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = "●",
                                style = TextStyle(
                                    color = ColorProvider(Color.parseColor("#10a37f")),
                                    fontSize = 8
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(4))
                            Text(
                                text = "OpenAI",
                                style = TextStyle(
                                    color = ColorProvider(Color.parseColor("#94a3b8")),
                                    fontSize = 11
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Column(horizontalAlignment = Alignment.Horizontal.End) {
                                Text(
                                    text = oaiValue,
                                    style = TextStyle(
                                        color = ColorProvider(Color.parseColor("#e2e8f0")),
                                        fontSize = 12,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = oaiSub,
                                    style = TextStyle(
                                        color = ColorProvider(Color.parseColor("#64748b")),
                                        fontSize = 9
                                    )
                                )
                            }
                        }
                    }

                    // Fallback
                    if (!showAny) {
                        Text(
                            text = if (data != null) "⚠️ 请先配置 API Key" else "⏳ 加载中...",
                            style = TextStyle(
                                color = ColorProvider(Color.parseColor("#64748b")),
                                fontSize = 12
                            )
                        )
                    }
                }
            }
        }
    }

    private fun fmt(n: Long): String = when {
        n >= 1_000_000 -> "%.1f".format(n / 1_000_000.0) + "M"
        n >= 1_000 -> "%.1f".format(n / 1_000.0) + "K"
        else -> n.toString()
    }

    companion object {
        const val BUILD_DEFAULT_URL = "https://creation-subprime-underwear.ngrok-free.dev"
    }
}

class TokenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TokenWidget()
}
