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
    webUrl: String = "https://freenet.unaux.com/" // Use 10.0.2.2 for emulator, or PC LAN IP for physical device
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
            // Optional warning banners displayed cleanly above the WebView
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

            // Fullscreen WebView with back navigation and reload bindings
            AppWebView(
                url = webUrl,
                modifier = Modifier.fillMaxSize(),
                onWebViewCreated = { webView ->
                    webViewInstance = webView
                },
                onCanGoBackChanged = { backAvailable ->
                    canGoBack = backAvailable
                }
            )
        }
    }
}

/**
 * Robust Fullscreen WebView with Reload, Back History, and Error State
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AppWebView(
    url: String,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {},
    onCanGoBackChanged: (Boolean) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = false
                        displayZoomControls = false
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, targetUrl: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, targetUrl, favicon)
                            isLoading = true
                            hasError = false
                            onCanGoBackChanged(view?.canGoBack() == true)
                        }

                        override fun onPageFinished(view: WebView?, targetUrl: String?) {
                            super.onPageFinished(view, targetUrl)
                            isLoading = false
                            onCanGoBackChanged(view?.canGoBack() == true)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            // Only trigger the error card for top-level frame requests
                            if (request?.isForMainFrame == true) {
                                isLoading = false
                                hasError = true
                                errorMessage = error?.description?.toString() ?: "Failed to connect to the server."
                            }
                        }
                    }

                    loadUrl(url)
                    webViewRef = this
                    onWebViewCreated(this)
                }
            },
            update = { webView ->
                webViewRef = webView
                onWebViewCreated(webView)
            }
        )

        // Loading spinner
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Connection error overlay card
        if (hasError) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Connection Refused",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            hasError = false
                            webViewRef?.reload()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Retry Connection")
                    }
                }
            }
        }
    }
}