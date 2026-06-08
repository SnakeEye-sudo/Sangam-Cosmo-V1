package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BrowserScreen
import com.example.ui.OnboardingScreen
import com.example.ui.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.isDarkThemeBasedOnKolkataTime
import com.example.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val browserViewModel: BrowserViewModel = viewModel()
      val themeMode by browserViewModel.themeMode.collectAsState()
      
      // Calculate theme dynamically depending on override configuration
      val isDark = when (themeMode) {
          "LIGHT" -> false
          "DARK" -> true
          else -> isDarkThemeBasedOnKolkataTime()
      }

      val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
      ) { _ -> }

      LaunchedEffect(Unit) {
        permissionLauncher.launch(
          arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
          )
        )
      }

      MyApplicationTheme(darkTheme = isDark) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          var showSplash by remember { mutableStateOf(true) }
          val onboardingCompleted by browserViewModel.onboardingCompleted.collectAsState()

          Crossfade(
            targetState = showSplash,
            animationSpec = tween(600),
            label = "SplashToMainTransition"
          ) { splashActive ->
            if (splashActive) {
                SplashScreen(onFinished = { showSplash = false })
            } else {
                if (!onboardingCompleted) {
                    OnboardingScreen(
                        viewModel = browserViewModel,
                        onFinished = { browserViewModel.completeOnboarding() }
                    )
                } else {
                    BrowserScreen(viewModel = browserViewModel)
                }
            }
          }
        }
      }
    }
  }
}
