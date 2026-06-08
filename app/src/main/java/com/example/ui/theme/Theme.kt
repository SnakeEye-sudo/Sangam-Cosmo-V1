package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.TimeZone
import androidx.compose.ui.graphics.Color

// Calculate theme dynamically based on Asia/Kolkata timezone (Day = Light, Night = Dark)
fun isDarkThemeBasedOnKolkataTime(): Boolean {
    val timezone = TimeZone.getTimeZone("Asia/Kolkata")
    val calendar = Calendar.getInstance(timezone)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    return hour < 6 || hour >= 18
}

// Retaining old alias function for compatibility
fun isDarkThemeBasedOnTime(): Boolean {
    return isDarkThemeBasedOnKolkataTime()
}

private val CosmoDarkColorScheme = darkColorScheme(
  primary = CosmoSecondary, // Neon convergent indigo (Sangam)
  secondary = CosmoGold, // Warm solar cosmic orange (Sanatan)
  tertiary = CosmoTertiary, // Radiant cosmic purple (Brahmand)
  background = CosmoDarkBackground,
  surface = CosmoDarkSurface,
  surfaceVariant = CosmoDarkSurfaceVariant,
  onPrimary = Color.White,
  onSecondary = Color.White,
  onTertiary = Color.White,
  onBackground = CosmoTextPrimary,
  onSurface = CosmoTextPrimary,
  outline = CosmoBorder
)

private val CosmoLightColorScheme = lightColorScheme(
  primary = CosmoSecondary, 
  secondary = CosmoGold, 
  tertiary = CosmoTertiary,
  background = Color(0xFFF8FAFC),    // Slate 50 premium soft light background
  surface = Color.White,             // Clear white card surfaces
  surfaceVariant = Color(0xFFF1F5F9), // Slate 100 soft surfaces
  onPrimary = Color.White,
  onSecondary = Color.White,
  onTertiary = Color.White,
  onBackground = Color(0xFF0F172A),  // Slate 900 high readability dark text
  onSurface = Color(0xFF0F172A),     // Slate 900 dark text
  outline = Color(0xFFE2E8F0)        // Slate 200 border outlines
)

@Composable
fun SangamCosmoTheme(
  darkTheme: Boolean = isDarkThemeBasedOnKolkataTime(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val baseColorScheme = if (darkTheme) CosmoDarkColorScheme else CosmoLightColorScheme

  // Animate colors smoothly for magnificent transition visuals throughout the app
  val primary = animateColorAsState(targetValue = baseColorScheme.primary, animationSpec = tween(500), label = "PrimaryAnim").value
  val secondary = animateColorAsState(targetValue = baseColorScheme.secondary, animationSpec = tween(500), label = "SecondaryAnim").value
  val tertiary = animateColorAsState(targetValue = baseColorScheme.tertiary, animationSpec = tween(500), label = "TertiaryAnim").value
  val background = animateColorAsState(targetValue = baseColorScheme.background, animationSpec = tween(500), label = "BgAnim").value
  val surface = animateColorAsState(targetValue = baseColorScheme.surface, animationSpec = tween(500), label = "SurfaceAnim").value
  val surfaceVariant = animateColorAsState(targetValue = baseColorScheme.surfaceVariant, animationSpec = tween(500), label = "SurfaceVarAnim").value
  val onPrimary = animateColorAsState(targetValue = baseColorScheme.onPrimary, animationSpec = tween(500), label = "OnPrimaryAnim").value
  val onSecondary = animateColorAsState(targetValue = baseColorScheme.onSecondary, animationSpec = tween(500), label = "OnSecondaryAnim").value
  val onTertiary = animateColorAsState(targetValue = baseColorScheme.onTertiary, animationSpec = tween(500), label = "OnTertiaryAnim").value
  val onBackground = animateColorAsState(targetValue = baseColorScheme.onBackground, animationSpec = tween(500), label = "OnBgAnim").value
  val onSurface = animateColorAsState(targetValue = baseColorScheme.onSurface, animationSpec = tween(500), label = "OnSurfaceAnim").value
  val outline = animateColorAsState(targetValue = baseColorScheme.outline, animationSpec = tween(500), label = "OutlineAnim").value

  val animatedColorScheme = baseColorScheme.copy(
      primary = primary,
      secondary = secondary,
      tertiary = tertiary,
      background = background,
      surface = surface,
      surfaceVariant = surfaceVariant,
      onPrimary = onPrimary,
      onSecondary = onSecondary,
      onTertiary = onTertiary,
      onBackground = onBackground,
      onSurface = onSurface,
      outline = outline
  )

  MaterialTheme(
    colorScheme = animatedColorScheme,
    typography = Typography,
    content = content
  )
}

// Retain alias for existing imports compatibility
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isDarkThemeBasedOnKolkataTime(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  SangamCosmoTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
