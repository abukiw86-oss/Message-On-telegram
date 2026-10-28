package com.msgrelay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.msgrelay.core.database.LocalDatabase
import com.msgrelay.core.preferences.PreferencesManager
import com.msgrelay.worker.SyncForegroundService
import com.msgrelay.worker.SyncWorker

class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NetworkChangeReceiver"

        fun isOnline(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                return networkInfo != null && networkInfo.isConnected
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferencesManager.getInstance(context)
        if (!prefs.isPaired || !prefs.isForwardingEnabled) {
            return
        }

        if (isOnline(context)) {
            val db = LocalDatabase.getInstance(context)
            if (db.hasPendingMessages()) {
                Log.i(TAG, "Network restored and pending messages detected. Triggering immediate real-time sync!")
                SyncForegroundService.start(context)
                SyncWorker.enqueueImmediate(context)
            }
        }
    }
}
