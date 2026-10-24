package com.example.messageontelegram.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.messageontelegram.core.database.LocalDatabase
import com.example.messageontelegram.core.network.RelayApiClient
import com.example.messageontelegram.core.preferences.PreferencesManager
import com.example.messageontelegram.receiver.SyncAlarmReceiver
import kotlinx.coroutines.*

class SyncForegroundService : Service() {

    companion object {
        private const val TAG = "SyncForegroundService"
        private const val NOTIFICATION_ID = 3003
        private const val CHANNEL_ID = "realtime_sync_channel"
        private const val RETRY_INTERVAL_MS = 15_000L // Retry every 15 seconds (under 30s)

        fun start(context: Context) {
            val db = LocalDatabase.getInstance(context)
            if (!db.hasPendingMessages()) {
                Log.d(TAG, "No pending messages to sync.")
                return
            }

            val intent = Intent(context, SyncForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
                Log.i(TAG, "Started real-time sub-30s sync foreground service.")
            } catch (e: Exception) {
                Log.w(TAG, "Could not start foreground service directly: ${e.message}. Falling back to SyncWorker/Alarm.")
                SyncWorker.enqueueImmediate(context)
                SyncAlarmReceiver.scheduleNext(context, 15)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SyncForegroundService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground("Syncing messages with Telegram in real-time...")
        startSyncLoop()
        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MessageOnTelegram:SyncWakeLock")
            wakeLock?.acquire(3 * 60 * 1000L) // 3-minute max safety timeout
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release wake lock: ${e.message}")
        }
    }

    private fun startInForeground(contentText: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Message on Telegram")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Message on Telegram")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            while (isActive) {
                val db = LocalDatabase.getInstance(this@SyncForegroundService)
                val pending = db.getPendingMessages()

                if (pending.isEmpty()) {
                    Log.i(TAG, "All messages synced successfully! Stopping foreground service.")
                    SyncAlarmReceiver.cancel(this@SyncForegroundService)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                updateNotification("Syncing ${pending.size} message(s) to Telegram (retrying every 15s)...")
                val successCount = processPendingMessages(pending)
                val remaining = db.getPendingMessages()

                if (remaining.isEmpty()) {
                    Log.i(TAG, "Queue empty after dispatch ($successCount sent). Shutting down.")
                    SyncAlarmReceiver.cancel(this@SyncForegroundService)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                // Schedule alarm backup for 20 seconds in case OS suspends process
                SyncAlarmReceiver.scheduleNext(this@SyncForegroundService, 20)

                // Wait 15 seconds before the next retry (under the 30-second target)
                Log.d(TAG, "${remaining.size} message(s) still pending. Retrying in 15 seconds...")
                delay(RETRY_INTERVAL_MS)
            }
        }
    }

    private suspend fun processPendingMessages(pending: List<com.example.messageontelegram.core.database.LocalMessage>): Int {
        val prefs = PreferencesManager.getInstance(this)
        val token = prefs.deviceToken ?: return 0
        val serverUrl = prefs.serverUrl
        val db = LocalDatabase.getInstance(this)
        var count = 0

        for (msg in pending) {
            val result = RelayApiClient.forward(
                serverUrl = serverUrl,
                deviceToken = token,
                sender = msg.sender,
                body = msg.body,
                receivedAt = msg.timestamp,
                simSlot = msg.simSlot
            )

            if (result.isSuccess) {
                db.markStatus(msg.id, "SENT")
                count++
                Log.i(TAG, "Real-time dispatch: SMS id ${msg.id} forwarded successfully!")
            } else {
                db.incrementRetry(msg.id)
                val errorMsg = result.exceptionOrNull()?.message ?: "Network error"
                if (errorMsg.contains("HTTP 401") || errorMsg.contains("Device not found") || errorMsg.contains("Invalid device token")) {
                    db.markStatus(msg.id, "FAILED", errorMsg)
                } else {
                    db.markStatus(msg.id, "PENDING", errorMsg)
                }
            }
        }
        return count
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.i(TAG, "Network became available! Triggering immediate sync without waiting for 15s timer...")
                    // Restart loop immediately so it executes without waiting for delay()
                    startSyncLoop()
                }
            }
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.w(TAG, "Could not register NetworkCallback in service: ${e.message}")
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback!!)
                networkCallback = null
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Real-time Message Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ensures pending offline messages are retried every 15-30 seconds"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
        serviceScope.cancel()
        unregisterNetworkCallback()
        releaseWakeLock()
        Log.i(TAG, "SyncForegroundService destroyed.")
    }
}
