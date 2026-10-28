package com.msgrelay.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.msgrelay.core.database.LocalDatabase
import com.msgrelay.core.network.RelayApiClient
import com.msgrelay.core.preferences.PreferencesManager
import com.msgrelay.receiver.SyncAlarmReceiver
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME_EXPEDITED = "sync_pending_sms_expedited"
        private const val WORK_NAME_PERIODIC = "sync_pending_sms_periodic"
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "sync_worker_channel" 

        fun enqueueImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    10,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_EXPEDITED,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Enqueued immediate expedited sync worker.")
        }

        fun enqueue(context: Context) {
            enqueueImmediate(context)
        } 
        
        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Enqueued periodic sync worker safety net (every 15 min).")
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(applicationContext)
    }

    private fun createForegroundInfo(context: Context): ForegroundInfo {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Message Sync",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Syncs queued messages with Telegram in the background"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Forwarding to Telegram")
            .setContentText("Syncing queued messages...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager.getInstance(applicationContext)
        val db = LocalDatabase.getInstance(applicationContext)

        if (!prefs.isPaired || !prefs.isForwardingEnabled) {
            return Result.success()
        }

        val token = prefs.deviceToken ?: return Result.success()
        val serverUrl = prefs.serverUrl
        val pendingMessages = db.getPendingMessages()

        if (pendingMessages.isEmpty()) {
            return Result.success()
        }

        Log.i(TAG, "Starting sync for ${pendingMessages.size} pending message(s)...")
        var anyFailed = false

        for (msg in pendingMessages) {
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
                Log.i(TAG, "Queued SMS id ${msg.id} from ${msg.sender} successfully delivered to Telegram!")
            } else {
                db.incrementRetry(msg.id)
                val errorMsg = result.exceptionOrNull()?.message ?: "Network error"
                Log.w(TAG, "Failed delivery attempt for SMS id ${msg.id}: $errorMsg")

                // Only fail permanently if token was revoked or deleted (HTTP 401/403)
                if (errorMsg.contains("HTTP 401") || errorMsg.contains("Device not found") || errorMsg.contains("Invalid device token")) {
                    db.markStatus(msg.id, "FAILED", errorMsg)
                } else {
                    // Keep message in PENDING queue so it is NEVER abandoned!
                    db.markStatus(msg.id, "PENDING", errorMsg)
                    anyFailed = true
                }
            }
        }

        if (anyFailed) {
            SyncAlarmReceiver.scheduleNext(applicationContext, 15)
            SyncForegroundService.start(applicationContext)
            return Result.retry()
        } else {
            SyncAlarmReceiver.cancel(applicationContext)
            return Result.success()
        }
    }
}
