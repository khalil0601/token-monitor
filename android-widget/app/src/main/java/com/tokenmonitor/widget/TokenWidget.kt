package com.tokenmonitor.widget

import android.content.Context
import android.content.Intent
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
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import androidx.glance.unit.sp

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

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(0xFF1e293b.toInt()))
                        .cornerRadius(16.dp)
                        .padding(12.dp)
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
                                color = ColorProvider(0xFFe2e8f0.toInt()),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    if (data != null) {
                        // DeepSeek
                        val dsData = data.deepseek
                        if (dsData != null && dsData.error == null && dsData.totalBalanceCny != null) {
                            makeProviderRow(
                                label = "DeepSeek",
                                value = "¥${"%.2f".format(dsData.totalBalanceCny)}",
                                sub = "余额",
                                color = 0xFF4f46e5.toInt()
                            )
                        }

                        // Anthropic
                        val antData = data.anthropic
                        if (antData != null && antData.error == null && antData.remaining != null) {
                            val pct = if (antData.usagePercent != null) " ${(100 - antData.usagePercent).toInt()}%" else ""
                            makeProviderRow(
                                label = "Anthropic",
                                value = "${fmt(antData.remaining)}$pct",
                                sub = "剩余 / ${fmt(antData.monthlyLimit ?: 0)}",
                                color = 0xFFd97706.toInt()
                            )
                        }

                        // OpenAI
                        val oaiData = data.openai
                        if (oaiData != null && oaiData.error == null && oaiData.remaining != null) {
                            val pct = if (oaiData.usagePercent != null) " ${(100 - oaiData.usagePercent).toInt()}%" else ""
                            makeProviderRow(
                                label = "OpenAI",
                                value = "${fmt(oaiData.remaining)}$pct",
                                sub = "剩余 / ${fmt(oaiData.monthlyLimit ?: 0)}",
                                color = 0xFF10a37f.toInt()
                            )
                        }
                    } else {
                        Text(
                            text = "⏳ 加载中...",
                            style = TextStyle(
                                color = ColorProvider(0xFF64748b.toInt()),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }

    // Must be called within provideContent {} block
    @androidx.compose.runtime.Composable
    private fun makeProviderRow(label: String, value: String, sub: String, color: Int) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "●",
                style = TextStyle(color = ColorProvider(color), fontSize = 8.sp)
            )
            Text(
                text = " $label",
                style = TextStyle(color = ColorProvider(0xFF94a3b8.toInt()), fontSize = 11.sp),
                modifier = GlanceModifier.defaultWeight()
            )
            Column(horizontalAlignment = Alignment.Horizontal.End) {
                Text(
                    text = value,
                    style = TextStyle(
                        color = ColorProvider(0xFFe2e8f0.toInt()),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = sub,
                    style = TextStyle(color = ColorProvider(0xFF64748b.toInt()), fontSize = 9.sp)
                )
            }
        }
    }

    private fun fmt(n: Long): String = when {
        n >= 1_000_000 -> "${"%.1f".format(n / 1_000_000.0)}M"
        n >= 1_000 -> "${"%.1f".format(n / 1_000.0)}K"
        else -> n.toString()
    }

    companion object {
        const val BUILD_DEFAULT_URL = "https://creation-subprime-underwear.ngrok-free.dev"
    }
}

class TokenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TokenWidget()
}
