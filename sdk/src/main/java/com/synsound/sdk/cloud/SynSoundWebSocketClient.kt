package com.synsound.sdk.cloud

import com.synsound.sdk.cloud.models.CloudInferenceResult
import com.synsound.sdk.cloud.models.StreamMetadata
import com.synsound.sdk.cloud.models.TelemetryPayload
import com.synsound.sdk.core.SynSoundConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-throughput WebSocket client for real-time live streaming of audio frames and receiving AI classifications.
 */
class SynSoundWebSocketClient(
    private val config: SynSoundConfig,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()
) {

    interface StreamListener {
        fun onConnected()
        fun onInferenceReceived(result: CloudInferenceResult)
        fun onDisconnected(code: Int, reason: String)
        fun onError(throwable: Throwable)
    }

    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private val listeners = CopyOnWriteArrayList<StreamListener>()

    private val _inferenceFlow = MutableSharedFlow<CloudInferenceResult>(extraBufferCapacity = 32)
    val inferenceFlow: SharedFlow<CloudInferenceResult> = _inferenceFlow.asSharedFlow()

    fun addListener(listener: StreamListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: StreamListener) {
        listeners.remove(listener)
    }

    fun connect(metadata: StreamMetadata? = null) {
        if (isConnected.get()) return

        val request = Request.Builder()
            .url(config.environment.wsUrl)
            .apply {
                config.apiKey?.let { addHeader("Authorization", "Bearer $it") }
            }
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected.set(true)
                metadata?.let {
                    webSocket.send(it.toJson())
                }
                for (l in listeners) {
                    try { l.onConnected() } catch (_: Exception) {}
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                CloudInferenceResult.fromJson(text)?.let { result ->
                    _inferenceFlow.tryEmit(result)
                    for (l in listeners) {
                        try { l.onInferenceReceived(result) } catch (_: Exception) {}
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                for (l in listeners) {
                    try { l.onDisconnected(code, reason) } catch (_: Exception) {}
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected.set(false)
                for (l in listeners) {
                    try { l.onError(t) } catch (_: Exception) {}
                }
            }
        })
    }

    fun sendAudioFrame(pcmData: ByteArray): Boolean {
        val ws = webSocket ?: return false
        if (!isConnected.get()) return false
        return ws.send(pcmData.toByteString())
    }

    fun sendTelemetry(telemetry: TelemetryPayload): Boolean {
        val ws = webSocket ?: return false
        if (!isConnected.get()) return false
        return ws.send(telemetry.toJson())
    }

    fun disconnect() {
        isConnected.set(false)
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }
}
