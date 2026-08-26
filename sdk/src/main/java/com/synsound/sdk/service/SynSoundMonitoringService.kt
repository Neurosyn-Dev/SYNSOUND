package com.synsound.sdk.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.synsound.sdk.R
import com.synsound.sdk.core.SynSoundSDK

/**
 * Headless Foreground Service for 24/7 continuous acoustic intelligence monitoring.
 */
class SynSoundMonitoringService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP) {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceWithNotification()
        startMonitoring()

        return START_STICKY
    }

    private fun startMonitoring() {
        try {
            if (SynSoundSDK.isInitialized()) {
                val sdk = SynSoundSDK.getInstance()
                sdk.startAcousticMonitoring()
            }
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun stopMonitoring() {
        if (SynSoundSDK.isInitialized()) {
            SynSoundSDK.getInstance().stopAcousticMonitoring()
        }
    }

    private fun startForegroundServiceWithNotification() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, SynSoundMonitoringService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.syn_sdk_service_title))
            .setContentText(getString(R.string.syn_sdk_service_content))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.syn_sdk_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.syn_sdk_service_channel_desc)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopMonitoring()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "synsound_acoustic_monitoring"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.synsound.sdk.action.START_MONITORING"
        const val ACTION_STOP = "com.synsound.sdk.action.STOP_MONITORING"

        fun start(context: Context) {
            val intent = Intent(context, SynSoundMonitoringService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SynSoundMonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
