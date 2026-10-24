package com.example.messageontelegram.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class PairResult(
    val deviceToken: String,
    val firstName: String?,
    val username: String?
)

data class ForwardResult(
    val status: String,
    val messageId: Long?
)

data class HealthResult(
    val isOnline: Boolean,
    val isBotConfigured: Boolean
)

object RelayApiClient {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun checkHealth(serverUrl: String): Result<HealthResult> = withContext(Dispatchers.IO) {
        try {
            val url = "${serverUrl.removeSuffix("/")}/api/v1/health"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server returned HTTP ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)
                Result.success(
                    HealthResult(
                        isOnline = json.optString("status") == "healthy",
                        isBotConfigured = json.optBoolean("bot_configured", false)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pair(
        serverUrl: String,
        pairingCode: String,
        deviceName: String
    ): Result<PairResult> = withContext(Dispatchers.IO) {
        try {
            val url = "${serverUrl.removeSuffix("/")}/api/v1/pair"
            val payload = JSONObject().apply {
                put("pairing_code", pairingCode.trim())
                put("device_name", deviceName.trim())
            }

            val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)

                if (!response.isSuccessful) {
                    val detail = json.optString("detail", "Pairing failed with status ${response.code}")
                    return@withContext Result.failure(IOException(detail))
                }

                val token = json.getString("device_token")
                val userObj = json.optJSONObject("user")
                val firstName = if (userObj != null && userObj.has("first_name") && !userObj.isNull("first_name")) userObj.getString("first_name") else null
                val username = if (userObj != null && userObj.has("username") && !userObj.isNull("username")) userObj.getString("username") else null

                Result.success(
                    PairResult(
                        deviceToken = token,
                        firstName = firstName,
                        username = username
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forward(
        serverUrl: String,
        deviceToken: String,
        sender: String,
        body: String,
        receivedAt: Long,
        simSlot: Int? = null
    ): Result<ForwardResult> = withContext(Dispatchers.IO) {
        try {
            val url = "${serverUrl.removeSuffix("/")}/api/v1/forward"
            val payload = JSONObject().apply {
                put("sender", sender)
                put("body", body)
                put("received_at", receivedAt)
                if (simSlot != null) {
                    put("sim_slot", simSlot)
                }
            }

            val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $deviceToken")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)

                if (!response.isSuccessful) {
                    val detail = json.optString("detail", "Forwarding failed with HTTP ${response.code}")
                    return@withContext Result.failure(IOException(detail))
                }

                val status = json.optString("status", "delivered")
                val msgId = if (json.has("telegram_message_id")) json.optLong("telegram_message_id") else null

                Result.success(ForwardResult(status, msgId))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
