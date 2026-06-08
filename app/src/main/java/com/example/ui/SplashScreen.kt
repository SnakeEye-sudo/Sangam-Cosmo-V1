package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("Initializing cosmos connection...") }
    
    // Scale animation for the logo
    val infiniteTransition = rememberInfiniteTransition(label = "SplashGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    LaunchedEffect(Unit) {
        // Dynamic loading stages matching standard high-fidelity apps
        val stages = listOf(
            0.15f to "Awakening quantum nexus engines...",
            0.40f to "Weaving secure browse connections...",
            0.65f to "Establishing local sandbox shields...",
            0.85f to "Synchronizing personal history databases...",
            1.00f to "Welcome to the Sangam Nexus Cosmos..."
        )
        
        for (stage in stages) {
            val targetProgress = stage.first
            val phrase = stage.second
            statusText = phrase
            
            // Increment progress smoothly
            while (progress < targetProgress) {
                progress += 0.05f
                delay(60)
            }
            delay(150)
        }
        delay(300)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmoDarkBackground),
        contentAlignment = Alignment.Center
    ) {
        // High-depth atmospheric glow
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(CosmoAtmosphereGlow)
        )

        // Stardust particle design element using simple decorative circles
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Center Content group
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Highly polished logo container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                ) {
                    // Cosmic dust outer rings
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .background(Color.White.copy(alpha = 0.01f), CircleShape)
                            .border(1.dp, CosmoSecondary.copy(alpha = 0.15f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(102.dp)
                            .background(Color.White.copy(alpha = 0.03f), CircleShape)
                            .border(1.5.dp, CosmoSecondary.copy(alpha = 0.25f), CircleShape)
                    )
                    
                    // Core Brahmand Gradient S-Shield representation matching the app icon
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF04030A), Color(0xFF0C071F), Color(0xFF06040F))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.ic_launcher_foreground),
                            contentDescription = "Sangam Nexus Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Brand Display Typography
                Text(
                    text = "SANGAM NEXUS",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = CosmoTextPrimary,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "U N I V E R S A L   C O S M I C   E D I T I O N",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmoSecondary,
                    letterSpacing = 2.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Linear smooth progress indicator
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = CosmoSecondary,
                        trackColor = Color.Transparent
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Smooth status phrase
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = CosmoTextSecondary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }

            // Bottom trust credentials
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Secured Sandbox logo",
                        tint = CosmoGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "SECURED LOCAL SANDBOX",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmoGreen,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = "Sangam Nexus Core · Version 2.1.0",
                    fontSize = 10.sp,
                    color = CosmoTextSecondary.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
