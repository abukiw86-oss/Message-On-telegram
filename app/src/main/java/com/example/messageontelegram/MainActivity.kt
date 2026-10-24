package com.example.messageontelegram

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.messageontelegram.core.preferences.PreferencesManager
import com.example.messageontelegram.presentation.ui.DashboardScreen
import com.example.messageontelegram.presentation.ui.PairingScreen
import com.example.messageontelegram.worker.SyncForegroundService
import com.example.messageontelegram.worker.SyncWorker

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = PreferencesManager.getInstance(this)
        if (prefs.isPaired) {
            SyncForegroundService.start(this)
            SyncWorker.enqueueImmediate(this)
        }
    }

    @Composable
    private fun MainAppContent() {
        val prefs = remember { PreferencesManager.getInstance(this@MainActivity) }
        var isPaired by remember { mutableStateOf(prefs.isPaired) }
        var hasSmsPermission by remember { mutableStateOf(checkSmsPermissions()) }
        var isBatteryIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored()) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            hasSmsPermission = permissions[Manifest.permission.RECEIVE_SMS] == true
        }

        LaunchedEffect(Unit) {
            if (!hasSmsPermission) {
                requestNecessaryPermissions(permissionLauncher)
            }
            if (isPaired) {
                SyncWorker.enqueuePeriodic(this@MainActivity)
                SyncWorker.enqueueImmediate(this@MainActivity)
            }
        }

        if (!isPaired) {
            PairingScreen(
                onPairingSuccess = {
                    isPaired = true
                    SyncWorker.enqueuePeriodic(this@MainActivity)
                    SyncWorker.enqueueImmediate(this@MainActivity)
                    if (!hasSmsPermission) {
                        requestNecessaryPermissions(permissionLauncher)
                    }
                    if (!isBatteryIgnored) {
                        requestIgnoreBatteryOptimization()
                    }
                }
            )
        } else {
            DashboardScreen(
                hasSmsPermission = hasSmsPermission,
                onRequestSmsPermission = {
                    requestNecessaryPermissions(permissionLauncher)
                },
                isBatteryOptimizationIgnored = isBatteryIgnored,
                onRequestBatteryExemption = {
                    requestIgnoreBatteryOptimization()
                    isBatteryIgnored = isBatteryOptimizationIgnored()
                },
                onUnpair = {
                    isPaired = false
                }
            )
        }
    }

    private fun checkSmsPermissions(): Boolean {
        val receiveGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val readGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return receiveGranted && readGranted
    }

    private fun requestNecessaryPermissions(
        launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    ) {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        launcher.launch(permissions.toTypedArray())
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(packageName) ?: true
        }
        return true
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(fallbackIntent)
                } catch (e2: Exception) {
                    // Ignore
                }
            }
        }
    }
}
