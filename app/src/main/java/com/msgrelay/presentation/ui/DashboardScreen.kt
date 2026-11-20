package com.msgrelay.presentation.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.msgrelay.core.database.LocalDatabase
import com.msgrelay.core.database.LocalMessage
import com.msgrelay.core.preferences.PreferencesManager
import com.msgrelay.worker.SyncForegroundService
import com.msgrelay.worker.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// --- THEME COLORS ---
private val StatusDeliveredGreen = Color(0xFF00C853)
private val StatusDeliveredContainer = Color(0xFFE8F5E9)
private val StatusPendingAmber = Color(0xFFFF9100)
private val StatusPendingContainer = Color(0xFFFFF3E0)
private val StatusFailedRed = Color(0xFFD50000)
private val StatusFailedContainer = Color(0xFFFFEBEE)

private val BrandDarkTeal = Color(0xFF00363A)
private val BrandTealPrimary = Color(0xFF00695C)
private val BrandCyanGlow = Color(0xFF00B0FF)
private val CardBorderColor = Color(0xFFB2DFDB).copy(alpha = 0.5f)

private enum class MessageTab(val label: String) {
    ALL("All"),
    RELAYED("Relayed"),
    UNRELAYED("Unrelayed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    isBatteryOptimizationIgnored: Boolean = true,
    onRequestBatteryExemption: () -> Unit = {},
    onUnpair: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    val db = remember { LocalDatabase.getInstance(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    // State
    var selectedTab by remember { mutableStateOf(MessageTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var isForwardingActive by remember { mutableStateOf(prefs.isForwardingEnabled) }
    var showMenu by remember { mutableStateOf(false) }
    var showUnpairDialog by remember { mutableStateOf(false) }
    var showClearRelayedDialog by remember { mutableStateOf(false) }
    var selectedMessageForDetail by remember { mutableStateOf<LocalMessage?>(null) }

    // Message Lists
    var relayedMessages by remember { mutableStateOf<List<LocalMessage>>(emptyList()) }
    var unrelayedMessages by remember { mutableStateOf<List<LocalMessage>>(emptyList()) }

    // Database Loader
    suspend fun refreshMessages() {
        withContext(Dispatchers.IO) {
            val relayed = db.getRelayedMessages(200)
            val unrelayed = db.getUnrelayedMessages(200)
            withContext(Dispatchers.Main) {
                relayedMessages = relayed
                unrelayedMessages = unrelayed
            }
        }
    }

    // Manual / Automatic Sync trigger
    fun triggerSync() {
        if (isSyncing) return
        isSyncing = true
        scope.launch {
            SyncWorker.enqueueImmediate(context)
            SyncForegroundService.start(context)
            delay(1200)
            refreshMessages()
            isSyncing = false
            snackbarHostState.showSnackbar("Sync triggered")
        }
    }

    // Auto-refresh periodically (every 4 seconds) to pick up incoming SMS or completed deliveries
    LaunchedEffect(Unit) {
        refreshMessages()
        while (isActive) {
            delay(4000)
            refreshMessages()
        }
    }

    // Rotating animation for sync icon
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    // Filtered Messages based on search & tab
    val displayedMessages = remember(selectedTab, searchQuery, relayedMessages, unrelayedMessages) {
        val baseList = when (selectedTab) {
            MessageTab.ALL -> (unrelayedMessages + relayedMessages).sortedByDescending { it.timestamp }
            MessageTab.RELAYED -> relayedMessages
            MessageTab.UNRELAYED -> unrelayedMessages
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val query = searchQuery.trim().lowercase(Locale.getDefault())
            baseList.filter {
                it.sender.lowercase(Locale.getDefault()).contains(query) ||
                        it.body.lowercase(Locale.getDefault()).contains(query)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isForwardingActive) StatusDeliveredGreen else Color.Gray,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MsgRelay Dashboard",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (isForwardingActive) "Active & Forwarding" else "Forwarding Paused",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(
                        onClick = { triggerSync() },
                        enabled = !isSyncing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Messages",
                            modifier = if (isSyncing) Modifier.rotate(rotationAngle) else Modifier
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(if (isForwardingActive) "Pause Forwarding" else "Resume Forwarding")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isForwardingActive) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                val newState = !isForwardingActive
                                isForwardingActive = newState
                                prefs.isForwardingEnabled = newState
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (newState) "SMS Forwarding enabled" else "SMS Forwarding paused"
                                    )
                                }
                            }
                        )
                        if (unrelayedMessages.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Retry All Unrelayed (${unrelayedMessages.size})") },
                                leadingIcon = {
                                    Icon(Icons.Default.Replay, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            db.resetAllFailedToPending()
                                        }
                                        triggerSync()
                                    }
                                }
                            )
                        }
                        if (relayedMessages.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Clear Delivered History") },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    showClearRelayedDialog = true
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Unpair Device", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Default.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                showMenu = false
                                showUnpairDialog = true
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth > 600.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // SYSTEM WARNING BANNERS
                if (!hasSmsPermission) {
                    PermissionWarningBanner(
                        title = "SMS Permission Required",
                        description = "App cannot receive or relay incoming text messages without SMS permission.",
                        actionLabel = "Grant",
                        onAction = onRequestSmsPermission
                    )
                }

                if (!isBatteryOptimizationIgnored) {
                    PermissionWarningBanner(
                        title = "Battery Optimization Active",
                        description = "Allow background execution so messages relay instantly when screen is off.",
                        actionLabel = "Fix",
                        onAction = onRequestBatteryExemption
                    )
                }

                // RESPONSIVE SUMMARY CARDS & TELEGRAM INFO
                SummarySection(
                    isWideScreen = isWideScreen,
                    relayedCount = relayedMessages.size,
                    unrelayedCount = unrelayedMessages.size,
                    telegramUsername = prefs.telegramUsername ?: prefs.telegramFirstName ?: "Connected",
                    deviceName = prefs.deviceName,
                    isForwardingActive = isForwardingActive,
                    onRetryAllUnrelayed = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.resetAllFailedToPending()
                            }
                            triggerSync()
                        }
                    }
                )

                // SEARCH & FILTER BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search sender or message content...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // TABS FOR ALL / RELAYED / UNRELAYED
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MessageTab.values().forEach { tab ->
                        val count = when (tab) {
                            MessageTab.ALL -> relayedMessages.size + unrelayedMessages.size
                            MessageTab.RELAYED -> relayedMessages.size
                            MessageTab.UNRELAYED -> unrelayedMessages.size
                        }

                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = tab.label,
                                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(
                                        containerColor = when (tab) {
                                            MessageTab.RELAYED -> StatusDeliveredGreen
                                            MessageTab.UNRELAYED -> if (count > 0) StatusPendingAmber else MaterialTheme.colorScheme.surfaceVariant
                                            MessageTab.ALL -> MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        contentColor = when (tab) {
                                            MessageTab.RELAYED -> Color.White
                                            MessageTab.UNRELAYED -> if (count > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            MessageTab.ALL -> MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                    ) {
                                        Text(count.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        )
                    }
                }

                // MESSAGE LIST
                if (displayedMessages.isEmpty()) {
                    EmptyMessageState(
                        tab = selectedTab,
                        searchQuery = searchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayedMessages, key = { it.id }) { message ->
                            MessageItemCard(
                                message = message,
                                onClick = { selectedMessageForDetail = message },
                                onRetry = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            db.retrySingleMessage(message.id)
                                        }
                                        triggerSync()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // MESSAGE DETAIL DIALOG
    selectedMessageForDetail?.let { msg ->
        MessageDetailDialog(
            message = msg,
            onDismiss = { selectedMessageForDetail = null },
            onRetry = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        db.retrySingleMessage(msg.id)
                    }
                    selectedMessageForDetail = null
                    triggerSync()
                }
            },
            onDelete = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        db.deleteMessage(msg.id)
                    }
                    selectedMessageForDetail = null
                    refreshMessages()
                    snackbarHostState.showSnackbar("Message deleted")
                }
            }
        )
    }

    // UNPAIR CONFIRMATION DIALOG
    if (showUnpairDialog) {
        AlertDialog(
            onDismissRequest = { showUnpairDialog = false },
            title = { Text("Unpair Device?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will disconnect this device from your Telegram account and stop forwarding messages.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnpairDialog = false
                        prefs.clearPairing()
                        SyncForegroundService.stop(context)
                        onUnpair()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Unpair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpairDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // CLEAR RELAYED HISTORY DIALOG
    if (showClearRelayedDialog) {
        AlertDialog(
            onDismissRequest = { showClearRelayedDialog = false },
            title = { Text("Clear Relayed History?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will remove all already relayed messages from this device's local database. Unrelayed messages will remain untouched.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearRelayedDialog = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.clearRelayedMessages()
                            }
                            refreshMessages()
                            snackbarHostState.showSnackbar("Delivered messages history cleared")
                        }
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearRelayedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// RESPONSIVE SUMMARY METRIC CARDS & TELEGRAM STATUS
// -------------------------------------------------------------
@Composable
private fun SummarySection(
    isWideScreen: Boolean,
    relayedCount: Int,
    unrelayedCount: Int,
    telegramUsername: String,
    deviceName: String,
    isForwardingActive: Boolean,
    onRetryAllUnrelayed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // TELEGRAM STATUS CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Telegram: @$telegramUsername",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Device: $deviceName",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isForwardingActive) StatusDeliveredGreen.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, if (isForwardingActive) StatusDeliveredGreen else Color.Gray)
                ) {
                    Text(
                        text = if (isForwardingActive) "Active" else "Paused",
                        color = if (isForwardingActive) StatusDeliveredGreen else Color.DarkGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // METRICS ROW: Relayed & Unrelayed
        if (isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Relayed Messages",
                    count = relayedCount,
                    icon = Icons.Default.CheckCircle,
                    accentColor = StatusDeliveredGreen,
                    bgColor = StatusDeliveredContainer
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Unrelayed Messages",
                    count = unrelayedCount,
                    icon = Icons.Default.Schedule,
                    accentColor = if (unrelayedCount > 0) StatusPendingAmber else Color.Gray,
                    bgColor = if (unrelayedCount > 0) StatusPendingContainer else MaterialTheme.colorScheme.surfaceVariant,
                    actionButton = if (unrelayedCount > 0) {
                        {
                            TextButton(
                                onClick = onRetryAllUnrelayed,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else null
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Relayed",
                    count = relayedCount,
                    icon = Icons.Default.CheckCircle,
                    accentColor = StatusDeliveredGreen,
                    bgColor = StatusDeliveredContainer
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Unrelayed",
                    count = unrelayedCount,
                    icon = Icons.Default.Schedule,
                    accentColor = if (unrelayedCount > 0) StatusPendingAmber else Color.Gray,
                    bgColor = if (unrelayedCount > 0) StatusPendingContainer else MaterialTheme.colorScheme.surfaceVariant,
                    actionButton = if (unrelayedCount > 0) {
                        {
                            TextButton(
                                onClick = onRetryAllUnrelayed,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    bgColor: Color,
    actionButton: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                actionButton?.invoke()
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// -------------------------------------------------------------
// MESSAGE CARD ITEM
// -------------------------------------------------------------
@Composable
private fun MessageItemCard(
    message: LocalMessage,
    onClick: () -> Unit,
    onRetry: () -> Unit
) {
    val isRelayed = message.status == "SENT"
    val isPending = message.status == "PENDING"
    val isFailed = message.status == "FAILED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // TOP ROW: SENDER, SIM SLOT, & STATUS CHIP
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRelayed) StatusDeliveredGreen.copy(alpha = 0.15f)
                                else StatusPendingAmber.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = if (isRelayed) StatusDeliveredGreen else StatusPendingAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.sender,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (message.simSlot != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "SIM ${message.simSlot + 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // STATUS CHIP
                StatusBadge(
                    status = message.status,
                    retryCount = message.retryCount
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MESSAGE BODY
            Text(
                text = message.body,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )

            // OPTIONAL ERROR REASON (IF UNRELAYED)
            if (!isRelayed && !message.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StatusFailedContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Error: ${message.errorMessage}",
                        fontSize = 11.sp,
                        color = StatusFailedRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // FOOTER: TIMESTAMP & RETRY ACTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatFriendlyTimestamp(message.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                if (!isRelayed) {
                    FilledTonalButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Retry",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String, retryCount: Int) {
    val (bgColor, textColor, label, icon) = when (status) {
        "SENT" -> Quadruple(
            StatusDeliveredContainer,
            StatusDeliveredGreen,
            "Relayed",
            Icons.Default.CheckCircle
        )
        "FAILED" -> Quadruple(
            StatusFailedContainer,
            StatusFailedRed,
            if (retryCount > 0) "Failed ($retryCount)" else "Failed",
            Icons.Default.ErrorOutline
        )
        else -> Quadruple(
            StatusPendingContainer,
            StatusPendingAmber,
            if (retryCount > 0) "Pending ($retryCount)" else "Pending",
            Icons.Default.Schedule
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

// -------------------------------------------------------------
// MESSAGE DETAIL DIALOG
// -------------------------------------------------------------
@Composable
private fun MessageDetailDialog(
    message: LocalMessage,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isRelayed = message.status == "SENT"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Message Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                StatusBadge(status = message.status, retryCount = message.retryCount)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "From: ${message.sender}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                if (message.simSlot != null) {
                    Text(
                        text = "Received on: SIM ${message.simSlot + 1}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Received at: ${formatFullDate(message.timestamp)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!message.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Transmission Error: ${message.errorMessage}",
                        fontSize = 12.sp,
                        color = StatusFailedRed
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Message Content:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.body,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("SMS Message", message.body)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy SMS Content")
                }
            }
        },
        confirmButton = {
            if (!isRelayed) {
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Delivery")
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

// -------------------------------------------------------------
// EMPTY STATES
// -------------------------------------------------------------
@Composable
private fun EmptyMessageState(
    tab: MessageTab,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val title = if (searchQuery.isNotEmpty()) {
        "No matching messages"
    } else {
        when (tab) {
            MessageTab.ALL -> "No messages logged yet"
            MessageTab.RELAYED -> "No relayed messages yet"
            MessageTab.UNRELAYED -> "No pending messages! 🎉"
        }
    }

    val subtitle = if (searchQuery.isNotEmpty()) {
        "Try adjusting your search criteria."
    } else {
        when (tab) {
            MessageTab.ALL -> "Incoming SMS received on this phone will automatically appear and relay here."
            MessageTab.RELAYED -> "Delivered SMS forwarded to your Telegram bot will be listed here."
            MessageTab.UNRELAYED -> "All incoming messages have been successfully dispatched to Telegram."
        }
    }

    val icon = when (tab) {
        MessageTab.UNRELAYED -> Icons.Default.CloudDone
        MessageTab.RELAYED -> Icons.AutoMirrored.Filled.Send
        MessageTab.ALL -> Icons.Default.Inbox
    }

    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -------------------------------------------------------------
// PERMISSION WARNING BANNER
// -------------------------------------------------------------
@Composable
private fun PermissionWarningBanner(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                        lineHeight = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// UTILITY DATE FORMATTERS
// -------------------------------------------------------------
private fun formatFriendlyTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000L -> "Just now"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 24 * 3600_000L -> "${diff / 3600_000L}h ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

private fun formatFullDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
