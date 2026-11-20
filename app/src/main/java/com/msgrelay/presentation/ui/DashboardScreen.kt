package com.msgrelay.presentation.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup 
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.msgrelay.core.database.LocalDatabase
import com.msgrelay.core.database.LocalMessage
import com.msgrelay.core.preferences.PreferencesManager
import com.msgrelay.worker.SyncWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    isBatteryOptimizationIgnored: Boolean = true,
    onRequestBatteryExemption: () -> Unit = {},
    onUnpair: () -> Unit, 
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val db = remember { LocalDatabase.getInstance(context) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SyncWorker.enqueueImmediate(context)
    }

    // Intercept hardware back button to navigate WebView history first
    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MsgRelay", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload Page")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { 
            if (!hasSmsPermission) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SMS Permission Missing",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRequestSmsPermission) {
                            Text("Grant", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } 
        }
    }
} 