package com.example.messageontelegram.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messageontelegram.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Clean Telecom Aesthetic Color Palette
val TelecomOceanDeep = Color(0xFF002D3A)
val TelecomDeepTeal = Color(0xFF005973)
val TelecomOceanicCyan = Color(0xFF0A7C93)
val TelecomCyanHighlight = Color(0xFF00A7C5)
val TelecomSkyCyanGlow = Color(0xFF00E5FF)
val TelecomIceWhite = Color(0xFFE0F7FA)
val TelecomFrostedWhite = Color(0xF2FFFFFF)

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Pulse animation for the glowing halo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    // Entrance animation for center logo and text
    val entranceScale = remember { Animatable(0.85f) }
    val entranceAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Entrance animation
        launch {
            entranceScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            )
        }
        launch {
            entranceAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(700, easing = LinearEasing)
            )
        }

        // Display splash for 1.6s then smoothly continue
        delay(1600)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TelecomOceanDeep,
                        TelecomDeepTeal,
                        TelecomOceanicCyan,
                        TelecomDeepTeal,
                        TelecomOceanDeep
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle ambient radial glow in background
        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TelecomCyanHighlight.copy(alpha = pulseAlpha * 0.4f),
                            TelecomOceanicCyan.copy(alpha = pulseAlpha * 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(entranceScale.value)
                .padding(24.dp)
        ) {
            // Logo Container with Pulsing Halo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(190.dp)
            ) {
                // Outer glowing halo ring
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .border(
                            BorderStroke(2.dp, TelecomSkyCyanGlow.copy(alpha = pulseAlpha)),
                            CircleShape
                        )
                )

                // Secondary soft cyan accent ring
                Box(
                    modifier = Modifier
                        .size(155.dp)
                        .clip(CircleShape)
                        .border(
                            BorderStroke(1.5.dp, TelecomCyanHighlight.copy(alpha = 0.5f)),
                            CircleShape
                        )
                )

                // Center Image Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(136.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .border(
                            BorderStroke(2.dp, TelecomIceWhite.copy(alpha = 0.6f)),
                            RoundedCornerShape(32.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Title in Crisp Frosted White
            Text(
                text = "GET 5GB Internet Daily",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.5.sp,
                color = TelecomFrostedWhite,
                fontFamily = FontFamily.SansSerif
            ) 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(TelecomOceanDeep.copy(alpha = 0.55f))
                    .border(
                        BorderStroke(1.dp, TelecomCyanHighlight.copy(alpha = 0.35f)),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                // Pulsing glowing dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TelecomSkyCyanGlow)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Get it Daily",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = TelecomIceWhite
                )
            }
        }
    }
}
