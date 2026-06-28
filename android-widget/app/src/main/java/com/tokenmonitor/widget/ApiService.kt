package com.tokenmonitor.widget

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun fetchUsage(apiUrl: String): AllUsageResponse? {
        return withContext(Dispatchers.IO) {
            try {
                // Try api_status.json first (GitHub Pages), then /api/all (server)
                val urls = listOf(
                    if (apiUrl.endsWith("/")) apiUrl + "api_status.json" else "$apiUrl/api_status.json",
                    if (apiUrl.endsWith("/")) apiUrl + "api/all" else "$apiUrl/api/all"
                )
                var lastResponse: okhttp3.Response? = null
                for (url in urls) {
                    val request = Request.Builder().url(url)
                        .header("ngrok-skip-browser-warning", "true")
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) return@withContext gson.fromJson(body, AllUsageResponse::class.java)
                    }
                    lastResponse = response
                }
                null
            } catch (_: Exception) {
                null
            }
        }
    }
}
