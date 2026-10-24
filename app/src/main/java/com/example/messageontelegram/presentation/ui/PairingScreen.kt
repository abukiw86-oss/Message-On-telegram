package com.example.messageontelegram.presentation.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import com.example.messageontelegram.core.network.RelayApiClient
import com.example.messageontelegram.core.preferences.PreferencesManager
import kotlinx.coroutines.launch

// --- PALETTE EXTRACTED FROM THE IMAGE ---[cite: 1]
private val TealDark = Color(0xFF004D61)
private val TealMedium = Color(0xFF027A94)
private val CyanAccent = Color(0xFF00C0DE)
private val IceBlue = Color(0xFFD6F6FF)
private val IceWhite = Color(0xFFF2FBFF)
private val DarkBackground = Color(0xFF002936)
private val GlassCardBg = Color(0xFF01556B).copy(alpha = 0.65f)
private val CardBorderColor = Color(0xFF3CD5F5).copy(alpha = 0.35f)

@Composable
fun PairingScreen(
    hasSmsPermission: Boolean,
    onPermissionGranted: () -> Unit,
    onPairingSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }

    var pairingCode by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf(prefs.deviceName) }
    var serverUrl by remember { mutableStateOf(prefs.serverUrl) }
    var botUsername by remember { mutableStateOf(prefs.botUsername) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
 
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.RECEIVE_SMS] == true ||
                        permissions[Manifest.permission.READ_SMS] == true
        if (isGranted) {
            onPermissionGranted()
        } else {
            // If the user tapped Deny, check if it's a permanent denial
            val activity = context as? Activity
            val canShowAgain = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECEIVE_SMS)
            } ?: true

            // Only redirect to settings if they checked "Don't ask again"
            if (!canShowAgain) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }
    }

    // 2. Direct click handler that always triggers the native system dialog first
    fun requestNativeSmsDialog() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            )
        )
    }

    fun requestSmsOrOpenSettings() {
        val activity = context as? Activity
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECEIVE_SMS)
        } ?: false

        if (!hasSmsPermission && !shouldShowRationale) {
            // Direct system prompt
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                )
            )
        } else {
            // If declined previously or permanently, open app settings directly
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    // Hero Badge floating pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun submitPairing() {
        focusManager.clearFocus()
        if (pairingCode.isBlank() || pairingCode.length < 6) {
            errorMessage = "Please enter all 6 digits of your invitation code."
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            prefs.serverUrl = serverUrl
            prefs.deviceName = deviceName
            prefs.botUsername = botUsername

            val result = RelayApiClient.pair(
                serverUrl = serverUrl,
                pairingCode = pairingCode,
                deviceName = deviceName
            )

            isLoading = false
            if (result.isSuccess) {
                val pairData = result.getOrThrow()
                prefs.savePairing(
                    token = pairData.deviceToken,
                    firstName = pairData.firstName,
                    username = pairData.username
                )
                onPairingSuccess()
            } else {
                errorMessage = result.exceptionOrNull()?.message
                    ?: "Pairing failed. Please verify your code and connection."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(TealMedium, TealDark, DarkBackground)
                )
            )
    ) {
        // Ambient background glowing orb
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-40).dp)
                .background(CyanAccent.copy(alpha = 0.25f), shape = CircleShape)
                .blur(80.dp)
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .then(if (maxWidth > 600.dp) Modifier.width(480.dp) else Modifier.fillMaxWidth())
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Hero Logo Badge[cite: 1]
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(86.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(IceBlue, CyanAccent, TealMedium)
                            ),
                            shape = CircleShape
                        )
                        .border(2.dp, IceWhite.copy(alpha = 0.8f), CircleShape)
                        .shadow(16.dp, spotColor = CyanAccent, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = "Freenet Icon",
                        tint = TealDark,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Freenet",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = IceWhite,
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = "Internet daily for everyone",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = IceBlue.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                )

                // Step 1: Info Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderColor, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(CyanAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Get The Invitation Code",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = IceWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Request a valid token or code from your network host.",
                                fontSize = 12.sp,
                                color = IceBlue.copy(alpha = 0.7f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Step 2: Animated Split PIN Input Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderColor, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enter 6-Digit Code",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = IceWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        BasicTextField(
                            value = pairingCode,
                            onValueChange = {
                                if (it.length <= 6) pairingCode = it.filter { char -> char.isDigit() }
                            },
                            enabled = hasSmsPermission,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submitPairing() }),
                            modifier = Modifier.focusRequester(focusRequester),
                            decorationBox = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0 until 6) {
                                        val char = pairingCode.getOrNull(i)?.toString() ?: ""
                                        val isFocused = pairingCode.length == i

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(0.85f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (char.isNotEmpty()) Color(0xFF003847).copy(alpha = 0.8f)
                                                    else Color(0xFF002F3C).copy(alpha = 0.5f)
                                                )
                                                .border(
                                                    width = if (isFocused) 2.dp else 1.dp,
                                                    color = when {
                                                        isFocused -> CyanAccent
                                                        char.isNotEmpty() -> IceBlue.copy(alpha = 0.7f)
                                                        else -> CardBorderColor.copy(alpha = 0.3f)
                                                    },
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = char,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = IceWhite
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Error Message Animation
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF5A1A28)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF8A80)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFFEBEE),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Submit Action Button[cite: 1]
                val isButtonEnabled = !isLoading && pairingCode.length == 6 && hasSmsPermission
                Button(
                    onClick = { submitPairing() },
                    enabled = isButtonEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(
                            elevation = if (isButtonEnabled) 14.dp else 0.dp,
                            spotColor = CyanAccent,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = if (isButtonEnabled) {
                                    Brush.horizontalGradient(listOf(CyanAccent, IceBlue))
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(CyanAccent.copy(alpha = 0.2f), IceBlue.copy(alpha = 0.2f))
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = TealDark,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isButtonEnabled) TealDark else IceBlue.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verify Code",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isButtonEnabled) TealDark else IceBlue.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Your connection is our priority.",
                    fontSize = 12.sp,
                    color = IceBlue.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } 
        
        if (!hasSmsPermission) {
            SmsPermissionDialog(
                onRequestPermission = { requestNativeSmsDialog() }
            )
        }
    }
}

/**
 * Animated High-Conversion SMS Permission Explanation Dialog
 */
@Composable
fun SmsPermissionDialog(
    onRequestPermission: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "smsPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smsDialogPulse"
    )

    Dialog(
        onDismissRequest = { /* Must grant permission to proceed */ },
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
                .shadow(24.dp, spotColor = CyanAccent, shape = RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(TealMedium.copy(alpha = 0.95f), DarkBackground.copy(alpha = 0.98f))
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulsing Message/OTP Shield Icon[cite: 1]
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
                        imageVector = Icons.Rounded.MarkEmailRead,
                        contentDescription = "SMS Permission",
                        tint = TealDark,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "SMS Permission Required",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = IceWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Freenet requires SMS access to detect carrier verification codes automatically and establish the secure data bridge.",
                    fontSize = 13.sp,
                    color = IceBlue.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Explanatory bullet points
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF003847).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    PermissionReasonRow(text = "Automated OTP verification without manual input")
                    PermissionReasonRow(text = "Validates the connection bridge instantaneously")
                    PermissionReasonRow(text = "Zero storage of private personal text messages")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button[cite: 1]
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(12.dp, spotColor = CyanAccent, shape = RoundedCornerShape(14.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(CyanAccent, IceBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = TealDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Allow SMS Permission",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Click to open prompt or allow in app settings.",
                    fontSize = 11.sp,
                    color = IceBlue.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PermissionReasonRow(text: String) {
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