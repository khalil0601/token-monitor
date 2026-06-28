package com.tokenmonitor.widget

import com.google.gson.annotations.SerializedName

data class AllUsageResponse(
    val timestamp: String?,
    val anthropic: ProviderData?,
    val openai: ProviderData?,
    val deepseek: DeepSeekData?
)

data class ProviderData(
    val error: String?,
    val provider: String?,
    @SerializedName("total_used") val totalUsed: Long?,
    @SerializedName("monthly_limit") val monthlyLimit: Long?,
    val remaining: Long?,
    @SerializedName("usage_percent") val usagePercent: Double?,
    @SerializedName("estimated_cost_usd") val estimatedCostUsd: Double?
)

data class DeepSeekData(
    val error: String?,
    val provider: String?,
    @SerializedName("is_available") val isAvailable: Boolean?,
    @SerializedName("total_balance_cny") val totalBalanceCny: Double?,
    @SerializedName("topped_up_cny") val toppedUpCny: Double?,
    @SerializedName("granted_cny") val grantedCny: Double?,
    @SerializedName("estimated_tokens") val estimatedTokens: Long?
)
