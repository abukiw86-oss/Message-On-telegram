package com.example.messageontelegram

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.example.messageontelegram.core.database.LocalDatabase
import com.example.messageontelegram.core.preferences.PreferencesManager
import com.example.messageontelegram.worker.SyncForegroundService
import com.example.messageontelegram.worker.SyncWorker

class SmsRelayApp : Application() {

    companion object {
        private const val TAG = "SmsRelayApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing SMS Relay Application...")

        val prefs = PreferencesManager.getInstance(this)
        if (prefs.isPaired) {
            // Schedule guaranteed 15-minute periodic background sync safety net
            SyncWorker.enqueuePeriodic(this)
        }

        // Register dynamic network callback for instant zero-latency recovery when online
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val prefs = PreferencesManager.getInstance(this@SmsRelayApp)
                    if (!prefs.isPaired || !prefs.isForwardingEnabled) return

                    val db = LocalDatabase.getInstance(this@SmsRelayApp)
                    if (db.hasPendingMessages()) {
                        Log.i(TAG, "Network became available! Flushing pending offline SMS queue in real-time...")
                        SyncForegroundService.start(this@SmsRelayApp)
                        SyncWorker.enqueueImmediate(this@SmsRelayApp)
                    }
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register NetworkCallback: ${e.message}")
        }
    }
}
