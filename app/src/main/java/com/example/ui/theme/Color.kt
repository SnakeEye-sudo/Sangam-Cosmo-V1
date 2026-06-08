package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Cosmo Sangam - Immersive UI Colors (Brahmand Edition Theme)
val CosmoDarkBackground = Color(0xFF0A0B10)     // Eternal immersive space blackboard (#0A0B10)
val CosmoDarkSurface = Color(0xFF1C1D24)        // High-depth matte slate card (#1C1D24)
val CosmoDarkSurfaceVariant = Color(0xFF16171D) // Nebula translucent glass card (#16171D)
val CosmoGold = Color(0xFFF97316)               // Warm Solar Cosmic Orange Accent (#F97316)
val CosmoSecondary = Color(0xFF6366F1)          // Convergent Holy Indigo Accent (#6366F1)
val CosmoTertiary = Color(0xFF7E22CE)           // Radiant Cosmic Purple Accent (#7E22CE)
val CosmoTextPrimary = Color(0xFFE2E2E6)         // Stellar ash-white text readability (#E2E2E6)
val CosmoTextSecondary = Color(0xFF9CA3AF)       // Deep space stardust grey secondary text (#9CA3AF)
val CosmoBorder = Color(45, 46, 54)             // Thin borders: white with 10% opacity (#2d2e36)
val CosmoGreen = Color(0xFF10B981)              // Verified SSL Secured Emerald Green (#10B981)

// Dynamic Brahmand gradient brush (indigo-600 via-purple-700 to-orange-500)
val CosmoBrahmandGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4F46E5), // indigo-600
        Color(0xFF7E22CE), // purple-700
        Color(0xFFF97316)  // orange-500
    )
)

val CosmoAtmosphereGlow = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF6366F1).copy(alpha = 0.15f), // Glowing Indigo-500
        Color.Transparent
    )
)

