package com.synsound.sdk.cloud

import com.synsound.sdk.core.SynSoundConfig
import com.synsound.sdk.core.SynSoundResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * HTTP REST client for communicating with SynSound Platform backend.
 */
class SynSoundRestClient(
    private val config: SynSoundConfig,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val audioMediaType = "audio/vnd.wave".toMediaType()

    suspend fun checkHealth(): SynSoundResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${config.environment.baseUrl}/api/v1/health"
            val request = Request.Builder()
                .url(url)
                .get()
                .apply {
                    config.apiKey?.let { addHeader("Authorization", "Bearer $it") }
                }
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    SynSoundResult.Success(true)
                } else {
                    SynSoundResult.Error(response.code, response.message)
                }
            }
        } catch (e: Exception) {
            SynSoundResult.Failure(e)
        }
    }

    suspend fun registerDevice(deviceName: String, platform: String = "Android"): SynSoundResult<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("deviceName", deviceName)
                put("platform", platform)
                put("deviceId", config.deviceIdentifier ?: java.util.UUID.randomUUID().toString())
                put("timestamp", System.currentTimeMillis())
            }.toString()

            val request = Request.Builder()
                .url("${config.environment.baseUrl}/api/v1/devices/register")
                .post(payload.toRequestBody(jsonMediaType))
                .apply {
                    config.apiKey?.let { addHeader("Authorization", "Bearer $it") }
                }
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val deviceToken = json.optString("deviceToken", "registered")
                    SynSoundResult.Success(deviceToken)
                } else {
                    SynSoundResult.Error(response.code, "Registration failed: ${response.message}")
                }
            }
        } catch (e: Exception) {
            SynSoundResult.Failure(e)
        }
    }

    suspend fun uploadAudioSnapshot(
        audioBytes: ByteArray,
        filename: String = "snapshot.wav",
        metadataJson: String? = null
    ): SynSoundResult<String> = withContext(Dispatchers.IO) {
        try {
            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "audioFile",
                    filename,
                    audioBytes.toRequestBody(audioMediaType)
                )

            metadataJson?.let {
                builder.addFormDataPart("metadata", it)
            }

            val request = Request.Builder()
                .url("${config.environment.baseUrl}/api/v1/snapshots/upload")
                .post(builder.build())
                .apply {
                    config.apiKey?.let { addHeader("Authorization", "Bearer $it") }
                }
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    SynSoundResult.Success(body)
                } else {
                    SynSoundResult.Error(response.code, "Upload failed: ${response.message}")
                }
            }
        } catch (e: Exception) {
            SynSoundResult.Failure(e)
        }
    }
}
