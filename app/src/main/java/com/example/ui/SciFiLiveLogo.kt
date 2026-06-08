package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * A highly immersive, high-fidelity sci-fi animated live logo for the Sangam Search application.
 * Utilizes multiple drawing layers, concentric rotating sub-tracks, pulsating reactor cores,
 * and high-contrast atmospheric glow loops.
 */
@Composable
fun SciFiLiveLogo(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 100.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scifi_logo_infinite")

    // CONCENTRIC ROTATIONS (Various speeds & directions for deep sci-fi mechanical complexity)
    val outerRingAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    val innerRingAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotation"
    )

    // CORE PULSATION (Simulating a breathing nuclear fusion or neural core)
    val corePulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // GLOW OSCILLATION
    val glowColorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // QUANTUM PARTICLE POSITION TRANSITIONS (Continuous orbital floating)
    val orbitalPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_phase"
    )

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.01f)),
        contentAlignment = Alignment.Center
    ) {
        // Base Ambient Background Hologram Glow Ring
        Box(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .border(
                    width = 1.dp,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CosmoSecondary.copy(alpha = glowColorAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width / 2

            // 1. OUTER RING: Dashed radar style circle
            val outerRadius = maxRadius * 0.90f
            withTransform({
                rotate(degrees = outerRingAngle, pivot = center)
            }) {
                drawCircle(
                    color = CosmoSecondary.copy(alpha = 0.35f),
                    radius = outerRadius,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(25f, 15f),
                            phase = 0f
                        )
                    )
                )

                // High-visibility node pins on the outer edge
                for (i in 0 until 360 step 60) {
                    val angleRad = Math.toRadians(i.toDouble()).toFloat()
                    val pinX = center.x + outerRadius * cos(angleRad)
                    val pinY = center.y + outerRadius * sin(angleRad)
                    drawCircle(
                        color = CosmoGold.copy(alpha = 0.8f),
                        radius = 2.dp.toPx(),
                        center = Offset(pinX, pinY)
                    )
                }
            }

            // 2. MIDDLE SWEEPER LAYER: Twin solid heavy brackets
            val middleRadius = maxRadius * 0.70f
            withTransform({
                rotate(degrees = innerRingAngle, pivot = center)
            }) {
                drawArc(
                    color = CosmoTertiary.copy(alpha = 0.7f),
                    startAngle = 10f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
                    size = Size(middleRadius * 2, middleRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = CosmoGreen.copy(alpha = 0.7f),
                    startAngle = 190f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
                    size = Size(middleRadius * 2, middleRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. INTERNAL TECH GRID INDEX
            val innerRadius = maxRadius * 0.45f
            drawCircle(
                color = CosmoSecondary.copy(alpha = 0.15f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Dynamic Crosshair gridlines
            drawLine(
                color = CosmoSecondary.copy(alpha = 0.25f),
                start = Offset(center.x - innerRadius - 10f, center.y),
                end = Offset(center.x - innerRadius + 10f, center.y),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = CosmoSecondary.copy(alpha = 0.25f),
                start = Offset(center.x + innerRadius - 10f, center.y),
                end = Offset(center.x + innerRadius + 10f, center.y),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = CosmoSecondary.copy(alpha = 0.25f),
                start = Offset(center.x, center.y - innerRadius - 10f),
                end = Offset(center.x, center.y - innerRadius + 10f),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = CosmoSecondary.copy(alpha = 0.25f),
                start = Offset(center.x, center.y + innerRadius - 10f),
                end = Offset(center.x, center.y + innerRadius + 10f),
                strokeWidth = 1.5.dp.toPx()
            )

            // 4. FLOATING ORBITAL COG-PARTICLES (Orbiting the Core)
            val orbitRadius = maxRadius * 0.55f
            val orbiterCount = 3
            for (i in 0 until orbiterCount) {
                val offsetPhase = (orbitalPhase + (i * 2 * Math.PI / orbiterCount)).toFloat()
                val orbiterX = center.x + orbitRadius * cos(offsetPhase)
                val orbiterY = center.y + orbitRadius * sin(offsetPhase)

                // Neon glowing orbital tracer
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CosmoGold,
                            CosmoGold.copy(alpha = 0.1f)
                        )
                    ),
                    radius = (4 + i * 2).dp.toPx(),
                    center = Offset(orbiterX, orbiterY)
                )
            }

            // 5. THE NUCLEAR PROTO-CORE: Blurry glowing energy nodule
            val centralCoreRadius = maxRadius * 0.28f * corePulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CosmoGold,
                        CosmoSecondary.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = centralCoreRadius * 1.6f
                ),
                radius = centralCoreRadius * 1.5f,
                center = center
            )

            // High Precision geometric central micro-nucleus
            drawCircle(
                color = Color.White,
                radius = (5f * corePulseScale),
                center = center
            )
        }
    }
}

/**
 * A beautiful, highly customizable sci-fi title with sweep/shimmer scan overlays,
 * custom dynamic gradients, and live blinking terminal prompt carets.
 */
@Composable
fun SciFiLiveTitle(
    titleText: String,
    subtitleText: String,
    modifier: Modifier = Modifier,
    titleSizeSp: Int = 22,
    subTextSizeSp: Int = 10,
    subTextColor: Color = CosmoGold
) {
    val infiniteTransition = rememberInfiniteTransition(label = "title_glow_infinite")

    // Ambient letter-spacing or opacity pulse for supreme sci-fi breathing vibe
    val glowOpacity by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_opacity"
    )

    // Bypassing text cursor indicator
    val blinkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caret_blink"
    )
    val showCursor = blinkProgress > 0.5f

    // Scanning light swipe across the text
    val textSweepProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_swipe"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            CosmoTextPrimary,
            CosmoSecondary,
            CosmoGold,
            CosmoTextPrimary
        ),
        start = Offset(x = textSweepProgress * 1000f, y = 0f),
        end = Offset(x = (textSweepProgress + 0.5f) * 1000f, y = 50f)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Holographic Top bar line
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(1.5.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CosmoSecondary.copy(alpha = glowOpacity),
                            CosmoGold.copy(alpha = glowOpacity),
                            Color.Transparent
                        )
                    )
                )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Large Cybernetic Gradient-Shimmer Title
        Text(
            text = titleText,
            fontSize = titleSizeSp.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(brush = shimmerBrush),
            letterSpacing = 2.8.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle text with pulsating scan cursor
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = subtitleText,
                fontSize = subTextSizeSp.sp,
                fontWeight = FontWeight.ExtraBold,
                color = subTextColor.copy(alpha = glowOpacity),
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Blinking computer terminal cursor character
            Text(
                text = if (showCursor) " █" else "  ",
                fontSize = subTextSizeSp.sp,
                fontWeight = FontWeight.Black,
                color = CosmoSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Lower scanner bar boundary
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CosmoTertiary.copy(alpha = glowOpacity),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
