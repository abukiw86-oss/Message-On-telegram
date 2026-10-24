package com.example.messageontelegram.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.messageontelegram.core.preferences.PreferencesManager
import com.example.messageontelegram.worker.SyncWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Device reboot detected. Checking pending SMS queue...")
            val prefs = PreferencesManager.getInstance(context)
            if (prefs.isPaired && prefs.isForwardingEnabled) {
                SyncWorker.enqueuePeriodic(context)
                SyncWorker.enqueueImmediate(context)
            }
        }
    }
}
