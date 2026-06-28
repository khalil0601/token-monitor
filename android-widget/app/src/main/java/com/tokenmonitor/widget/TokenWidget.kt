package com.tokenmonitor.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import android.content.Intent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.action.Action
import android.app.PendingIntent
import android.app.Activity

class TokenWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        minSize = androidx.glance.unit.DpSize(200.dp, 80.dp),
        maxSize = androidx.glance.unit.DpSize(400.dp, 200.dp),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context, id)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(context: Context, glanceId: GlanceId) {
        val prefs = context.getSharedPreferences("token_widget_prefs", Context.MODE_PRIVATE)
        val apiUrl = prefs.getString("api_url", BUILD_DEFAULT_URL) ?: BUILD_DEFAULT_URL

        // Fetch data
        val data = ApiService.fetchUsage(apiUrl)

        // Calculate summary line
        val summaryLine = buildSummaryLine(data)

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
                        }
                    )
                )
        ) {
            // Header row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡",
                    style = TextStyle(fontSize = 16.sp)
                )
                Text(
                    text = " Token Monitor",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(0xFFe2e8f0.toInt())
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = summaryLine,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(0xFF94a3b8.toInt())
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Provider rows
            if (data != null) {
                // DeepSeek
                data.deepseek?.let { ds ->
                    if (ds.error == null) {
                        ProviderRow(
                            label = "DeepSeek",
                            value = "¥${ds.totalBalanceCny?.let { String.format("%.2f", it) } ?: "--"}",
                            color = 0xFF4f46e5.toInt(),
                            subValue = if (ds.estimatedTokens != null) "${formatNumber(ds.estimatedTokens)} tokens" else null
                        )
                    }
                }

                // Anthropic
                data.anthropic?.let { ant ->
                    if (ant.error == null && ant.remaining != null) {
                        ProviderRow(
                            label = "Anthropic",
                            value = "${if (ant.usagePercent != null) "${(100 - ant.usagePercent).toInt()}%" else "--"}",
                            color = 0xFFd97706.toInt(),
                            subValue = "${formatNumber(ant.remaining)} / ${formatNumber(ant.monthlyLimit ?: 0)}"
                        )
                    }
                }

                // OpenAI
                data.openai?.let { oai ->
                    if (oai.error == null && oai.remaining != null) {
                        ProviderRow(
                            label = "OpenAI",
                            value = "${if (oai.usagePercent != null) "${(100 - oai.usagePercent).toInt()}%" else "--"}",
                            color = 0xFF10a37f.toInt(),
                            subValue = "${formatNumber(oai.remaining)} / ${formatNumber(oai.monthlyLimit ?: 0)}"
                        )
                    }
                }

                if (data.deepseek?.error != null && data.anthropic?.error != null && data.openai?.error != null) {
                    Text(
                        text = "⚠️ 请配置 API Key",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(0xFFf59e0b.toInt())
                        )
                    )
                }
            } else {
                Text(
                    text = "⏳ 加载中...",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(0xFF64748b.toInt())
                    )
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ProviderRow(
        label: String,
        value: String,
        color: Int,
        subValue: String?
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color dot
            Text(
                text = "●",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(color)
                )
            )
            Text(
                text = " $label",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = ColorProvider(0xFF94a3b8.toInt())
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = value,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorProvider(0xFFe2e8f0.toInt())
                    )
                )
                if (subValue != null) {
                    Text(
                        text = subValue,
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = ColorProvider(0xFF64748b.toInt())
                        )
                    )
                }
            }
        }
    }

    private fun buildSummaryLine(data: AllUsageResponse?): String {
        if (data == null) return ""
        val parts = mutableListOf<String>()
        data.deepseek?.let {
            if (it.error == null && it.totalBalanceCny != null) {
                parts.add("DS: ¥${String.format("%.2f", it.totalBalanceCny)}")
            }
        }
        data.anthropic?.let {
            if (it.error == null && it.remaining != null) {
                parts.add("Ant: ${formatNumber(it.remaining)}")
            }
        }
        data.openai?.let {
            if (it.error == null && it.remaining != null) {
                parts.add("OAI: ${formatNumber(it.remaining)}")
            }
        }
        return parts.joinToString("  ")
    }

    private fun formatNumber(n: Long): String {
        return when {
            n >= 1_000_000 -> "${"%.1f".format(n / 1_000_000.0)}M"
            n >= 1_000 -> "${"%.1f".format(n / 1_000.0)}K"
            else -> n.toString()
        }
    }

    companion object {
        const val BUILD_DEFAULT_URL = "https://creation-subprime-underwear.ngrok-free.dev"
    }
}

class TokenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TokenWidget()
}
