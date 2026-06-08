package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.*
import com.example.viewmodel.BrowserViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: BrowserViewModel,
    onFinished: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    
    // Step States
    var googleEmailInput by remember { mutableStateOf("krishna.sangam11@gmail.com") }
    var openaiApiKey by remember { mutableStateOf("") }
    var claudeApiKey by remember { mutableStateOf("") }
    
    // Popup Dialogue states for simulated OAuth
    var showGoogleAuthDialog by remember { mutableStateOf(false) }
    var showOpenAiAuthDialog by remember { mutableStateOf(false) }
    var showClaudeAuthDialog by remember { mutableStateOf(false) }
    var showOpenAiGoogleAuthDialog by remember { mutableStateOf(false) }
    var showClaudeGoogleAuthDialog by remember { mutableStateOf(false) }

    val isGoogleConnected by viewModel.isGoogleConnected.collectAsState()
    val googleEmail by viewModel.googleEmail.collectAsState()
    val isOpenAiConnected by viewModel.isOpenAiConnected.collectAsState()
    val isClaudeConnected by viewModel.isClaudeConnected.collectAsState()

    // Smooth background gradient mirroring the cosmos
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF04030A), Color(0xFF0C071F), Color(0xFF06040F))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Global Skip Header row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onFinished,
                    colors = ButtonDefaults.textButtonColors(contentColor = CosmoGold),
                    modifier = Modifier.testTag("skip_all_onboarding_button")
                ) {
                    Text(
                        "Skip Setup & Enter Browser ❯",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                SciFiLiveLogo(
                    sizeDp = 80.dp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                SciFiLiveTitle(
                    titleText = "SANGAM NEXUS",
                    subtitleText = "UNIVERSAL SETUP CONVERGENCE"
                )
                
                // Indicators Row
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepIndicator(stepNumber = 1, activeStep = currentStep, label = "Google")
                    Divider(modifier = Modifier.width(16.dp), color = CosmoBorder)
                    StepIndicator(stepNumber = 2, activeStep = currentStep, label = "OpenAI")
                    Divider(modifier = Modifier.width(16.dp), color = CosmoBorder)
                    StepIndicator(stepNumber = 3, activeStep = currentStep, label = "Claude")
                }
            }

            // Step Content container with animated switches
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width / 2 } + fadeIn() with
                                    slideOutHorizontally { width -> -width / 2 } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width / 2 } + fadeIn() with
                                    slideOutHorizontally { width -> width / 2 } + fadeOut()
                        }.using(SizeTransform(clip = false))
                    }
                ) { step ->
                    val currentGmail = googleEmail ?: googleEmailInput
                    when (step) {
                        1 -> GoogleStepView(
                            isGoogleConnected = isGoogleConnected,
                            connectedEmail = currentGmail,
                            onSignInClicked = { showGoogleAuthDialog = true },
                            onSkipClicked = { currentStep = 2 }
                        )
                        2 -> OpenAiStepView(
                            isOpenAiConnected = isOpenAiConnected,
                            isGoogleConnected = isGoogleConnected,
                            connectedEmail = currentGmail,
                            apiKey = openaiApiKey,
                            onApiKeyChange = { openaiApiKey = it },
                            onConnectKey = { viewModel.connectOpenAi(openaiApiKey) },
                            onOAuthClicked = { showOpenAiAuthDialog = true },
                            onLoginWithGmailClicked = {
                                if (isGoogleConnected) {
                                    viewModel.connectOpenAi("openai_simulated_gmail_token_${currentGmail}")
                                    currentStep = 3
                                } else {
                                    showOpenAiGoogleAuthDialog = true
                                }
                            },
                            onSkipClicked = { currentStep = 3 }
                        )
                        3 -> ClaudeStepView(
                            isClaudeConnected = isClaudeConnected,
                            isGoogleConnected = isGoogleConnected,
                            connectedEmail = currentGmail,
                            apiKey = claudeApiKey,
                            onApiKeyChange = { claudeApiKey = it },
                            onConnectKey = { viewModel.connectClaude(claudeApiKey) },
                            onOAuthClicked = { showClaudeAuthDialog = true },
                            onLoginWithGmailClicked = {
                                if (isGoogleConnected) {
                                    viewModel.connectClaude("claude_simulated_gmail_token_${currentGmail}")
                                    onFinished()
                                } else {
                                    showClaudeGoogleAuthDialog = true
                                }
                            },
                            onSkipClicked = { onFinished() }
                        )
                    }
                }
            }

            // Navigation Bottom Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Return Back
                if (currentStep > 1) {
                    TextButton(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.textButtonColors(contentColor = CosmoTextSecondary)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                // Advance forward
                if (currentStep == 1) {
                    Button(
                        onClick = { currentStep = 2 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CosmoSecondary,
                            disabledContainerColor = CosmoBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isGoogleConnected) "Continue" else "Continue without Google",
                            color = CosmoDarkBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next")
                    }
                } else if (currentStep == 2) {
                    Button(
                        onClick = { currentStep = 3 },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Go to Step 3", color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next")
                    }
                } else {
                    Button(
                        onClick = { onFinished() },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Launch Sangam Nexus", color = CosmoDarkBackground, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Finished")
                    }
                }
            }
        }

        // --- Simulated AUTH Dialogue Overlays ---
        if (showGoogleAuthDialog) {
            SimulatedOAuthDialog(
                title = "Sign in with Google",
                description = "Activate Google Environment Sync, Drive backups, and real-time Gemini AI integration features safely.",
                suggestedUser = googleEmailInput,
                onUserChange = { googleEmailInput = it },
                onAuthorize = {
                    viewModel.connectGoogle(googleEmailInput)
                    showGoogleAuthDialog = false
                    currentStep = 2 // Auto advance to Step 2!
                },
                onDismiss = { showGoogleAuthDialog = false }
            )
        }

        if (showOpenAiAuthDialog) {
            SimulatedOAuthDialog(
                title = "OpenAI Secure Connection",
                description = "Authenticate securely with your OpenAI account to unlock local ChatGPT assistants, summarizers and table researchers.",
                suggestedUser = "openai_account_holder@gmail.com",
                onAuthorize = {
                    viewModel.connectOpenAi("openai_simulated_token_" + java.util.UUID.randomUUID().toString())
                    showOpenAiAuthDialog = false
                    currentStep = 3 // Auto advance to Step 3!
                },
                onDismiss = { showOpenAiAuthDialog = false }
            )
        }

        if (showClaudeAuthDialog) {
            SimulatedOAuthDialog(
                title = "Claude Anthropic OAuth Sync",
                description = "Securely verify your Anthropic account details with local Keystore tokens to unleash high context document searches.",
                suggestedUser = "anthropic_developer@gmail.com",
                onAuthorize = {
                    viewModel.connectClaude("claude_simulated_token_" + java.util.UUID.randomUUID().toString())
                    showClaudeAuthDialog = false
                    onFinished() // Complete onboarding!
                },
                onDismiss = { showClaudeAuthDialog = false }
            )
        }

        if (showOpenAiGoogleAuthDialog) {
            SimulatedOAuthDialog(
                title = "Sign in to OpenAI with Google",
                description = "Login securely to ChatGPT features using your Gmail address and Google profile.",
                suggestedUser = googleEmail ?: googleEmailInput,
                onUserChange = { googleEmailInput = it },
                onAuthorize = {
                    if (!isGoogleConnected) {
                        viewModel.connectGoogle(googleEmailInput)
                    }
                    viewModel.connectOpenAi("openai_simulated_gmail_token_${googleEmailInput}")
                    showOpenAiGoogleAuthDialog = false
                    currentStep = 3
                },
                onDismiss = { showOpenAiGoogleAuthDialog = false }
            )
        }

        if (showClaudeGoogleAuthDialog) {
            SimulatedOAuthDialog(
                title = "Sign in to Claude with Google",
                description = "Login securely to Anthropic features using your Gmail address and Google profile.",
                suggestedUser = googleEmail ?: googleEmailInput,
                onUserChange = { googleEmailInput = it },
                onAuthorize = {
                    if (!isGoogleConnected) {
                        viewModel.connectGoogle(googleEmailInput)
                    }
                    viewModel.connectClaude("claude_simulated_gmail_token_${googleEmailInput}")
                    showClaudeGoogleAuthDialog = false
                    onFinished()
                },
                onDismiss = { showClaudeGoogleAuthDialog = false }
            )
        }
    }
}

@Composable
fun StepIndicator(stepNumber: Int, activeStep: Int, label: String) {
    val isActive = stepNumber == activeStep
    val isCompleted = activeStep > stepNumber
    
    val bgColor = when {
        isActive -> CosmoSecondary
        isCompleted -> CosmoGreen
        else -> CosmoBorder
    }
    
    val textColor = if (isActive || isCompleted) CosmoDarkBackground else CosmoTextSecondary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(bgColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "", tint = CosmoDarkBackground, modifier = Modifier.size(11.dp))
            } else {
                Text(text = stepNumber.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
fun GoogleStepView(
    isGoogleConnected: Boolean,
    connectedEmail: String,
    onSignInClicked: () -> Unit,
    onSkipClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CosmoBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "",
                tint = CosmoSecondary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "STEP 1: Google Sign-In",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CosmoTextPrimary
            )
            Text(
                text = "Secure Google Account authentication is required to activate and protect your environment sync backups.",
                fontSize = 12.sp,
                color = CosmoTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Unlocked Items Visual checklist
            UnlockFeatureItem(text = "Activate Cloud Sync Services")
            UnlockFeatureItem(text = "Encrypted Google Drive Backups")
            UnlockFeatureItem(text = "Google Gemini Core Agent API")
            UnlockFeatureItem(text = "Secure Passcode Account Recovery")

            Spacer(modifier = Modifier.height(30.dp))

            if (isGoogleConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmoGreen.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, CosmoGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "", tint = CosmoGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Google Environment Active", color = CosmoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(connectedEmail, color = CosmoGreen, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Button(
                    onClick = onSignInClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(45, 46, 54))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Login, contentDescription = "", tint = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sign in with Google Account", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(
                onClick = onSkipClicked,
                modifier = Modifier.testTag("skip_google_step_button")
            ) {
                Text("Skip Google Step", color = CosmoTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OpenAiStepView(
    isOpenAiConnected: Boolean,
    isGoogleConnected: Boolean,
    connectedEmail: String,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onConnectKey: () -> Unit,
    onOAuthClicked: () -> Unit,
    onLoginWithGmailClicked: () -> Unit,
    onSkipClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CosmoBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "",
                tint = CosmoGold,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "STEP 2: OpenAI Assistant (Optional)",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CosmoTextPrimary
            )
            Text(
                text = "Connect your OpenAI Account or input your personal ChatGPT API Key to unlock advanced models.",
                fontSize = 12.sp,
                color = CosmoTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            UnlockFeatureItem(text = "Enables Universal ChatGPT Agent")
            UnlockFeatureItem(text = "Executive Summaries & Code Generators")
            UnlockFeatureItem(text = "Assist Form Filling suggestion parameters")

            Spacer(modifier = Modifier.height(24.dp))

            if (isOpenAiConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmoGreen.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, CosmoGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "", tint = CosmoGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("OpenAI Agent Service Unlocked", color = CosmoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Active via Secure Keystore Token", color = CosmoTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                // Main Google/Gmail login integration button
                Button(
                    onClick = onLoginWithGmailClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)), // Google Brand Red
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MailOutline, contentDescription = "", tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isGoogleConnected && connectedEmail.isNotBlank()) "Login with connected Gmail ($connectedEmail)" else "Login with Google/Gmail Account",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "OR CONNECT VIA DEVPARTNER KEYS",
                    fontSize = 9.sp,
                    color = CosmoSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    placeholder = { Text("sk-...", color = CosmoTextSecondary.copy(alpha = 0.5f)) },
                    label = { Text("Enter OpenAI API Key", color = CosmoSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CosmoTextPrimary,
                        unfocusedTextColor = CosmoTextPrimary,
                        focusedBorderColor = CosmoSecondary,
                        unfocusedBorderColor = CosmoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onConnectKey,
                        enabled = apiKey.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Key", color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = onOAuthClicked,
                        border = BorderStroke(1.dp, CosmoBorder),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("OpenAI Login", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(onClick = onSkipClicked) {
                Text("Skip OpenAI Step", color = CosmoTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ClaudeStepView(
    isClaudeConnected: Boolean,
    isGoogleConnected: Boolean,
    connectedEmail: String,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onConnectKey: () -> Unit,
    onOAuthClicked: () -> Unit,
    onLoginWithGmailClicked: () -> Unit,
    onSkipClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CosmoBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "",
                tint = CosmoTertiary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "STEP 3: Claude Anthropic (Optional)",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CosmoTextPrimary
            )
            Text(
                text = "Verify your Anthropic account details or enter your personal Claude developer key.",
                fontSize = 12.sp,
                color = CosmoTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            UnlockFeatureItem(text = "Claude Intelligent Agent layer")
            UnlockFeatureItem(text = "Long context page review structures")
            UnlockFeatureItem(text = "Full document deep analysis capability")

            Spacer(modifier = Modifier.height(24.dp))

            if (isClaudeConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmoGreen.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, CosmoGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "", tint = CosmoGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Claude Agent Service Unlocked", color = CosmoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Active via Secure Keystore Token", color = CosmoTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                // Main Google/Gmail login integration button
                Button(
                    onClick = onLoginWithGmailClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)), // Google Brand Red
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MailOutline, contentDescription = "", tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isGoogleConnected && connectedEmail.isNotBlank()) "Login with connected Gmail ($connectedEmail)" else "Login with Google/Gmail Account",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "OR CONNECT VIA DEVPARTNER KEYS",
                    fontSize = 9.sp,
                    color = CosmoTertiary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    placeholder = { Text("sk-ant-...", color = CosmoTextSecondary.copy(alpha = 0.5f)) },
                    label = { Text("Enter Claude API Key", color = CosmoTertiary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CosmoTextPrimary,
                        unfocusedTextColor = CosmoTextPrimary,
                        focusedBorderColor = CosmoTertiary,
                        unfocusedBorderColor = CosmoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onConnectKey,
                        enabled = apiKey.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoTertiary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Key", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = onOAuthClicked,
                        border = BorderStroke(1.dp, CosmoBorder),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Claude Login", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(onClick = onSkipClicked) {
                Text("Skip Claude Step", color = CosmoTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun UnlockFeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "",
            tint = CosmoGold,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = CosmoTextPrimary, fontSize = 12.sp)
    }
}

@Composable
fun SimulatedOAuthDialog(
    title: String,
    description: String,
    suggestedUser: String = "krishna.sangam11@gmail.com",
    onUserChange: ((String) -> Unit)? = null,
    onAuthorize: () -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf(suggestedUser) }
    var password by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CosmoBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header brand log
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = "", tint = CosmoSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Sangam Nexus OAuth Gateway", style = MaterialTheme.typography.titleSmall, color = CosmoTextPrimary)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = CosmoBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = CosmoTextPrimary
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = CosmoTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        onUserChange?.invoke(it)
                    },
                    label = { Text("Email Address", color = CosmoSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CosmoTextPrimary,
                        unfocusedTextColor = CosmoTextPrimary,
                        focusedBorderColor = CosmoSecondary,
                        unfocusedBorderColor = CosmoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = CosmoSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CosmoTextPrimary,
                        unfocusedTextColor = CosmoTextPrimary,
                        focusedBorderColor = CosmoSecondary,
                        unfocusedBorderColor = CosmoBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, CosmoBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmoTextPrimary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onAuthorize,
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Connect", color = CosmoDarkBackground, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
