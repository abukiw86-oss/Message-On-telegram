package com.msgrelay.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.msgrelay.core.database.LocalDatabase
import com.msgrelay.core.network.RelayApiClient
import com.msgrelay.core.preferences.PreferencesManager
import com.msgrelay.worker.SyncForegroundService
import com.msgrelay.worker.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private const val CHANNEL_ID = "sms_forwarded_notifications"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val prefs = PreferencesManager.getInstance(context)
        if (!prefs.isPaired || !prefs.isForwardingEnabled) {
            Log.d(TAG, "SMS received but forwarding is disabled or device is not paired.")
            return
        }

        val messages: Array<SmsMessage>? = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        val simSlot = detectSimSlot(context, intent)

        // Group message parts by originating address (sender) to assemble multi-part SMS
        val groupedMessages = mutableMapOf<String, StringBuilder>()
        var timestamp = System.currentTimeMillis()

        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: "Unknown Sender"
            val bodyPart = sms.displayMessageBody ?: ""
            timestamp = sms.timestampMillis

            val currentBody = groupedMessages.getOrPut(sender) { StringBuilder() }
            currentBody.append(bodyPart)
        }

        val db = LocalDatabase.getInstance(context)
        val pendingIds = mutableListOf<Long>()

        for ((sender, bodyBuilder) in groupedMessages) {
            val fullBody = bodyBuilder.toString()
            val id = db.insertMessage(sender, fullBody, timestamp, simSlot)
            pendingIds.add(id)
            Log.i(TAG, "Recorded incoming SMS from $sender (id: $id, sim: $simSlot)")
        }

        // Attempt immediate dispatch via coroutine with goAsync()
        val pendingResult = goAsync()
        val serverUrl = prefs.serverUrl
        val token = prefs.deviceToken

        if (token.isNullOrBlank()) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (id in pendingIds) {
                    val msg = db.getPendingMessages().find { it.id == id } ?: continue
                    val result = RelayApiClient.forward(
                        serverUrl = serverUrl,
                        deviceToken = token,
                        sender = msg.sender,
                        body = msg.body,
                        receivedAt = msg.timestamp,
                        simSlot = msg.simSlot
                    )

                    if (result.isSuccess) {
                        db.markStatus(id, "SENT")
                        Log.i(TAG, "SMS id $id forwarded successfully to Telegram!")
                        showRelayedNotification(context, msg.sender)
                    } else {
                        Log.w(TAG, "Immediate dispatch failed for id $id: ${result.exceptionOrNull()?.message}")
                        // Trigger sub-30s real-time foreground sync + Alarm + WorkManager
                        SyncForegroundService.start(context)
                        SyncAlarmReceiver.scheduleNext(context, 15)
                        SyncWorker.enqueueImmediate(context)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in immediate SMS dispatch", e)
                SyncForegroundService.start(context)
                SyncAlarmReceiver.scheduleNext(context, 15)
                SyncWorker.enqueueImmediate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun detectSimSlot(context: Context, intent: Intent): Int? {
        val slotKeys = arrayOf(
            "slot", "simId", "sim_id", "simSlot", "sim_slot",
            "android.telephony.extra.SLOT_INDEX", "phone", "simnum"
        )
        for (key in slotKeys) {
            if (intent.hasExtra(key)) {
                val slot = intent.getIntExtra(key, -1)
                if (slot in 0..3) return slot
            }
        }
        val subId = intent.getIntExtra("subscription", -1)
        if (subId != -1) {
            try {
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val info = subManager?.getActiveSubscriptionInfo(subId)
                if (info != null && info.simSlotIndex >= 0) {
                    return info.simSlotIndex
                }
            } catch (e: Exception) {
                // Ignore security or system exceptions
            }
        }
        return null
    }

    private fun showRelayedNotification(context: Context, sender: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "SMS Forwarding Status",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Confirms when an incoming message is relayed to Telegram"
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("SMS Relayed to Telegram")
                .setContentText("Message from $sender forwarded successfully")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to post notification: ${e.message}")
        }
    }
}
