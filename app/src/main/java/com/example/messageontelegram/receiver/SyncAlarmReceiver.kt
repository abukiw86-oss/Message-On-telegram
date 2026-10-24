package com.example.messageontelegram.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.messageontelegram.core.database.LocalDatabase
import com.example.messageontelegram.core.preferences.PreferencesManager
import com.example.messageontelegram.worker.SyncForegroundService
import com.example.messageontelegram.worker.SyncWorker

class SyncAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SyncAlarmReceiver"
        private const val REQUEST_CODE = 4004

        fun scheduleNext(context: Context, delaySeconds: Int = 15) {
            val db = LocalDatabase.getInstance(context)
            if (!db.hasPendingMessages()) {
                cancel(context)
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, SyncAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val triggerAtMillis = SystemClock.elapsedRealtime() + (delaySeconds * 1000L)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Scheduled real-time sync alarm in $delaySeconds seconds.")
            } catch (e: Exception) {
                Log.w(TAG, "Could not set exact alarm: ${e.message}")
            }
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, SyncAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Canceled sync alarm.")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferencesManager.getInstance(context)
        if (!prefs.isPaired || !prefs.isForwardingEnabled) {
            return
        }

        val db = LocalDatabase.getInstance(context)
        if (db.hasPendingMessages()) {
            Log.i(TAG, "Real-time sync alarm triggered! Flushing queue...")
            SyncForegroundService.start(context)
            SyncWorker.enqueueImmediate(context)
        }
    }
}
