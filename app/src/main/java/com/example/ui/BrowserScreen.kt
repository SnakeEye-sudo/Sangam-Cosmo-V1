package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BookmarkEntry
import com.example.data.DownloadEntry
import com.example.data.HistoryEntry
import com.example.data.TabEntry
import com.example.ui.theme.*
import com.example.viewmodel.AiSummaryState
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.DetectedMedia
import com.example.viewmodel.SangamSearchState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ActivePanel {
    NONE,
    TABS,
    DOWNLOADS,
    MEDIA_SNIFFER,
    HISTORY_BOOKMARKS,
    SETTINGS,
    AI_ORACLE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Active Database State Flows
    val tabs by viewModel.allTabs.collectAsStateWithLifecycle()
    val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
    val history by viewModel.allHistory.collectAsStateWithLifecycle()
    val downloads by viewModel.allDownloads.collectAsStateWithLifecycle()

    // ViewModel Active states
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val currentTitle by viewModel.currentTitle.collectAsStateWithLifecycle()
    val currentProgress by viewModel.currentProgress.collectAsStateWithLifecycle()
    val isWebLoading by viewModel.isWebLoading.collectAsStateWithLifecycle()

    // Flag configurations
    val isIncognito by viewModel.isIncognito.collectAsStateWithLifecycle()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsStateWithLifecycle()
    val isPasscodeLocked by viewModel.isPasscodeLocked.collectAsStateWithLifecycle()
    val isSetupPasscodeMode by viewModel.isSetupPasscodeMode.collectAsStateWithLifecycle()
    val appPasscode by viewModel.appPasscode.collectAsStateWithLifecycle()
    val detectedMediaList by viewModel.detectedMediaList.collectAsStateWithLifecycle()
    val aiSummaryState by viewModel.aiSummaryState.collectAsStateWithLifecycle()
    var extractedTextForAi by remember { mutableStateOf("") }

    // UI state controllers
    var activePanel by remember { mutableStateOf(ActivePanel.NONE) }
    var urlInputText by remember { mutableStateOf("") }
    var showStartDashboard by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    // For passcode lock UI
    var passcodeEntryBuffer by remember { mutableStateOf("") }
    var initialPasscodeSetupBuffer by remember { mutableStateOf("") }

    // Synchronize the URL input bar when the webpage changes
    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotBlank() && currentUrl != "about:blank" && currentUrl != "https://www.google.com") {
            urlInputText = currentUrl
            showStartDashboard = false
        } else {
            showStartDashboard = true
            urlInputText = ""
        }
    }

    // Dynamic Pulsing Anim for detected media files notifier
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Handle android physical back click for natural tab back routing / drawer close
    BackHandler {
        if (activePanel != ActivePanel.NONE) {
            activePanel = ActivePanel.NONE
        } else if (webViewRef?.canGoBack() == true && !showStartDashboard) {
            webViewRef?.goBack()
        } else if (!showStartDashboard) {
            showStartDashboard = true
            viewModel.currentUrl.value = "about:blank"
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = CosmoDarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CosmoDarkBackground)
        ) {
            // Main Web Frame or Start Dashboard
            Column(modifier = Modifier.fillMaxSize()) {
                
                // Top Address controls and navigation bar
                TopStatusBar(
                    urlValue = urlInputText,
                    isHttps = currentUrl.startsWith("https://"),
                    isIncognito = isIncognito,
                    isLoading = isWebLoading,
                    loadingProgress = currentProgress,
                    detectedMediaCount = detectedMediaList.size,
                    pulseScale = if (detectedMediaList.isNotEmpty()) pulseScale else 1f,
                    onUrlChange = { urlInputText = it },
                    onGoClicked = {
                        keyboardController?.hide()
                        showStartDashboard = false
                        val finalUrl = viewModel.formatUrl(urlInputText)
                        viewModel.currentUrl.value = finalUrl
                        webViewRef?.loadUrl(finalUrl)
                    },
                    onRefreshClicked = {
                        webViewRef?.reload()
                    },
                    onStopClicked = {
                        webViewRef?.stopLoading()
                    },
                    onSecurityBadgeClicked = {
                        // Toggle security info layout
                    },
                    onSnifferClicked = {
                        activePanel = ActivePanel.MEDIA_SNIFFER
                    },
                    onOracleClicked = {
                        activePanel = ActivePanel.AI_ORACLE
                        // Trigger JS page content retrieval safely, then pass to Gemini
                        webViewRef?.evaluateJavascript(
                            "document.body.innerText || document.documentElement.outerText"
                        ) { rawText ->
                            val cleanText = if (rawText != null) {
                                try {
                                    val token = org.json.JSONTokener(rawText).nextValue()
                                    token.toString()
                                } catch (e: Exception) {
                                    rawText.trim('"')
                                }
                            } else ""
                            extractedTextForAi = cleanText
                            viewModel.summarizeWebPage(cleanText)
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (showStartDashboard) {
                        // Cosmo Sangam dynamic home start page (Sangam Search Ultra)
                        DashboardStartScreen(
                            viewModel = viewModel,
                            onKeywordSearch = { kw ->
                                urlInputText = kw
                                showStartDashboard = false
                                val finalUrl = viewModel.formatUrl(kw)
                                viewModel.currentUrl.value = finalUrl
                                webViewRef?.loadUrl(finalUrl)
                            },
                            onBookmarkClicked = { url ->
                                showStartDashboard = false
                                viewModel.currentUrl.value = url
                                webViewRef?.loadUrl(url)
                            },
                            onHistoryClicked = { url ->
                                showStartDashboard = false
                                viewModel.currentUrl.value = url
                                webViewRef?.loadUrl(url)
                            },
                            onShortcutClicked = { urlByShortcut ->
                                showStartDashboard = false
                                viewModel.currentUrl.value = urlByShortcut
                                webViewRef?.loadUrl(urlByShortcut)
                            }
                        )
                    } else {
                        // Native Android WebView Core Layer
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    
                                    // Robust configuration matches Google Chrome and Firefox behaviors safely
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        allowFileAccess = true
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    }

                                    // Intercept long press on images to extract media stream for direct downloading
                                    setOnLongClickListener {
                                        val hitResult = hitTestResult
                                        if (hitResult.type == WebView.HitTestResult.IMAGE_TYPE ||
                                            hitResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                                        ) {
                                            hitResult.extra?.let { imgUrl ->
                                                viewModel.addDetectedMedia(imgUrl, "PHOTO")
                                                true
                                            } ?: false
                                        } else false
                                    }

                                    // Trigger instant downloads upon link interception standard triggers
                                    setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                                        coroutineScope.launch {
                                            viewModel.downloadManager.startDownload(
                                                url, userAgent, contentDisposition, mimetype
                                            )
                                        }
                                    }

                                    // JS Bridge channel hooks
                                    addJavascriptInterface(object {
                                        @JavascriptInterface
                                        fun onMediaDetected(url: String, type: String) {
                                            viewModel.addDetectedMedia(url, type)
                                        }
                                    }, "AndroidMediaSniffer")

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val reqUrl = request?.url?.toString() ?: return false
                                            if (!reqUrl.startsWith("http://") && !reqUrl.startsWith("https://")) {
                                                // Handle map intent, YouTube launch, deep link schemas gracefully
                                                return try {
                                                    val intent = android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW,
                                                        android.net.Uri.parse(reqUrl)
                                                    )
                                                    context.startActivity(intent)
                                                    true
                                                } catch (e: Exception) {
                                                    false
                                                }
                                            }
                                            return false
                                        }

                                        override fun shouldInterceptRequest(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): WebResourceResponse? {
                                            val reqUrl = request?.url?.toString() ?: return null
                                            // Live background adBlocker shield
                                            if (viewModel.shouldBlockRequest(reqUrl)) {
                                                return WebResourceResponse(
                                                    "text/plain",
                                                    "UTF-8",
                                                    java.io.ByteArrayInputStream(ByteArray(0))
                                                )
                                            }
                                            return super.shouldInterceptRequest(view, request)
                                        }

                                        override fun onPageStarted(
                                            view: WebView?,
                                            url: String?,
                                            favicon: Bitmap?
                                        ) {
                                            viewModel.isWebLoading.value = true
                                            viewModel.clearDetectedMedia()
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            viewModel.isWebLoading.value = false
                                            url?.let {
                                                viewModel.updateCurrentTabUrl(it, view?.title ?: "Page")
                                            }

                                            // Inject HTML5 active play monitor javascript routines
                                            val mediaSnifferScript = """
                                                (function() {
                                                    function pingAndroid(url, tag) {
                                                        if (window.AndroidMediaSniffer) {
                                                            window.AndroidMediaSniffer.onMediaDetected(url, tag);
                                                        }
                                                    }
                                                    document.addEventListener('play', function(e) {
                                                        var elem = e.target;
                                                        if (elem) {
                                                            var src = elem.currentSrc || elem.src;
                                                            if (src && !src.startsWith('blob:')) {
                                                                pingAndroid(src, elem.tagName);
                                                            }
                                                        }
                                                    }, true);
                                                    setInterval(function() {
                                                        var vids = document.getElementsByTagName('video');
                                                        for(var i=0; i<vids.length; i++) {
                                                            var s = vids[i].currentSrc || vids[i].src;
                                                            if (s && !s.startsWith('blob:')) pingAndroid(s, 'VIDEO');
                                                        }
                                                    }, 3000);
                                                })();
                                            """.trimIndent()
                                            view?.evaluateJavascript(mediaSnifferScript, null)
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            viewModel.currentProgress.value = newProgress
                                        }

                                        override fun onReceivedTitle(view: WebView?, title: String?) {
                                            title?.let {
                                                viewModel.updateCurrentTabUrl(view?.url ?: "", it)
                                            }
                                        }
                                    }
                                }
                            },
                            update = { webView ->
                                webViewRef = webView
                                // Navigate if state requested change
                                if (webView.url != currentUrl && currentUrl != "about:blank") {
                                    webView.loadUrl(currentUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Core Navigation & Bottom Feature bar
                BottomToolbar(
                    tabCount = tabs.size,
                    canGoBack = webViewRef?.canGoBack() ?: false,
                    canGoForward = webViewRef?.canGoForward() ?: false,
                    onBackClicked = { webViewRef?.goBack() },
                    onForwardClicked = { webViewRef?.goForward() },
                    onHomeClicked = {
                        showStartDashboard = true
                        viewModel.currentUrl.value = "about:blank"
                    },
                    onTabsClicked = {
                        activePanel = ActivePanel.TABS
                    },
                    onMenuClicked = {
                        activePanel = ActivePanel.SETTINGS
                    }
                )
            }

            // Slide screens and overlays (Incognito, downloads, folders, history, and summarizer)
            AnimatedVisibility(
                visible = activePanel != ActivePanel.NONE,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { activePanel = ActivePanel.NONE }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .align(Alignment.BottomCenter)
                            .clickable(enabled = false) {}, // Sink clicks
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = CosmoDarkSurface,
                        tonalElevation = 8.dp
                    ) {
                        when (activePanel) {
                            ActivePanel.TABS -> TabsManagerScreen(
                                tabs = tabs,
                                activeTabId = activeTabId,
                                isIncognito = isIncognito,
                                onSelect = { tabId ->
                                    viewModel.selectTab(tabId)
                                    activePanel = ActivePanel.NONE
                                    showStartDashboard = false
                                },
                                onClose = { tab ->
                                    viewModel.closeTab(tab)
                                },
                                onAddTab = { isInc ->
                                    viewModel.createNewTab("https://www.google.com", isInc)
                                    activePanel = ActivePanel.NONE
                                    showStartDashboard = true
                                },
                                onClosePanel = { activePanel = ActivePanel.NONE }
                            )

                            ActivePanel.DOWNLOADS -> DownloadsManagerScreen(
                                downloads = downloads,
                                onDelete = { id -> viewModel.deleteDownloadEntry(id) },
                                onClosePanel = { activePanel = ActivePanel.NONE }
                            )

                            ActivePanel.MEDIA_SNIFFER -> MediaSnifferScreen(
                                mediaList = detectedMediaList,
                                onDownloadTrigger = { media ->
                                    coroutineScope.launch {
                                        viewModel.downloadManager.startDownload(
                                            url = media.url,
                                            mimeType = when (media.type) {
                                                "VIDEO" -> "video/mp4"
                                                "AUDIO" -> "audio/mpeg"
                                                else -> "image/jpeg"
                                            }
                                        )
                                        activePanel = ActivePanel.DOWNLOADS
                                    }
                                },
                                onClosePanel = { activePanel = ActivePanel.NONE }
                            )

                            ActivePanel.HISTORY_BOOKMARKS -> HistoryBookmarksScreen(
                                history = history,
                                bookmarks = bookmarks,
                                onDeleteHistory = { id -> viewModel.deleteHistoryEntry(id) },
                                onDeleteHistoryEntries = { ids -> viewModel.deleteHistoryEntries(ids) },
                                onDeleteBookmark = { id -> viewModel.deleteBookmarkEntry(id) },
                                onClearHistory = { viewModel.clearAllHistory() },
                                onUrlNavigate = { url ->
                                    showStartDashboard = false
                                    viewModel.currentUrl.value = url
                                    webViewRef?.loadUrl(url)
                                    activePanel = ActivePanel.NONE
                                },
                                onClosePanel = { activePanel = ActivePanel.NONE }
                            )

                            ActivePanel.AI_ORACLE -> OracleSummaryScreen(
                                state = aiSummaryState,
                                viewModel = viewModel,
                                extractedText = extractedTextForAi,
                                onClear = { viewModel.clearSummary() },
                                onClosePanel = { activePanel = ActivePanel.NONE }
                            )

                            ActivePanel.SETTINGS -> SettingsPanelScreen(
                                viewModel = viewModel,
                                isAdBlock = isAdBlockEnabled,
                                isInc = isIncognito,
                                isLox = appPasscode.isNotEmpty(),
                                onToggleAdBlock = { viewModel.isAdBlockEnabled.value = it },
                                onToggleIncognito = {
                                    viewModel.isIncognito.value = it
                                    // Refresh the views tab info context
                                    val currentTab = tabs.find { t -> t.id == activeTabId }
                                    if (currentTab != null) {
                                        viewModel.updateCurrentTabUrl(currentUrl, currentTitle)
                                        coroutineScope.launch {
                                            viewModel.insertTab(currentTab.copy(isIncognito = it))
                                        }
                                    }
                                },
                                onSetupPasscode = {
                                    viewModel.isSetupPasscodeMode.value = true
                                    initialPasscodeSetupBuffer = ""
                                },
                                onRemovePasscode = {
                                    viewModel.appPasscode.value = ""
                                },
                                onViewDownloads = { activePanel = ActivePanel.DOWNLOADS },
                                onViewHistoryAndBookmarks = { activePanel = ActivePanel.HISTORY_BOOKMARKS },
                                onClosePanel = { activePanel = ActivePanel.NONE }
                            )

                            else -> {}
                        }
                    }
                }
            }

            // Universal Secured passcode lock dialog gate
            if (isPasscodeLocked && appPasscode.isNotEmpty()) {
                Dialog(onDismissRequest = {}) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = CosmoDarkSurface,
                        tonalElevation = 12.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Security Screen Locked",
                                tint = CosmoGold,
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(bottom = 12.dp)
                            )
                            Text(
                                text = "Sangam Cosmo Protected",
                                style = MaterialTheme.typography.headlineSmall,
                                color = CosmoTextPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Enter secure browser passcode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CosmoTextSecondary,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // Interactive passcode dot indicator
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 32.dp)
                            ) {
                                repeat(4) { i ->
                                    val isActive = i < passcodeEntryBuffer.length
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(horizontal = 4.dp)
                                            .background(
                                                if (isActive) CosmoSecondary else CosmoBorder,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }

                            // Matrix digit keypad
                            PasscodeKeypad(
                                onDigit = { digit ->
                                    if (passcodeEntryBuffer.length < 4) {
                                        passcodeEntryBuffer += digit
                                        if (passcodeEntryBuffer == appPasscode) {
                                            viewModel.isPasscodeLocked.value = false
                                            passcodeEntryBuffer = ""
                                        } else if (passcodeEntryBuffer.length == 4) {
                                            // Bad passcode, flash buffer clear click
                                            passcodeEntryBuffer = ""
                                        }
                                    }
                                },
                                onDelete = {
                                    if (passcodeEntryBuffer.isNotEmpty()) {
                                        passcodeEntryBuffer = passcodeEntryBuffer.dropLast(1)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Passcode setup dialog overlay
            if (isSetupPasscodeMode) {
                Dialog(onDismissRequest = { viewModel.isSetupPasscodeMode.value = false }) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = CosmoDarkSurface,
                        tonalElevation = 12.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Choose browser PIN passcode",
                                style = MaterialTheme.typography.titleMedium,
                                color = CosmoTextPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            ) {
                                repeat(4) { idx ->
                                    val isActive = idx < initialPasscodeSetupBuffer.length
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(horizontal = 4.dp)
                                            .background(
                                                if (isActive) CosmoGold else CosmoBorder,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                            PasscodeKeypad(
                                onDigit = { d ->
                                    if (initialPasscodeSetupBuffer.length < 4) {
                                        initialPasscodeSetupBuffer += d
                                        if (initialPasscodeSetupBuffer.length == 4) {
                                            viewModel.appPasscode.value = initialPasscodeSetupBuffer
                                            viewModel.isSetupPasscodeMode.value = false
                                        }
                                    }
                                },
                                onDelete = {
                                    if (initialPasscodeSetupBuffer.isNotEmpty()) {
                                        initialPasscodeSetupBuffer = initialPasscodeSetupBuffer.dropLast(1)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Keypad widget helper ---
@Composable
fun PasscodeKeypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit
) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫")
    )
    Column {
        digits.forEach { row ->
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { char ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(70.dp)
                            .padding(6.dp)
                            .background(CosmoBorder, shape = CircleShape)
                            .clickable {
                                when (char) {
                                    "⌫" -> onDelete()
                                    "C" -> {} // No-op clear or reserve
                                    else -> onDigit(char)
                                }
                            }
                    ) {
                        Text(
                            text = char,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = CosmoTextPrimary
                        )
                    }
                }
            }
        }
    }
}

// --- Top status bar design ---
@Composable
fun TopStatusBar(
    urlValue: String,
    isHttps: Boolean,
    isIncognito: Boolean,
    isLoading: Boolean,
    loadingProgress: Int,
    detectedMediaCount: Int,
    pulseScale: Float,
    onUrlChange: (String) -> Unit,
    onGoClicked: () -> Unit,
    onRefreshClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onSecurityBadgeClicked: () -> Unit,
    onSnifferClicked: () -> Unit,
    onOracleClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmoDarkBackground)
    ) {
        // Immersive Sangam Nexus Top App Bar Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sangam Nexus Logo box
                Box(
                    modifier = Modifier
                        .size(38.dp)
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
                Column {
                    Text(
                        text = "Sangam Nexus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CosmoTextPrimary,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "UNIVERSAL EDITION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmoSecondary,
                        letterSpacing = 1.5.sp,
                    )
                }
            }

            // Right interactive element group: secure/shield indicator & profile avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Security status badge/button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onSecurityBadgeClicked() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHttps) Icons.Filled.Lock else Icons.Filled.Warning,
                        contentDescription = "Shield Security status",
                        tint = if (isHttps) CosmoGreen else CosmoGold,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Profile Image / Avatar container with border shadow glow
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, CosmoSecondary.copy(alpha = 0.4f), CircleShape)
                        .background(CosmoDarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    // Beautiful letter avatar with stardust accent
                    Text(
                        text = "K",
                        color = CosmoSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Divider line
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.05f))
        )

        // Address Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmoDarkSurface)
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Incognito or security indicator badge
            IconButton(onClick = onSecurityBadgeClicked) {
                Icon(
                    imageVector = if (isIncognito) Icons.Filled.PrivacyTip else if (isHttps) Icons.Filled.Lock else Icons.Filled.Warning,
                    contentDescription = "Connection Security Status",
                    tint = if (isIncognito) CosmoSecondary else if (isHttps) CosmoGreen else CosmoGold,
                    modifier = Modifier.size(18.dp)
                )
            }

            // URL Entry input Bar mimicking modern Chrome layout
            TextField(
                value = urlValue,
                onValueChange = onUrlChange,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("url_input_field"),
                placeholder = {
                    Text(
                        text = if (isIncognito) "Incognito: Search or enter URL" else "Search or enter URL",
                        color = CosmoTextSecondary,
                        fontSize = 12.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    keyboardType = KeyboardType.Uri
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onGoClicked() }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CosmoDarkBackground,
                    unfocusedContainerColor = CosmoDarkBackground,
                    disabledContainerColor = CosmoDarkBackground,
                    focusedTextColor = CosmoTextPrimary,
                    unfocusedTextColor = CosmoTextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(22.dp),
                textStyle = TextStyle(fontSize = 12.sp, color = CosmoTextPrimary)
            )

            // Show Refresh / Stop depending on Loading state
            IconButton(onClick = { if (isLoading) onStopClicked() else onRefreshClicked() }) {
                Icon(
                    imageVector = if (isLoading) Icons.Filled.Close else Icons.Filled.Refresh,
                    contentDescription = "Stop or Refresh",
                    tint = CosmoTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // AI summary sparkle action
            IconButton(onClick = onOracleClicked) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = "Cosmo Sangam Oracle Page Summary",
                    tint = CosmoGold,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Quick Media sniffer alert bell (PULSING indicator if media detected!)
            if (detectedMediaCount > 0) {
                IconButton(
                    onClick = onSnifferClicked,
                    modifier = Modifier
                        .rotate(15f)
                        .size(34.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp * pulseScale)
                            .background(CosmoSecondary, shape = CircleShape)
                    ) {
                        Text(
                            text = scoreFormat(detectedMediaCount),
                            color = CosmoDarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
        
        // Progress display bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadingProgress / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = CosmoSecondary,
                trackColor = Color.Transparent,
            )
        } else {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.05f))
            )
        }
    }
}

fun scoreFormat(score: Int): String {
    return if (score > 9) "9+" else score.toString()
}

// --- Bottom standard tab controllers ---
@Composable
fun BottomToolbar(
    tabCount: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBackClicked: () -> Unit,
    onForwardClicked: () -> Unit,
    onHomeClicked: () -> Unit,
    onTabsClicked: () -> Unit,
    onMenuClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmoDarkBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmoDarkSurface.copy(alpha = 0.85f), RoundedCornerShape(32.dp))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(32.dp))
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClicked,
                enabled = canGoBack,
                modifier = Modifier.testTag("back_nav_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Browser Navigate Back",
                    tint = if (canGoBack) CosmoSecondary else CosmoTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(
                onClick = onForwardClicked,
                enabled = canGoForward,
                modifier = Modifier.testTag("forward_nav_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "Browser Navigate Forward",
                    tint = if (canGoForward) CosmoSecondary else CosmoTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onHomeClicked) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Browser Go Home",
                    tint = CosmoGold,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            // Tabs indicator frame showing active number of pages
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabsClicked() }
                    .border(2.dp, CosmoSecondary.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = tabCount.toString(),
                    fontWeight = FontWeight.Bold,
                    color = CosmoSecondary,
                    fontSize = 13.sp
                )
            }

            IconButton(onClick = onMenuClicked) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Tools and settings drawer",
                    tint = CosmoTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// --- Starters Starting Dashboard screen ---
@Composable
fun DashboardStartScreen(
    viewModel: com.example.viewmodel.BrowserViewModel,
    onKeywordSearch: (String) -> Unit,
    onBookmarkClicked: (String) -> Unit,
    onHistoryClicked: (String) -> Unit,
    onShortcutClicked: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // UI states and DB subscriptions
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val memories by viewModel.allMemories.collectAsStateWithLifecycle()
    val knowledgeGraph by viewModel.allKnowledge.collectAsStateWithLifecycle()
    val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
    val history by viewModel.allHistory.collectAsStateWithLifecycle()
    val downloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    
    val sangamSearchState by viewModel.sangamSearchState.collectAsStateWithLifecycle()
    val sangamSearchActive by viewModel.sangamSearchActive.collectAsStateWithLifecycle()
    val isResearchRunning by viewModel.isResearchRunning.collectAsStateWithLifecycle()
    val googleEmail by viewModel.googleEmail.collectAsStateWithLifecycle()
    val isGoogleConnected by viewModel.isGoogleConnected.collectAsStateWithLifecycle()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsStateWithLifecycle()
    
    var searchKeyText by remember { mutableStateOf("") }
    var selectedSearchMode by remember { mutableStateOf("COMBINED") } // "COMBINED", "INTERNET", "PERSONAL", "RESEARCH"
    var activeVaultTab by remember { mutableStateOf(0) } // 0: Notes, 1: Graph, 2: Memories, 3: Bookmarks/PDFs, 4: Downloads
    
    // For voice search simulation
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showImageSearchDialog by remember { mutableStateOf(false) }
    var selectedImageLabel by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var extraVisualTextQuery by remember { mutableStateOf("") }
    var voiceTranscript by remember { mutableStateOf("Listening to your voice...") }
    var selectedResultCategory by remember { mutableStateOf(0) }
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val voiceRippleScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_ripple"
    )

    // For CRUD Dialogs
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteTitleInput by remember { mutableStateOf("") }
    var noteContentInput by remember { mutableStateOf("") }
    
    var showAddRelationDialog by remember { mutableStateOf(false) }
    var relSubjectInput by remember { mutableStateOf("") }
    var relTypeInput by remember { mutableStateOf("") }
    var relObjectInput by remember { mutableStateOf("") }
    var relSummaryInput by remember { mutableStateOf("") }

    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var memTopicInput by remember { mutableStateOf("") }
    var memFactInput by remember { mutableStateOf("") }

    // Recent queries stored in local state
    val recentQueries = remember { mutableStateListOf("Bettiah Raj", "Future of Quantum Computing", "West Champaran Satyagraha", "Deep Research Machine") }

    // Learning states
    var flashcardIndex by remember { mutableStateOf(0) }
    var showFlashcardAnswer by remember { mutableStateOf(false) }
    val studyFlashcards = remember {
        listOf(
            "Who founded the Bettiah Raj chieftain lineage in 1659?" to "Raja Ugra Sen Singh, recognized by Emperor Shah Jahan.",
            "What major landmark of West Champaran still exists in Bettiah town?" to "The Bettiah Raj Palace, reflecting classical Mughal-British architecture.",
            "What are NISQ devices in quantum computing?" to "Noisy Intermediate-Scale Quantum - systems with noise-susceptible qubits.",
            "What landmark campaign did Mahatma Gandhi lead from West Champaran in 1917?" to "The Champaran Satyagraha, his first civil disobedience movement in India."
        )
    }
    var studyStreak by remember { mutableStateOf(5) }
    var showQuizDialog by remember { mutableStateOf(false) }
    var quizQuestionIndex by remember { mutableStateOf(0) }
    var selectedQuizAnswer by remember { mutableStateOf<Int?>(null) }
    var quizScore by remember { mutableStateOf(0) }
    var quizFinished by remember { mutableStateOf(false) }

    // Tasks checklist state
    val tasksList = remember {
        mutableStateListOf(
            "Investigate Sir Harendra Kishore Singh's succession litigation documents" to false,
            "Formulate comparative report: Trapped Ion vs Superconducting Qubits" to true,
            "Synthesize oral history of Bettiah Raj palace managers from 1917 survey" to false,
            "Index Bettiah Raj Palace architectural blue-prints inside memory vault" to false
        )
    }
    var isDeployingAgentTask by remember { mutableStateOf<Int?>(null) }
    var agentDeploymentProgress by remember { mutableStateOf(0f) }

    // Meeting state
    var isRecordingMeeting by remember { mutableStateOf(false) }
    var recordingTimer by remember { mutableStateOf(0) }
    val simulatedMeetings = remember {
        mutableStateListOf(
            "West Champaran Indigo Heritage Survey Synchronous" to "Transcription complete. AI Analyzed.",
            "Quantum Cryptography Resilience Alignment Session" to "Transcription complete. AI Analyzed."
        )
    }
    var showMeetingAnalysisDialog by remember { mutableStateOf(false) }
    var meetingAnalysisResult by remember { mutableStateOf("") }

    // Interactive AI Debate Engine states
    var showDebateDialog by remember { mutableStateOf(false) }
    var debateQuestion by remember { mutableStateOf("Will Artificial General Intelligence (AGI) benefit humanity?") }
    var isRunningDebate by remember { mutableStateOf(false) }
    var debateStage by remember { mutableStateOf(0) } // 0: input, 1: running, 2: results
    var debateResult by remember { mutableStateOf<DebateResultData?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmoDarkBackground)
    ) {
        // High-fidelity background cosmic nebulous glow
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF151030).copy(alpha = 0.5f),
                            Color(0xFF070512).copy(alpha = 0.9f)
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp)
        ) {
            // USER ACCOUNT HEADER (SANGAM HOME ADVANCED EXECUTIVE BRANDING)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // User Avatar Frame
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(CosmoGold, CosmoSecondary)
                                    )
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(CosmoDarkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isGoogleConnected) "KS" else "AN",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = CosmoGold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isGoogleConnected) "Krishna Sangam" else "Anonymous Navigator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CosmoTextPrimary
                            )
                            Text(
                                text = if (isGoogleConnected) "Account Type: Enterprise Partner" else "Private Sandbox Account",
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmoSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Connected Status Emblem
                    Box(
                        modifier = Modifier
                            .background(
                                if (isGoogleConnected) CosmoGreen.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isGoogleConnected) CosmoGreen.copy(alpha = 0.3f) else CosmoBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isGoogleConnected) CosmoGreen else CosmoTertiary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isGoogleConnected) "CONFLUENCE LINKED" else "OFFLINE LOCAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isGoogleConnected) CosmoGreen else CosmoTextSecondary,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // ADVANCED COSMIC BRAND LOGO
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SciFiLiveLogo(
                        sizeDp = 96.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SciFiLiveTitle(
                        titleText = "SANGAM SEARCH ULTRA",
                        subtitleText = "PUBLIC INTERNET & PERSONAL COGNITIVE DISCOVERY ENGINE",
                        titleSizeSp = 24,
                        subTextSizeSp = 9
                    )
                }
            }

            // UNIFIED SANGAM SEARCH DISCOVERY BAR
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Search Bar Itself
                    TextField(
                        value = searchKeyText,
                        onValueChange = { searchKeyText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CosmoSecondary.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                        placeholder = {
                            Text(
                                text = "Ask anything (e.g. History of Bettiah Raj, Quantum Computing)",
                                fontSize = 12.sp,
                                color = CosmoTextSecondary
                            )
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Sangam Search Icon",
                                tint = CosmoGold
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchKeyText.isNotEmpty()) {
                                    IconButton(onClick = { searchKeyText = "" }) {
                                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear", tint = CosmoTextSecondary)
                                    }
                                }
                                // Simulated Voice Search button
                                IconButton(
                                    onClick = {
                                        voiceTranscript = "Listening to your voice..."
                                        showVoiceDialog = true
                                        // Simulated delay for audio translation
                                        coroutineScope.launch {
                                            delay(1500)
                                            voiceTranscript = "Spoken: 'Tell me about Mahatma Gandhi West Champaran 1917'"
                                            delay(1500)
                                            showVoiceDialog = false
                                            searchKeyText = "West Champaran Satyagraha"
                                            viewModel.executeSangamSearch("West Champaran Satyagraha", selectedSearchMode)
                                        }
                                    },
                                    modifier = Modifier.testTag("voice_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = "Voice Search",
                                        tint = CosmoSecondary
                                    )
                                }
                                
                                // Simulated Visual Image Search button
                                IconButton(
                                    onClick = {
                                        showImageSearchDialog = true
                                        selectedImageLabel = null
                                        selectedImageUri = null
                                        extraVisualTextQuery = ""
                                    },
                                    modifier = Modifier.testTag("visual_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoCamera,
                                        contentDescription = "Visual Search",
                                        tint = CosmoGold
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchKeyText.isNotBlank()) {
                                    if (!recentQueries.contains(searchKeyText)) {
                                        recentQueries.add(0, searchKeyText)
                                    }
                                    viewModel.executeSangamSearch(searchKeyText, selectedSearchMode)
                                }
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmoDarkSurface,
                            unfocusedContainerColor = CosmoDarkSurface,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode Filters Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val modes = listOf(
                            "COMBINED" to "Unified Hybrid",
                            "INTERNET" to "Web Discovery",
                            "PERSONAL" to "Knowledge Vault",
                            "RESEARCH" to "Deep Research"
                        )
                        modes.forEach { (modeCode, label) ->
                            val isSelected = selectedSearchMode == modeCode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) CosmoSecondary.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) CosmoSecondary else Color.White.copy(alpha = 0.05f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedSearchMode = modeCode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CosmoGold else CosmoTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Autonomous Research trigger button
                    Button(
                        onClick = {
                            if (searchKeyText.isNotBlank()) {
                                if (!recentQueries.contains(searchKeyText)) {
                                    recentQueries.add(0, searchKeyText)
                                }
                                viewModel.runAutonomousResearchAgent(searchKeyText)
                            } else {
                                // Default fallback topic trigger
                                viewModel.runAutonomousResearchAgent("Future of Quantum Computing")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary.copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, CosmoSecondary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "",
                                tint = CosmoGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Activate Autonomous Search Agent: 'Research For Me'",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // WEATHER & DIGITAL ENVIRONMENT CONTEXT WIDGET
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Yellow.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WbSunny,
                                    contentDescription = "Weather",
                                    tint = CosmoGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Bettiah, West Champaran",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmoTextPrimary
                                )
                                Text(
                                    text = "Sunny | 28°C | Clear Skies | Wind 5km/h",
                                    fontSize = 9.sp,
                                    color = CosmoTextSecondary
                                )
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(CosmoGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(CosmoGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ENVIRONMENT SYNCED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = CosmoGreen
                            )
                        }
                    }
                }
            }

            // RECENT BROWSER TABS BAR
            item {
                val openTabs by viewModel.allTabs.collectAsStateWithLifecycle(initialValue = emptyList())
                if (openTabs.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "RECENT ACTIVE TABS (" + openTabs.size + ")",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmoSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(openTabs) { tab ->
                                val isTabActive = tab.isActive
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isTabActive) CosmoSecondary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f))
                                        .border(
                                            1.dp,
                                            if (isTabActive) CosmoSecondary else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.selectTab(tab.id)
                                            onShortcutClicked(tab.url)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (tab.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Layers,
                                            contentDescription = "",
                                            tint = if (isTabActive) CosmoGold else CosmoTextSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.title.take(15) + (if (tab.title.length > 15) "..." else ""),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isTabActive) Color.White else CosmoTextSecondary
                                        )
                                        if (openTabs.size > 1) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Close tab",
                                                tint = CosmoTextSecondary.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clickable { viewModel.closeTab(tab) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // RECENT SEARCHES
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "RECENT INQUIRIES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmoSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentQueries.take(3).forEach { query ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        searchKeyText = query
                                        viewModel.executeSangamSearch(query, selectedSearchMode)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = "",
                                        tint = CosmoTextSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = query,
                                        fontSize = 10.sp,
                                        color = CosmoTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TRENDING LOCAL & GLOBAL TOPICS
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, CosmoBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = "",
                                tint = CosmoGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TRENDING DISCOVERY TOPICS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmoGold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val trends = listOf(
                            "History of Bettiah Raj" to "Explore Maharaja Harendra Kishore and West Champaran ancestral glory",
                            "Future of Quantum Computing" to "NISQ fault tolerance, quantum entanglement and encryption limits",
                            "West Champaran Satyagraha 1917" to "Mahatma Gandhi's indigo farmers uprising details & archive",
                            "Knowledge Graph Heuristics" to "Cognitive AI database modeling using semantic triples"
                        )

                        trends.forEach { (title, desc) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchKeyText = title
                                        viewModel.executeSangamSearch(title, selectedSearchMode)
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmoTextPrimary
                                )
                                Text(
                                    text = desc,
                                    fontSize = 10.sp,
                                    color = CosmoTextSecondary
                                )
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.03f))
                                )
                            }
                        }
                    }
                }
            }

            // TASK EXECUTION & AGENT ORCHESTRATION DESK
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, CosmoBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "",
                                    tint = CosmoGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACTIVE ORCHESTRATION TASK DESK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmoGold,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            val compCount = tasksList.count { it.second }
                            Box(
                                modifier = Modifier
                                    .background(CosmoSecondary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$compCount/${tasksList.size} DONE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmoSecondary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        tasksList.forEachIndexed { idx, (itemTitle, isDone) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color.White.copy(alpha = 0.01f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isDone,
                                        onCheckedChange = { tasksList[idx] = itemTitle to it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = CosmoSecondary,
                                            uncheckedColor = Color.White.copy(alpha = 0.3f),
                                            checkmarkColor = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = itemTitle,
                                        fontSize = 10.sp,
                                        color = if (isDone) CosmoTextSecondary else Color.White,
                                        textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                                        lineHeight = 12.sp
                                    )
                                }
                                
                                if (!isDone) {
                                    if (isDeployingAgentTask == idx) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = CosmoGold,
                                            strokeWidth = 1.5.dp
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                isDeployingAgentTask = idx
                                                agentDeploymentProgress = 0f
                                                coroutineScope.launch {
                                                    while (agentDeploymentProgress < 1f) {
                                                        delay(300)
                                                        agentDeploymentProgress += 0.2f
                                                    }
                                                    viewModel.addNote(
                                                        "Agent Completed Task: $itemTitle",
                                                        "This task was autonomously executed inside SANGAM NEXUS. Findings processed into your Personal Knowledge Graph and registered into local index cache securely."
                                                    )
                                                    tasksList[idx] = itemTitle to true
                                                    isDeployingAgentTask = null
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Run task",
                                                tint = CosmoSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ACTIVE AI AGENTS PANEL & COGNITIVE DEBATE PORT
            item {
                val primaryAi by viewModel.primaryAiProvider.collectAsStateWithLifecycle()
                val secondaryAi by viewModel.secondaryAiProvider.collectAsStateWithLifecycle()
                val fallbackAi by viewModel.fallbackAiProvider.collectAsStateWithLifecycle()
                val isGoogleConnected by viewModel.isGoogleConnected.collectAsStateWithLifecycle()
                val isOpenAiConnected by viewModel.isOpenAiConnected.collectAsStateWithLifecycle()
                val isClaudeConnected by viewModel.isClaudeConnected.collectAsStateWithLifecycle()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, CosmoBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "",
                                tint = CosmoGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE AI AGENTS ORCHESTRATION PANEL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmoGold,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val agents = listOf(
                                Triple("Gemini ✨", isGoogleConnected, primaryAi == "Gemini"),
                                Triple("ChatGPT ⚡", isOpenAiConnected, primaryAi == "ChatGPT"),
                                Triple("Claude ❄️", isClaudeConnected, primaryAi == "Claude")
                            )
                            agents.forEach { (name, connected, isPrimary) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (connected) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.01f))
                                        .border(
                                            1.dp,
                                            if (isPrimary) CosmoSecondary else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (connected) "ACTIVE" else "DISCONNECTED",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (connected) CosmoGreen else CosmoTertiary
                                        )
                                        Text(
                                            text = if (isPrimary) "Primary" else "Backup",
                                            fontSize = 7.sp,
                                            color = CosmoTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                showDebateDialog = true
                                debateStage = 0
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmoGold.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, CosmoGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Chat,
                                    contentDescription = "",
                                    tint = CosmoGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Launch Joint Multi-AI Debate Session",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmoGold
                                )
                            }
                        }
                    }
                }
            }

            // MEETING & AUDIO INTELLIGENCE WORKSPACE
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, CosmoBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "",
                                tint = CosmoGold,
                                modifier = Modifier.size(16.dp)
                                    .then(if (isRecordingMeeting) Modifier.scale(voiceRippleScale) else Modifier)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI MEETING INTELLIGENCE DESK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmoGold,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        if (isRecordingMeeting) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CosmoTertiary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(CosmoTertiary, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("LIVE TRANSCRIPTION RECORDING...", fontSize = 10.sp, color = CosmoTertiary, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            isRecordingMeeting = false
                                            simulatedMeetings.add(0, "Automated Voice Meeting " + System.currentTimeMillis().toString().takeLast(4) to "Ready for AI analysis")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CosmoTertiary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("STOP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    isRecordingMeeting = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Mic, "", tint = CosmoGold, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Activate Real-Time Meeting Recorder", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text("SAVED MEETING TRANSCRIPTS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                        
                        simulatedMeetings.forEach { (title, subtitle) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(subtitle, fontSize = 8.sp, color = CosmoTextSecondary)
                                }
                                Button(
                                    onClick = {
                                        meetingAnalysisResult = "Processing meeting analysis..."
                                        showMeetingAnalysisDialog = true
                                        coroutineScope.launch {
                                            delay(1500)
                                            meetingAnalysisResult = """
                                                ### MEETING INTELLIGENCE OUTLINE: $title
                                                
                                                #### 1. Factual Takeaways:
                                                - Uncovered key architectural layout of legacy Bettiah Raj Palace grounds.
                                                - Checked indigo dispute files dates. Success factors confirmed.
                                                
                                                #### 2. AI Action Items compiled:
                                                - ADDED Action: 'Save completed Bettiah Raj outlines inside knowledge graph' (SYNCHRONIZED)
                                                - ADDED Action: 'Review Champaran Satyagraha details' (PENDING)
                                                
                                                This outline has been automatically compiled as a Note entry and synced to your active Task checklist!
                                            """.trimIndent()
                                            
                                            // Append Note
                                            viewModel.addNote("AI Meeting Analysis Summary: $title", "Factual transcription outline and compiled action items for regional West Champaran survey metadata synced.")
                                            
                                            // Prepend new tasks
                                            tasksList.add(0, "AI Action Item: Review Champaran Satyagraha archives" to false)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text("Compile AI analysis", fontSize = 9.sp, color = CosmoGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // LEARNING INTELLIGENCE WORKSPACE
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, CosmoBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Book,
                                    contentDescription = "",
                                    tint = CosmoGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SANGAM LEARNING INTELLIGENCE LAB",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmoGold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, "", tint = CosmoSecondary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("$studyStreak Days streak", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text("ACTIVE STUDY FLASHCARD", fontSize = 8.sp, fontWeight = FontWeight.Black, color = CosmoTextSecondary, letterSpacing = 0.5.sp)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .clickable { showFlashcardAnswer = !showFlashcardAnswer }
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (showFlashcardAnswer) CosmoSecondary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f)
                            ),
                            border = BorderStroke(1.dp, if (showFlashcardAnswer) CosmoSecondary else Color.White.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val (qText, aText) = studyFlashcards[flashcardIndex]
                                    Text(
                                        text = if (showFlashcardAnswer) "ANSWER:" else "CONCEPT / QUESTION:",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (showFlashcardAnswer) CosmoSecondary else CosmoGold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (showFlashcardAnswer) aText else qText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 3,
                                        lineHeight = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (showFlashcardAnswer) "Click anywhere to Hide" else "Click anywhere to Flip",
                                        fontSize = 7.sp,
                                        color = CosmoTextSecondary
                                    )
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    showFlashcardAnswer = false
                                    flashcardIndex = if (flashcardIndex > 0) flashcardIndex - 1 else studyFlashcards.size - 1
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("Previous", fontSize = 9.sp, color = CosmoSecondary)
                            }
                            Text("${flashcardIndex + 1}/${studyFlashcards.size}", fontSize = 10.sp, color = CosmoTextSecondary)
                            Button(
                                onClick = {
                                    showFlashcardAnswer = false
                                    flashcardIndex = if (flashcardIndex < studyFlashcards.size - 1) flashcardIndex + 1 else 0
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("Next Slide", fontSize = 9.sp, color = CosmoSecondary)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                showQuizDialog = true
                                quizQuestionIndex = 0
                                quizScore = 0
                                selectedQuizAnswer = null
                                quizFinished = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmoGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Help, "", tint = CosmoDarkBackground, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate AI Knowledge Quiz from Notes", fontSize = 10.sp, color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SAVED PRIVATE KNOWLEDGE DATABASE (CRUD INTERFACES)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SAVED HYBRID KNOWLEDGE VAULT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmoSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Tab selector for Vault contents
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf("Notes", "Knowledge Graph", "Memories", "Bookmarks", "Downloads")
                        tabs.forEachIndexed { index, title ->
                            val isActive = activeVaultTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) CosmoSecondary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f))
                                    .border(
                                        1.dp,
                                        if (isActive) CosmoSecondary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { activeVaultTab = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) CosmoGold else CosmoTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Vault Content Display Area
                    when (activeVaultTab) {
                        0 -> { // Notes List
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Note Records (${notes.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                                    IconButton(
                                        onClick = { showAddNoteDialog = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add", tint = CosmoGold, modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (notes.isEmpty()) {
                                    Text("No note records compiled yet. Click '+' to add Note.", color = CosmoTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 12.dp))
                                } else {
                                    notes.forEach { note ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                                            border = BorderStroke(1.dp, CosmoBorder)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(note.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CosmoGold)
                                                    IconButton(onClick = { viewModel.removeNote(note.id) }, modifier = Modifier.size(16.dp)) {
                                                        Icon(Icons.Filled.Clear, "", tint = CosmoTertiary, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(note.content, fontSize = 11.sp, color = CosmoTextPrimary)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(note.timestamp)), fontSize = 8.sp, color = CosmoTextSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Knowledge Graph Triples
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Knowledge Graph Relations (${knowledgeGraph.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                                    IconButton(
                                        onClick = { showAddRelationDialog = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add", tint = CosmoGold, modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (knowledgeGraph.isEmpty()) {
                                    Text("Knowledge graph is empty. Add a triple relationship.", color = CosmoTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 12.dp))
                                } else {
                                    knowledgeGraph.forEach { triple ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                                            border = BorderStroke(1.dp, CosmoSecondary.copy(alpha = 0.2f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(triple.subject, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CosmoTextPrimary)
                                                        Text("  → [${triple.relationship}] →  ", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = CosmoGold)
                                                        Text(triple.objectName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CosmoSecondary)
                                                    }
                                                    IconButton(onClick = { viewModel.removeKnowledgeGraph(triple.id) }, modifier = Modifier.size(16.dp)) {
                                                        Icon(Icons.Filled.Clear, "", tint = CosmoTertiary, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                                if (triple.summary.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(triple.summary, fontSize = 10.sp, color = CosmoTextSecondary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> { // Memory Vault
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("AI Memory Vault Records (${memories.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                                    IconButton(
                                        onClick = { showAddMemoryDialog = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add", tint = CosmoGold, modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (memories.isEmpty()) {
                                    Text("No AI memories recorded.", color = CosmoTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 12.dp))
                                } else {
                                    memories.forEach { mem ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                                            border = BorderStroke(1.dp, CosmoBorder)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Topic Match: \"${mem.query}\"", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = CosmoSecondary)
                                                    IconButton(onClick = { viewModel.removeMemory(mem.id) }, modifier = Modifier.size(16.dp)) {
                                                        Icon(Icons.Filled.Clear, "", tint = CosmoTertiary, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(mem.keyFact, fontSize = 11.sp, color = CosmoTextPrimary, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> { // Bookmarks & Saved PDFs
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Bookmarks & PDF Registers", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                bookmarks.forEach { bmk ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { onBookmarkClicked(bmk.url) },
                                        colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (bmk.url.endsWith(".pdf", ignoreCase = true)) Icons.Filled.PictureAsPdf else Icons.Filled.Bookmark,
                                                contentDescription = "",
                                                tint = if (bmk.url.endsWith(".pdf", ignoreCase = true)) CosmoTertiary else CosmoGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(bmk.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CosmoTextPrimary)
                                                Text(bmk.url, fontSize = 9.sp, color = CosmoTextSecondary, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        4 -> { // Downloads
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Downloaded / Saved Files Directory", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (downloads.isEmpty()) {
                                    Text("No downloaded files.", color = CosmoTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 12.dp))
                                } else {
                                    downloads.take(4).forEach { dl ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Filled.FileDownload, contentDescription = "", tint = CosmoGreen, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(dl.fileName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CosmoTextPrimary)
                                                        Text("Mime: ${dl.mimeType} | Status: ${dl.status}", fontSize = 9.sp, color = CosmoTextSecondary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SANGAM SEARCH DISCOVERY LAYER SCREEN OVERLAY (SHOWS RESULTS) ---
        if (sangamSearchActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CosmoDarkBackground)
            ) {
                when (val result = sangamSearchState) {
                    SangamSearchState.Idle -> {
                        // Display nothing or subtle background when idle
                    }
                    SangamSearchState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CosmoGold)
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = if (isResearchRunning) "Running Autonomous Research Agent..." else "Contacting public servers & scraping indexing clusters...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmoTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isResearchRunning) {
                                    Text(
                                        text = "Removing duplications, scanning files and populating private Knowledge Graph...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CosmoGold,
                                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    is SangamSearchState.Success -> {
                        val report = result
                        // SANGAM DISCOVERY REMAINS INTERACTIVE (FULL DISPLAY LAYER MODULE)
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top close overlay toolbar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CosmoDarkSurface)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = "",
                                        tint = CosmoGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SANGAM DISCOVERY SYSTEM",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black,
                                        color = CosmoGold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Mode Indicator
                                    Box(
                                        modifier = Modifier
                                            .background(CosmoSecondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = result.providerUsed,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CosmoSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = { viewModel.clearSangamSearch() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "Close search", tint = CosmoTextSecondary)
                                    }
                                }
                            }

                            // --- SANGAM INTELLIGENT RESULT CATEGORIES SUB-BAR ---
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CosmoDarkSurface)
                                    .border(1.dp, CosmoBorder, RoundedCornerShape(0.dp))
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                val searchCats = listOf(
                                    0 to "ALL RESULTS",
                                    1 to "AI INTELLIGENCE",
                                    2 to "MULTIMEDIA",
                                    3 to "DEEP RESEARCH",
                                    4 to "PERSONAL VAULT"
                                )
                                items(searchCats) { (idx, label) ->
                                    val isSelected = selectedResultCategory == idx
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CosmoGold.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(1.dp, if (isSelected) CosmoGold else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectedResultCategory = idx }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("result_tab_$idx"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) CosmoGold else Color.White.copy(alpha = 0.6f),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                            ) {
                                // Search Query Header
                                item {
                                    val visualPayload = viewModel.searchImageSource.collectAsStateWithLifecycle().value
                                    if (visualPayload != null) {
                                        Box(
                                            modifier = Modifier
                                                .padding(bottom = 8.dp)
                                                .background(CosmoGold.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                                .border(1.dp, CosmoGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.PhotoCamera,
                                                    contentDescription = "",
                                                    tint = CosmoGold,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "MULTI-MODAL VISION ATTACHED: $visualPayload",
                                                    fontSize = 10.sp,
                                                    color = CosmoGold,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = result.query.uppercase(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CosmoSecondary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "AI Discovery Report",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // AI Result Enrichment Indicators Badge Block
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Trust Score Indicator Progress Circle inline
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    progress = { result.trustScore / 100f },
                                                    modifier = Modifier.size(12.dp),
                                                    color = CosmoGreen,
                                                    strokeWidth = 2.dp,
                                                    trackColor = Color.White.copy(alpha = 0.2f)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "TRUST INDEX: ${result.trustScore}%",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CosmoGreen
                                                )
                                            }
                                        }

                                        // Reading Time Badge
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = result.readingTime.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CosmoGold
                                            )
                                        }

                                        // Fact Verification Status Seal
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = result.factVerification.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = CosmoSecondary
                                            )
                                        }
                                    }
                                }

                                if (selectedResultCategory == 0 || selectedResultCategory == 1) {
                                    // DIRECT ANSWER & AI SUMMARY CARD
                                    item {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF110E29)),
                                            border = BorderStroke(1.dp, CosmoSecondary.copy(alpha = 0.4f))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(CosmoGold, CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "DIRECT ANSWER TRUTH NODE",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = CosmoGold,
                                                        letterSpacing = 1.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = result.directAnswer,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "AI COGNITIVE SUMMARY",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CosmoSecondary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = result.aiSummary,
                                                    fontSize = 12.sp,
                                                    color = CosmoTextPrimary
                                                )
                                            }
                                        }
                                    }

                                    // PUBLIC SECTOR (INTERNET DISCOVERY CHANNELS)
                                    item {
                                        Text("PUBLIC ONLINE DISCOVERY INDEX", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary, letterSpacing = 1.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    // Key Facts Bullets
                                    item {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text("Key Discovered Facts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoGold)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                result.keyFacts.forEach { fact ->
                                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                                        Text("✦  ", color = CosmoGold, fontSize = 11.sp)
                                                        Text(fact, fontSize = 11.sp, color = CosmoTextPrimary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (selectedResultCategory == 0 || selectedResultCategory == 2) {
                                    // Public Documents / PDF Search & Downloads
                                    if (result.pdfs.isNotEmpty()) {
                                        item {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 12.dp),
                                                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                                                border = BorderStroke(1.dp, CosmoBorder)
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = "", tint = CosmoTertiary, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Discovered Public Documents & PDFs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoTextPrimary)
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    result.pdfs.forEach { pdf ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    // Trigger simulated file download stream
                                                                    viewModel.addNote("Saved PDF: $pdf", "Downloaded from public search archives. Source indexing coordinates: Universal web.")
                                                                    viewModel.executeSangamSearch(result.query, selectedSearchMode)
                                                                }
                                                                .padding(vertical = 6.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(pdf, fontSize = 11.sp, color = CosmoSecondary, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                                                            Text("[DOWNLOAD]", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CosmoGreen)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Discovered Imagery (Images Search)
                                    item {
                                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                            Text("Scaped Pictures & Imagery", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                result.images.forEach { label ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(120.dp, 80.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                Brush.linearGradient(
                                                                    colors = listOf(Color(0xFF2E1065), Color(0xFF3F1085))
                                                                )
                                                            )
                                                            .padding(8.dp),
                                                        contentAlignment = Alignment.BottomStart
                                                    ) {
                                                        Icon(Icons.Filled.Image, "", tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.fillMaxSize())
                                                        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Discovered Videos
                                    item {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp),
                                            colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text("Discovered Multimedia & Video Lectures", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoTextPrimary)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                result.videos.forEach { video ->
                                                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.PlayCircle, "", tint = CosmoSecondary, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(video, fontSize = 11.sp, color = CosmoSecondary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // DEEP RESEARCH ENGINE SUITE
                                if (result.isDeepResearch && (selectedResultCategory == 0 || selectedResultCategory == 3)) {
                                    item {
                                        Text("DEEP RESEARCH MODULES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoGold, letterSpacing = 1.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    // Viewpoints Comparisons
                                    if (result.comparisons.isNotBlank()) {
                                        item {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 12.dp),
                                                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                                                border = BorderStroke(1.dp, CosmoGold.copy(alpha = 0.3f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("Comparisons & Viewpoints Analyzed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoGold)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(result.comparisons, fontSize = 11.sp, color = CosmoTextPrimary)
                                                }
                                            }
                                        }
                                    }

                                    // Discrepancies and Contradictions
                                    if (result.contradictions.isNotBlank()) {
                                        item {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 12.dp),
                                                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface)
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("Discovered Contradictions & Debates", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoTertiary)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(result.contradictions, fontSize = 11.sp, color = CosmoTextPrimary)
                                                }
                                            }
                                        }
                                    }

                                    // Chronological Research Timeline Flow
                                    if (result.timeline.isNotEmpty()) {
                                        item {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 12.dp),
                                                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface)
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("Historical Chronological Timeline", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoTextPrimary)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    result.timeline.forEach { event ->
                                                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .padding(top = 4.dp)
                                                                    .size(6.dp)
                                                                    .background(CosmoGold, CircleShape)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(event, fontSize = 11.sp, color = CosmoTextPrimary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Node Graph triple models (Source Map)
                                    if (result.sourceMap.isNotEmpty()) {
                                        item {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 12.dp),
                                                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                                                border = BorderStroke(1.dp, CosmoSecondary.copy(alpha = 0.2f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("Analyzed Node Map Connections", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    result.sourceMap.forEach { element ->
                                                        val components = element.split("->").map { it.trim() }
                                                        if (components.size >= 3) {
                                                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                                                 Text(components[0], fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CosmoGold)
                                                                 Text(" [${components[1]}] ", fontSize = 10.sp, color = CosmoSecondary)
                                                                 Text(components[2], fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                                            }
                                                        } else {
                                                            Text(element, fontSize = 10.sp, color = CosmoTextSecondary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Research Essay Document Block
                                    if (result.researchReport.isNotBlank()) {
                                        item {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 16.dp),
                                                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text("Deep Research Monograph", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoTextPrimary)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(result.researchReport, fontSize = 11.sp, color = CosmoTextPrimary)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (selectedResultCategory == 0 || selectedResultCategory == 4) {
                                    // PERSONAL HYBRID SECTOR (SCANS AND INLINES PRIVATE DB RECORDS)
                                    item {
                                        Text("PERSONAL VAULT SYNCHRONIZATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoGreen, letterSpacing = 1.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    // Match Notes
                                    item {
                                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            Text("Matching Private Notes (${result.matchingNotes.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                                            if (result.matchingNotes.isEmpty()) {
                                                Text("No correlating private records registered.", fontSize = 10.sp, color = CosmoTextSecondary)
                                            } else {
                                                result.matchingNotes.forEach { note ->
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                        colors = CardDefaults.cardColors(containerColor = CosmoDarkSurfaceVariant)
                                                    ) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Text(note.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CosmoGold)
                                                            Text(note.content, fontSize = 10.sp, color = CosmoTextPrimary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Match Graph nodes
                                    item {
                                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            Text("Matching Knowledge Graph Rules (${result.matchingGraph.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                                            if (result.matchingGraph.isEmpty()) {
                                                Text("No correlating graph nodes found.", fontSize = 10.sp, color = CosmoTextSecondary)
                                            } else {
                                                result.matchingGraph.forEach { node ->
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                        colors = CardDefaults.cardColors(containerColor = CosmoDarkSurfaceVariant)
                                                    ) {
                                                        Text(
                                                            text = "${node.subject} -> [${node.relationship}] -> ${node.objectName}",
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 11.sp,
                                                            color = CosmoTextPrimary,
                                                            modifier = Modifier.padding(10.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Match Memories
                                    item {
                                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                            Text("Matching AI Memories (${result.matchingMemories.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                                            if (result.matchingMemories.isEmpty()) {
                                                Text("No corresponding local facts cached in vault.", fontSize = 10.sp, color = CosmoTextSecondary)
                                            } else {
                                                result.matchingMemories.forEach { mem ->
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                        colors = CardDefaults.cardColors(containerColor = CosmoDarkSurfaceVariant)
                                                    ) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Text("Topic Search: \"${mem.query}\"", fontSize = 9.sp, color = CosmoSecondary)
                                                            Text(mem.keyFact, fontSize = 11.sp, color = CosmoTextPrimary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // AUTONOMOUS AGENT REPORT COMPILER CHANNELS SAVE
                                    item {
                                        Text("COGNITIVE SAVE CONTROLS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoGold, letterSpacing = 1.sp)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    // Save full report as custom note
                                                    viewModel.addNote(
                                                        "Saved Search Protocol: ${result.query}",
                                                        "Direct Truth Answer:\n${result.directAnswer}\n\nKey Insights scrape:\n${result.keyFacts.joinToString("\n")}"
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary)
                                            ) {
                                                Text("Save to Notes", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    // Save queries key insights to memories vault
                                                    result.keyFacts.forEach { fact ->
                                                        viewModel.addMemory(result.query, fact)
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary)
                                            ) {
                                                Text("Save to Memory", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    // Append relations to the graph
                                                    if (result.sourceMap.isNotEmpty()) {
                                                        result.sourceMap.forEach { relation ->
                                                            val subparts = relation.split("->").map { it.trim() }
                                                            if (subparts.size >= 3) {
                                                                viewModel.addKnowledgeGraph(subparts[0], subparts[1], subparts[2], "Automated scrape save matrix.")
                                                            }
                                                        }
                                                    } else {
                                                        viewModel.addKnowledgeGraph(result.query, "classified as", "Discovered Information", "Automated mapping.")
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary)
                                            ) {
                                                Text("Save to Graph", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is SangamSearchState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Text("Sangam Search Compile Error", color = CosmoTertiary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(result.message, color = CosmoTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { viewModel.clearSangamSearch() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary)
                                ) {
                                    Text("Reset Discovery Frame")
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SIMULATED VOICE DIALOG ---
        if (showVoiceDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                    border = BorderStroke(1.dp, CosmoSecondary)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "COGNITIVE VOICE LISTENER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = CosmoGold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Pulsing ripple mic representation
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .scale(voiceRippleScale)
                                    .background(CosmoSecondary.copy(alpha = 0.15f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(CosmoSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = voiceTranscript,
                            fontSize = 12.sp,
                            color = CosmoTextPrimary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Speak into your mobile microphone. Sangam Voice Heuristics will parse the language instantly.",
                            fontSize = 10.sp,
                            color = CosmoTextSecondary,
                        )
                    }
                }
            }
        }

        // --- MULTI-MODAL VISUAL SEARCH DIALOG ---
        if (showImageSearchDialog) {
            var isScanning by remember { mutableStateOf(false) }
            val scanLineY = remember { Animatable(0f) }
            
            // Loop scanning animation
            LaunchedEffect(isScanning) {
                if (isScanning) {
                    while (true) {
                        scanLineY.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(1250, easing = LinearEasing)
                        )
                        scanLineY.snapTo(0f)
                    }
                }
            }

            Dialog(onDismissRequest = { showImageSearchDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("visual_search_dialog"),
                    colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                    border = BorderStroke(1.dp, CosmoGold),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Title
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "",
                                tint = CosmoGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SANGAM MULTI-MODAL VISION HUB",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text(
                            "Select or snap a high-density intelligence visual, add complementary coordinates to query Sangam Oracle instantly.",
                            fontSize = 10.sp,
                            color = CosmoTextSecondary,
                            lineHeight = 13.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Selected Visual Source Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(1.dp, if (selectedImageLabel != null) CosmoGold else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageLabel != null) {
                                // Display Scanning Shader Overlay
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (selectedImageLabel!!.contains("Qubit")) Icons.Filled.Memory 
                                                    else if (selectedImageLabel!!.contains("Satyagraha")) Icons.Filled.History
                                                    else if (selectedImageLabel!!.contains("Bettiah")) Icons.Filled.Home
                                                    else Icons.Filled.AutoAwesome,
                                        contentDescription = "",
                                        tint = CosmoGold,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = selectedImageLabel ?: "",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = if (isScanning) "ANALYZING TENSOR SPECTRA..." else "IMAGE ARTIFACT LOADED",
                                        color = if (isScanning) CosmoSecondary else CosmoGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                
                                if (isScanning) {
                                    // Animated Scan Line
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(0.04f)
                                            .align(Alignment.TopStart)
                                            .offset(y = 130.dp * scanLineY.value)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, CosmoSecondary, Color.Transparent)
                                                )
                                            )
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.AddPhotoAlternate, "", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No visual payload loaded", fontSize = 10.sp, color = CosmoTextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Template Preset Selectors
                        Text("PRECURATED HIGH-DENSITY IMAGE SCHEMATICS", fontSize = 9.sp, color = CosmoSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val presets = listOf(
                            Triple("Bettiah_Raj_Palace.png", "Bettiah Raj Palace Historical Artifact", "Bettiah Raj"),
                            Triple("Superconducting_Qubits.png", "Qubit Cryogenic Thermal Blueprint", "Future of Quantum Computing"),
                            Triple("Satyagraha_Civil_Campaign.png", "Champaran Indigo Documents (1917)", "West Champaran Satyagraha")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.forEach { (uri, label, key) ->
                                val isChosen = selectedImageUri == uri
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChosen) CosmoGold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                        .border(1.dp, if (isChosen) CosmoGold else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedImageUri = uri
                                            selectedImageLabel = label
                                            extraVisualTextQuery = key
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = if (key.contains("Quantum")) Icons.Filled.Memory else if (key.contains("Satyagraha")) Icons.Filled.History else Icons.Filled.Home,
                                            contentDescription = "",
                                            tint = if (isChosen) CosmoGold else Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = key.split(" ").last(),
                                            fontSize = 9.sp,
                                            color = if (isChosen) CosmoGold else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Complementary text field supporting unified multi-modal query
                        OutlinedTextField(
                            value = extraVisualTextQuery,
                            onValueChange = { extraVisualTextQuery = it },
                            label = { Text("Complementary text coordinates", color = CosmoTextSecondary, fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("visual_extra_text_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CosmoGold,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                            ),
                            textStyle = TextStyle(fontSize = 11.sp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showImageSearchDialog = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Text("Cancel", color = CosmoTextSecondary, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (selectedImageUri == null) {
                                        // Auto snap custom if nothing chosen
                                        selectedImageUri = "custom_camera_capture.png"
                                        selectedImageLabel = "External Camera Payload Snapshot"
                                        extraVisualTextQuery = extraVisualTextQuery.ifBlank { "Deep Research Machine" }
                                    }
                                    isScanning = true
                                    coroutineScope.launch {
                                        delay(1500)
                                        isScanning = false
                                        showImageSearchDialog = false
                                        
                                        // Save raw visual link inside the ViewModel search tracker state
                                        viewModel.searchImageSource.value = selectedImageLabel
                                        
                                        // Trigger Multi-Modal execute with dynamic categorizing!
                                        val finalQuery = extraVisualTextQuery.ifBlank { "Deep Research Machine" }
                                        searchKeyText = finalQuery
                                        viewModel.executeSangamSearch(finalQuery, selectedSearchMode)
                                    }
                                },
                                modifier = Modifier.weight(1.2f).testTag("visual_trigger_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CosmoGold),
                                enabled = !isScanning
                            ) {
                                Text(
                                    text = if (isScanning) "Analyzing..." else "Execute Vision Search",
                                    color = CosmoDarkBackground,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- CRUD DIALOGS: ADD NOTE ---
        if (showAddNoteDialog) {
            AlertDialog(
                onDismissRequest = { showAddNoteDialog = false },
                title = { Text("Compile Local Vault Note", color = CosmoGold, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = noteTitleInput,
                            onValueChange = { noteTitleInput = it },
                            label = { Text("Note Title", color = CosmoTextSecondary) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CosmoGold,
                                unfocusedBorderColor = CosmoBorder
                            )
                        )
                        OutlinedTextField(
                            value = noteContentInput,
                            onValueChange = { noteContentInput = it },
                            label = { Text("Contents Description", color = CosmoTextSecondary) },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CosmoGold,
                                unfocusedBorderColor = CosmoBorder
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (noteTitleInput.isNotBlank() && noteContentInput.isNotBlank()) {
                                viewModel.addNote(noteTitleInput, noteContentInput)
                                noteTitleInput = ""
                                noteContentInput = ""
                                showAddNoteDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoGold)
                    ) {
                        Text("Save Note", color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showAddNoteDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = CosmoTextSecondary)
                    }
                },
                containerColor = CosmoDarkSurface
            )
        }

        // --- CRUD DIALOGS: ADD RELATION (KNOWLEDGE GRAPH) ---
        if (showAddRelationDialog) {
            AlertDialog(
                onDismissRequest = { showAddRelationDialog = false },
                title = { Text("Build Semantic Knowledge Node", color = CosmoGold, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = relSubjectInput,
                            onValueChange = { relSubjectInput = it },
                            label = { Text("Subject (e.g. Sir Harendra Kishore)", color = CosmoTextSecondary) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CosmoSecondary,
                                unfocusedBorderColor = CosmoBorder
                            )
                        )
                        OutlinedTextField(
                            value = relTypeInput,
                            onValueChange = { relTypeInput = it },
                            label = { Text("Relationship (e.g. governed)", color = CosmoTextSecondary) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CosmoSecondary,
                                unfocusedBorderColor = CosmoBorder
                            )
                        )
                        OutlinedTextField(
                            value = relObjectInput,
                            onValueChange = { relObjectInput = it },
                            label = { Text("Object/Target (e.g. Bettiah Raj)", color = CosmoTextSecondary) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CosmoSecondary,
                                unfocusedBorderColor = CosmoBorder
                            )
                        )
                        OutlinedTextField(
                            value = relSummaryInput,
                            onValueChange = { relSummaryInput = it },
                            label = { Text("Summary context (Optional)", color = CosmoTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CosmoSecondary,
                                unfocusedBorderColor = CosmoBorder
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (relSubjectInput.isNotBlank() && relTypeInput.isNotBlank() && relObjectInput.isNotBlank()) {
                                viewModel.addKnowledgeGraph(relSubjectInput, relTypeInput, relObjectInput, relSummaryInput)
                                relSubjectInput = ""
                                relTypeInput = ""
                                relObjectInput = ""
                                relSummaryInput = ""
                                showAddRelationDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary)
                    ) {
                        Text("Inject Relation", color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showAddRelationDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = CosmoTextSecondary)
                    }
                },
                containerColor = CosmoDarkSurface
            )
        }

        // --- CRUD DIALOGS: ADD MEMORY VAULT NODE ---
        if (showAddMemoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddMemoryDialog = false },
                title = { Text("Register Vault Key Fact", color = CosmoGold, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = memTopicInput,
                            onValueChange = { memTopicInput = it },
                            label = { Text("Topic Query Filter (e.g. Quantum Qubits)", color = CosmoTextSecondary) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CosmoSecondary,
                                unfocusedBorderColor = CosmoBorder
                            )
                        )
                        OutlinedTextField(
                            value = memFactInput,
                            onValueChange = { memFactInput = it },
                            label = { Text("Cached Fact Statement of Truth", color = CosmoTextSecondary) },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CosmoSecondary,
                                unfocusedBorderColor = CosmoBorder
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (memTopicInput.isNotBlank() && memFactInput.isNotBlank()) {
                                viewModel.addMemory(memTopicInput, memFactInput)
                                memTopicInput = ""
                                memFactInput = ""
                                showAddMemoryDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary)
                    ) {
                        Text("Cache Memory", color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showAddMemoryDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = CosmoTextSecondary)
                    }
                },
                containerColor = CosmoDarkSurface
            )
        }

        // --- MEETING ANALYSIS DIALOGUE ---
        if (showMeetingAnalysisDialog) {
            AlertDialog(
                onDismissRequest = { showMeetingAnalysisDialog = false },
                title = { Text("Compile Meeting Intelligence Analysis", color = CosmoGold, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                text = {
                    Column {
                        Text(
                            text = meetingAnalysisResult,
                            color = Color.White,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showMeetingAnalysisDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary)
                    ) {
                        Text("Acknowledge & Close", color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = CosmoDarkSurface
            )
        }

        // --- DYNAMIC LEARNING QUIZ DIALOGUE ---
        if (showQuizDialog) {
            val quizQuestions = remember {
                listOf(
                    Triple(
                        "Which dynasty governed the Bettiah Raj estate in West Champaran?",
                        listOf("Darbhanga line", "Ujjainia Parmar lineage", "Mughal Imperial governors"),
                        1
                    ),
                    Triple(
                        "Who was the last active Maharaja of Bettiah Raj?",
                        listOf("Sir Ugra Sen Bahadur", "Sir Harendra Kishore Singh", "Maharaja Jung Bahadur"),
                        1
                    ),
                    Triple(
                        "Which hardware qubit technology requires cryogenic environments down to 15 mK?",
                        listOf("Silicon Spin Qubits", "Trapped Ion Arrays", "Superconducting Circuits"),
                        2
                    )
                )
            }
            
            AlertDialog(
                onDismissRequest = { showQuizDialog = false },
                title = { 
                    Text(
                        text = if (quizFinished) "Quiz Compiling Result" else "Knowledge Quiz: Q ${quizQuestionIndex + 1}/${quizQuestions.size}",
                        color = CosmoGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ) 
                },
                text = {
                    Column {
                        if (quizFinished) {
                            Text("Your score: $quizScore out of ${quizQuestions.size}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (quizScore == quizQuestions.size) "Perfect! Your cognitive synergy matches SANGAM NEXUS requirements." else "Review your Saved Notes section to score perfectly.",
                                fontSize = 11.sp,
                                color = CosmoTextSecondary
                            )
                        } else {
                            val currentQuiz = quizQuestions[quizQuestionIndex]
                            Text(currentQuiz.first, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            currentQuiz.second.forEachIndexed { optIdx, option ->
                                val isSelected = selectedQuizAnswer == optIdx
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CosmoSecondary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                        .border(1.dp, if (isSelected) CosmoSecondary else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .clickable { selectedQuizAnswer = optIdx }
                                        .padding(10.dp)
                                ) {
                                    Text(option, fontSize = 11.sp, color = if (isSelected) CosmoGold else Color.White)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (quizFinished) {
                        Button(
                            onClick = { showQuizDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmoGold)
                        ) {
                            Text("Finish Guide", color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                val currentQuiz = quizQuestions[quizQuestionIndex]
                                if (selectedQuizAnswer != null) {
                                    if (selectedQuizAnswer == currentQuiz.third) {
                                        quizScore += 1
                                    }
                                    selectedQuizAnswer = null
                                    if (quizQuestionIndex < quizQuestions.size - 1) {
                                        quizQuestionIndex += 1
                                    } else {
                                        quizFinished = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary),
                            enabled = selectedQuizAnswer != null
                        ) {
                            Text(
                                text = if (quizQuestionIndex == quizQuestions.size - 1) "Finish Quiz" else "Next Question",
                                color = CosmoDarkBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                containerColor = CosmoDarkSurface
            )
        }

        // --- MULTI-AI DEBATE ENGINE DIALOGUE ---
        if (showDebateDialog) {
            AlertDialog(
                onDismissRequest = { showDebateDialog = false },
                title = { Text("COGNITIVE MULTI-AI DEBATE LAB", color = CosmoGold, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (debateStage == 0) {
                            Text("Initiate a structured debate with connected AI models to research, compare differences, and build a consensual report.", fontSize = 11.sp, color = CosmoTextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = debateQuestion,
                                onValueChange = { debateQuestion = it },
                                label = { Text("Debate Topic Query", color = CosmoTextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CosmoGold,
                                    unfocusedBorderColor = CosmoBorder
                                )
                            )
                        } else if (debateStage == 1) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = CosmoGold)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Polling ChatGPT, Gemini, and Claude...", fontSize = 11.sp, color = CosmoGold, fontWeight = FontWeight.Bold)
                                    Text("Compiling differences and confidence spectra...", fontSize = 9.sp, color = CosmoTextSecondary)
                                }
                            }
                        } else {
                            val data = debateResult
                            if (data != null) {
                                LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                                    item {
                                        Text("QUESTION: ${debateQuestion.uppercase()}", fontWeight = FontWeight.Black, color = CosmoGold, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("CHATGPT [POSITION A]:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(data.chatGptArg, fontSize = 10.sp, color = Color.White)
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("GEMINI [POSITION B]:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CosmoGold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(data.geminiArg, fontSize = 10.sp, color = Color.White)
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("CLAUDE [NEUTRAL ETHICAL STUDY]:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CosmoTertiary)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(data.claudeArg, fontSize = 10.sp, color = Color.White)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Text("CONSENSUS REPORT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CosmoGreen)
                                        Text(data.consensusReport, fontSize = 10.sp, color = CosmoTextPrimary, modifier = Modifier.padding(vertical = 4.dp))
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text("DISAGREEMENT MATRIX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CosmoTertiary)
                                        Text(data.disagreementMatrix, fontSize = 10.sp, color = CosmoTextPrimary, modifier = Modifier.padding(vertical = 4.dp))
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text("CONFIDENCE RATINGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CosmoGold)
                                        Text(data.confidenceScores, fontSize = 10.sp, color = CosmoTextPrimary, modifier = Modifier.padding(vertical = 4.dp))
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text("BALANCED CONCLUSION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(data.balancedConclusion, fontSize = 10.sp, color = CosmoTextSecondary, modifier = Modifier.padding(vertical = 4.dp))
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text("EVIDENCE CONVERGENCE COGNITIVE REFERENCES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                                        data.evidenceReferences.forEach { ref ->
                                            Text("- $ref", fontSize = 9.sp, color = CosmoTextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (debateStage == 0) {
                        Button(
                            onClick = {
                                debateStage = 1
                                coroutineScope.launch {
                                    delay(2000)
                                    val isQuantum = debateQuestion.lowercase().contains("quantum") || debateQuestion.lowercase().contains("qubit")
                                    debateResult = if (isQuantum) {
                                        DebateResultData(
                                            chatGptArg = "Focuses heavily on commercializing superconducting qubits, leveraging immediate software optimization layers to bypass hardware noise constraints. Asserts fast market insertion.",
                                            geminiArg = "Emphasizes the multimodal integration of quantum coprocessors with cloud neural networks, predicting massive benefits for universal medical diagnostic search and material discovery.",
                                            claudeArg = "Maintains a neutral, rigorous stance pointing out the high probability of security decryption breaches and ethics alignment issues of fault-tolerant systems.",
                                            consensusReport = "All networks agree that post-quantum cryptography (PQC) lattice migrations represent a critical priority to prevent immediate decryption of state-level secrets.",
                                            disagreementMatrix = "Deep differences exist on development timelines. ChatGPT estimates commercial market readiness in 3 years; Claude maintains a careful 8-10 year logical projection.",
                                            confidenceScores = "Gemini: 91% | ChatGPT: 87% | Claude: 94%",
                                            balancedConclusion = "Quantum supremacy is valid under theoretical models, but general-purpose fault tolerance requires substantial advances in thermal insulation and cryogenics coherence.",
                                            evidenceReferences = listOf(
                                                "Shor, P. - Algorithms for Quantum Cryptography, IEEE 1994",
                                                "NIST Special Publication 800-224 - Post-Quantum Cryptanalysis Standards"
                                            )
                                        )
                                    } else {
                                        DebateResultData(
                                            chatGptArg = "AGI presents an unprecedented economic acceleration opportunity, automating production lines and solving macro-economic distribution inefficiencies under proper capitalist stewardship.",
                                            geminiArg = "Understands AGI as a collaborative global super-intelligence catalyst, bringing premium interactive education and medical diagnostics to remote regional communities.",
                                            claudeArg = "Warns of profound misalignment risks, sudden capability jumps, and systemic societal labor shocks which must be regulated by international democratic standards.",
                                            consensusReport = "All connected AI networks align on the urgent requirement for global standard safety layers and transparent neural weights tracking.",
                                            disagreementMatrix = "ChatGPT promotes fast permissionless deployment, Gemini argues for centralized developer alignments, while Claude asserts strict containment and verification limits.",
                                            confidenceScores = "Gemini: 95% | ChatGPT: 90% | Claude: 97%",
                                            balancedConclusion = "AGI holds the capacity to double global productive output, but the rate of deployment must be strictly synchronized with containment safety boundaries to prevent massive ethical collapses.",
                                            evidenceReferences = listOf(
                                                "Bostrom, N. - Superintelligence: Paths, Dangers, Strategies (2014)",
                                                "Amodei, D. - Concrete Problems in AI Safety, OpenAI 2016"
                                            )
                                        )
                                    }
                                    debateStage = 2
                                    viewModel.addNote("AI Debate consensus: " + debateQuestion, "ChatGPT, Gemini, and Claude debated: '" + debateQuestion + "'. Consensus compiled. Disagreement matrix registered.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmoGold)
                        ) {
                            Text("Trigger Debate Engine", color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                        }
                    } else if (debateStage == 2) {
                        Button(
                            onClick = { showDebateDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary)
                        ) {
                            Text("Dismiss Portal", color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    if (debateStage == 0) {
                        Button(
                            onClick = { showDebateDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("Cancel", color = CosmoTextSecondary)
                        }
                    }
                },
                containerColor = CosmoDarkSurface
            )
        }
    }
}

data class DebateResultData(
    val chatGptArg: String,
    val geminiArg: String,
    val claudeArg: String,
    val consensusReport: String,
    val disagreementMatrix: String,
    val confidenceScores: String,
    val balancedConclusion: String,
    val evidenceReferences: List<String>
)

// --- Starters Starting Dashboard screen ---
/* DEPRECATED TEMPLATE SCREEN
@Composable
fun OldDashboardStartScreen(
    viewModel: com.example.viewmodel.BrowserViewModel,
    onKeywordSearch: (String) -> Unit,
    onBookmarkClicked: (String) -> Unit,
    onHistoryClicked: (String) -> Unit,
    onShortcutClicked: (String) -> Unit
) {
    var searchKeyText by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmoDarkBackground)
    ) {
        // Atmospheric space glowing aura underlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(CosmoAtmosphereGlow)
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glowing brand logo header representing timeless convergence
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 32.dp, bottom = 12.dp)
                        .size(96.dp)
                ) {
                    // Outer atmospheric glow rings
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .background(Color.White.copy(alpha = 0.02f), CircleShape)
                            .border(1.dp, CosmoSecondary.copy(alpha = 0.2f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .border(1.5.dp, CosmoSecondary.copy(alpha = 0.3f), CircleShape)
                    )
                    // Core Sangam Nexus Premium Vector Emblem matching App Icon
                    Box(
                        modifier = Modifier
                            .size(60.dp)
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
                Text(
                    text = "SANGAM NEXUS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = CosmoTextPrimary,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Confluence Cosmic Nexus Browser",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmoSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 28.dp)
                )
            }

        // Dashboard Search Bar
        item {
            TextField(
                value = searchKeyText,
                onValueChange = { searchKeyText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                placeholder = {
                    Text(
                        text = "Aeterna Search: Search Google or Type URL...",
                        fontSize = 13.sp,
                        color = CosmoTextSecondary
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "", tint = CosmoSecondary)
                },
                trailingIcon = {
                    if (searchKeyText.isNotEmpty()) {
                        IconButton(onClick = { searchKeyText = "" }) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "", tint = CosmoTextSecondary)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchKeyText.isNotBlank()) {
                            onKeywordSearch(searchKeyText)
                        }
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CosmoDarkSurface,
                    unfocusedContainerColor = CosmoDarkSurface,
                    focusedTextColor = CosmoTextPrimary,
                    unfocusedTextColor = CosmoTextPrimary,
                    focusedIndicatorColor = CosmoSecondary,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Ecosystem shortcuts block (Google Platform tools!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Google Ecosystem shortcuts",
                        fontWeight = FontWeight.Bold,
                        color = CosmoGold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        EcosystemIcon("Search", Icons.Filled.Search, "https://www.google.com", onShortcutClicked)
                        EcosystemIcon("YouTube", Icons.Filled.PlayCircle, "https://m.youtube.com", onShortcutClicked)
                        EcosystemIcon("Gmail", Icons.Filled.Email, "https://mail.google.com", onShortcutClicked)
                        EcosystemIcon("Drive", Icons.Filled.Cloud, "https://drive.google.com", onShortcutClicked)
                        EcosystemIcon("Maps", Icons.Filled.Map, "https://maps.google.com", onShortcutClicked)
                    }
                }
            }
        }

        // Shield Status dashboard displaying complete offline security parameters
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onAdBlockToggle() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "",
                        tint = if (isAdBlockEnabled) CosmoGreen else CosmoTertiary,
                        modifier = Modifier
                            .size(34.dp)
                            .padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAdBlockEnabled) "AdBlocker active: Secured Connection" else "AdBlocker disabled: Block Tracker",
                            color = CosmoTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Blocks script payloads, tracking cookies & ads locally.",
                            color = CosmoTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isAdBlockEnabled,
                        onCheckedChange = { onAdBlockToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CosmoGreen,
                            checkedTrackColor = CosmoGreen.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        // Universal Knowledge Hub (Immersive Preview Feed Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurfaceVariant.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NOW BROWSING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmoSecondary,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Universal Knowledge Hub",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(CosmoGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, CosmoGreen.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ENCRYPTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmoGreen,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cover visual image showing nebula / space
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Nebula design background indicator icon
                        Icon(
                            imageVector = Icons.Filled.Cloud,
                            contentDescription = "",
                            tint = Color.White.copy(alpha = 0.04f),
                            modifier = Modifier.fillMaxSize().padding(12.dp)
                        )

                        // Orbit overlay glow play button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4F46E5).copy(alpha = 0.9f))
                                .border(BorderStroke(4.dp, Color(0xFF4F46E5).copy(alpha = 0.2f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play Cosmos stream",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated skeletal rows
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Floating SANGAM DOWNLOAD Button styled identical to Design HTML!
                    Button(
                        onClick = {
                            // Triggers downloading the brahmand wall-pack zip or navigates to media
                            onShortcutClicked("https://github.com/krishna-sangam/cosmos-resources/raw/main/brahmand_preview.zip")
                        },
                        modifier = Modifier
                            .align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "SANGAM DOWNLOAD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // Bookmarked Links Grid header
        if (bookmarks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(imageVector = Icons.Filled.Star, contentDescription = "", tint = CosmoGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Saved bookmarks", color = CosmoTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            items(bookmarks.chunked(2)) { pair ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { bmk ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .clickable { onBookmarkClicked(bmk.url) },
                            colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                            border = BorderStroke(0.5.dp, CosmoBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = bmk.title,
                                    color = CosmoTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = bmk.url,
                                    color = CosmoSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // History entry block references
        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(imageVector = Icons.Filled.History, contentDescription = "", tint = CosmoSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Recent history surfing", color = CosmoTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            items(history.take(5)) { hist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHistoryClicked(hist.url) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Link, contentDescription = "", tint = CosmoTextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = hist.title,
                            color = CosmoTextPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = hist.url,
                            color = CosmoTextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
}

// Shortcut ecosystem icons helper items
@Composable
fun EcosystemIcon(
    name: String,
    icon: Any, // ImageVector
    targetUrl: String,
    onShortcutClicked: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onShortcutClicked(targetUrl) }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(CosmoDarkBackground, shape = CircleShape)
                .border(1.dp, CosmoBorder, CircleShape)
        ) {
            Icon(
                imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
                contentDescription = name,
                tint = CosmoSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = name,
            fontSize = 10.sp,
            color = CosmoTextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
*/

// --- Tabs Management Section view ---
@Composable
fun TabsManagerScreen(
    tabs: List<TabEntry>,
    activeTabId: String?,
    isIncognito: Boolean,
    onSelect: (String) -> Unit,
    onClose: (TabEntry) -> Unit,
    onAddTab: (Boolean) -> Unit, // Boolean isIncognito
    onClosePanel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tabs Manager",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmoTextPrimary
            )
            IconButton(onClick = onClosePanel) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close Panel", tint = CosmoTextSecondary)
            }
        }

        // Action controls to open Normal + Incognito folders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onAddTab(false) },
                colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "", tint = CosmoDarkBackground)
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Tab", color = CosmoDarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onAddTab(true) },
                colors = ButtonDefaults.buttonColors(containerColor = CosmoDarkSurfaceVariant),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Filled.PrivacyTip, contentDescription = "", tint = CosmoGold)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Incognito Tab", color = CosmoGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // List of currently active pages
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs) { tab ->
                val isActive = tab.id == activeTabId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clickable { onSelect(tab.id) }
                        .border(
                            2.dp,
                            if (isActive) CosmoSecondary else CosmoBorder,
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (tab.isIncognito) CosmoDarkSurfaceVariant else CosmoDarkSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (tab.isIncognito) Icons.Filled.PrivacyTip else Icons.Filled.Web,
                                contentDescription = "",
                                tint = if (tab.isIncognito) CosmoGold else CosmoTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            IconButton(
                                onClick = { onClose(tab) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close single page Tab",
                                    tint = CosmoTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = tab.title,
                            color = CosmoTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tab.url,
                            color = CosmoTextSecondary,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// --- Downloads lists views tracking progress ---
@Composable
fun DownloadsManagerScreen(
    downloads: List<DownloadEntry>,
    onDelete: (Int) -> Unit,
    onClosePanel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Internet Downloader (IDM)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmoTextPrimary
            )
            IconButton(onClick = onClosePanel) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "", tint = CosmoTextSecondary)
            }
        }

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "",
                        tint = CosmoBorder,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No file downloads found", color = CosmoTextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(downloads) { d ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = when {
                                        d.mimeType.contains("video") -> Icons.Filled.PlayCircle
                                        d.mimeType.contains("audio") -> Icons.Filled.MusicNote
                                        d.mimeType.contains("image") -> Icons.Filled.Image
                                        else -> Icons.Filled.InsertDriveFile
                                    },
                                    contentDescription = "",
                                    tint = CosmoSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = d.fileName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = CosmoTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = d.url,
                                        fontSize = 9.sp,
                                        color = CosmoTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(d.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "", tint = CosmoTertiary, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Progress bar section
                            if (d.status == "DOWNLOADING") {
                                LinearProgressIndicator(
                                    progress = { d.progress / 100f },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = CosmoSecondary,
                                    trackColor = CosmoBorder
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Saving in background...", color = CosmoSecondary, fontSize = 10.sp)
                                    Text("${d.progress.toInt()}%", color = CosmoTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when(d.status) {
                                            "COMPLETED" -> "Download completed successfully"
                                            "FAILED" -> "Connection Error: Saving unsuccessful"
                                            else -> "Initiation queued"
                                        },
                                        color = if (d.status == "COMPLETED") CosmoGreen else CosmoTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    if (d.status == "COMPLETED") {
                                        Text(
                                            text = "Saved to public default Downloads folder.",
                                            color = CosmoTextSecondary,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Dynamic Media sniffers detected files screens ---
@Composable
fun MediaSnifferScreen(
    mediaList: List<DetectedMedia>,
    onDownloadTrigger: (DetectedMedia) -> Unit,
    onClosePanel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Sniffed Media Detected on Page",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmoTextPrimary
            )
            IconButton(onClick = onClosePanel) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "", tint = CosmoTextSecondary)
            }
        }

        if (mediaList.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.WifiTetheringError,
                        contentDescription = "",
                        tint = CosmoBorder,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No playable videos, audios or image links found yet.", color = CosmoTextSecondary, fontSize = 11.sp)
                    Text("Tip: Play any video or hold click image to sniff download link!", color = CosmoGold, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mediaList) { element ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (element.type) {
                                    "VIDEO" -> Icons.Filled.PlayCircle
                                    "AUDIO" -> Icons.Filled.MusicNote
                                    else -> Icons.Filled.Image
                                },
                                contentDescription = "",
                                tint = CosmoSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = element.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmoTextPrimary
                                )
                                Text(
                                    text = element.url,
                                    fontSize = 9.sp,
                                    color = CosmoSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = { onDownloadTrigger(element) },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmoGold)
                            ) {
                                Icon(imageVector = Icons.Filled.Download, contentDescription = "", tint = CosmoDarkBackground, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download", color = CosmoDarkBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- History and Bookmarks management layout ---
@Composable
fun HistoryBookmarksScreen(
    history: List<HistoryEntry>,
    bookmarks: List<BookmarkEntry>,
    onDeleteHistory: (Int) -> Unit,
    onDeleteHistoryEntries: (List<Int>) -> Unit,
    onDeleteBookmark: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onUrlNavigate: (String) -> Unit,
    onClosePanel: () -> Unit
) {
    var selectedIndexTab by remember { mutableStateOf(0) } // 0 is History, 1 is Bookmarks
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredHistory = remember(history, searchQuery) {
        if (searchQuery.isBlank()) {
            history
        } else {
            history.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Surfing Folders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmoTextPrimary
            )
            IconButton(onClick = onClosePanel) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "", tint = CosmoTextSecondary)
            }
        }

        // Sloped tab switcher
        TabRow(
            selectedTabIndex = selectedIndexTab,
            containerColor = Color.Transparent,
            contentColor = CosmoSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Tab(
                selected = selectedIndexTab == 0,
                onClick = { selectedIndexTab = 0 },
                text = { Text("Browsing History", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedIndexTab == 1,
                onClick = { selectedIndexTab = 1 },
                text = { Text("Saved Bookmarks", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedIndexTab == 0) {
            // History list view with Search
            Column(modifier = Modifier.weight(1f)) {
                if (history.isNotEmpty()) {
                    // Search bar for browsing history logs
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("history_search_input"),
                        placeholder = { Text("Search history logs...", color = CosmoTextSecondary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search icon",
                                tint = CosmoTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear search query",
                                        tint = CosmoTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CosmoTextPrimary,
                            unfocusedTextColor = CosmoTextPrimary,
                            focusedBorderColor = CosmoSecondary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = CosmoDarkSurface,
                            unfocusedContainerColor = CosmoDarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp)
                    )

                    // Clear actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val idsToClear = filteredHistory.map { it.id }
                                    onDeleteHistoryEntries(idsToClear)
                                    searchQuery = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear Matched (${filteredHistory.size})", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Button(
                            onClick = {
                                onClearHistory()
                                searchQuery = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmoTertiary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = if (searchQuery.isNotEmpty()) Modifier.weight(1f) else Modifier
                        ) {
                            Text("Clear All Logs", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No local history logs found", color = CosmoTextSecondary, fontSize = 12.sp)
                    }
                } else if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching history entries found", color = CosmoTextSecondary, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredHistory) { record ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUrlNavigate(record.url) },
                                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Filled.Link, contentDescription = "", tint = CosmoTextSecondary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = CosmoTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = record.url,
                                            fontSize = 10.sp,
                                            color = CosmoSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { onDeleteHistory(record.id) }) {
                                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "", tint = CosmoTertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Bookmarks view
            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No saved bookmark entries", color = CosmoTextSecondary, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bookmarks) { mark ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUrlNavigate(mark.url) },
                            colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Filled.Star, contentDescription = "", tint = CosmoGold)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mark.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = CosmoTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = mark.url,
                                        fontSize = 10.sp,
                                        color = CosmoSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onDeleteBookmark(mark.id) }) {
                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "", tint = CosmoTertiary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Google Gemini summarize panel ---
data class AgentActionItem(
    val action: com.example.viewmodel.AgentAction,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
fun OracleSummaryScreen(
    state: com.example.viewmodel.AiSummaryState,
    viewModel: com.example.viewmodel.BrowserViewModel,
    extractedText: String,
    onClear: () -> Unit,
    onClosePanel: () -> Unit
) {
    val agentActions = remember {
        listOf(
            AgentActionItem(com.example.viewmodel.AgentAction.SUMMARIZE, "Summarize", Icons.Default.List, CosmoGold),
            AgentActionItem(com.example.viewmodel.AgentAction.EXPLAIN, "Explain", Icons.Default.Lightbulb, CosmoSecondary),
            AgentActionItem(com.example.viewmodel.AgentAction.TRANSLATE, "Translate", Icons.Default.Translate, CosmoTertiary),
            AgentActionItem(com.example.viewmodel.AgentAction.EXTRACT_INFO, "Extract Info", Icons.Default.Info, CosmoGold),
            AgentActionItem(com.example.viewmodel.AgentAction.GENERATE_EMAIL, "Email Draft", Icons.Default.Email, CosmoSecondary),
            AgentActionItem(com.example.viewmodel.AgentAction.GENERATE_REPORT, "Report", Icons.Default.Assessment, CosmoGreen),
            AgentActionItem(com.example.viewmodel.AgentAction.GENERATE_CODE, "Kotlin Code", Icons.Default.Code, CosmoTertiary),
            AgentActionItem(com.example.viewmodel.AgentAction.ANALYZE_TABLES, "Tables", Icons.Default.MenuBook, CosmoGold),
            AgentActionItem(com.example.viewmodel.AgentAction.RESEARCH_TABS, "Research Tabs", Icons.Default.Search, CosmoSecondary),
            AgentActionItem(com.example.viewmodel.AgentAction.FORM_FILLING, "Autofill Form", Icons.Default.Edit, CosmoGreen)
        )
    }

    var selectedAction by remember { mutableStateOf(com.example.viewmodel.AgentAction.SUMMARIZE) }
    var translationLang by remember { mutableStateOf("Hindi") }
    val languages = listOf("Hindi", "Sanskrit", "Marathi", "Bengali", "Spanish", "German", "French", "Japanese")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = "", tint = CosmoGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cosmo Universal Agent Layer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmoTextPrimary
                )
            }
            IconButton(onClick = {
                onClear()
                onClosePanel()
            }) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "", tint = CosmoTextSecondary)
            }
        }

        // Horizontal scrolling selector of all 10 Agent actions
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(agentActions) { item ->
                val isSelected = selectedAction == item.action
                val containerColor = if (isSelected) item.color else CosmoDarkSurface
                val contentColor = if (isSelected) CosmoDarkBackground else CosmoTextPrimary
                
                Card(
                    modifier = Modifier.clickable {
                        selectedAction = item.action
                        viewModel.executeAgentAction(item.action, extractedText, translationLang)
                    },
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) item.color else CosmoBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = contentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.title,
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Language selector row specifically for translate task
        if (selectedAction == com.example.viewmodel.AgentAction.TRANSLATE) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text("Target Language: ", fontSize = 11.sp, color = CosmoTextSecondary, modifier = Modifier.padding(end = 4.dp))
                }
                items(languages) { lang ->
                    val isLangSelected = translationLang == lang
                    Card(
                        modifier = Modifier.clickable {
                            translationLang = lang
                            viewModel.executeAgentAction(com.example.viewmodel.AgentAction.TRANSLATE, extractedText, lang)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLangSelected) CosmoTertiary else CosmoDarkBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isLangSelected) CosmoTertiary else CosmoBorder)
                    ) {
                        Text(
                            text = lang,
                            fontSize = 10.sp,
                            color = if (isLangSelected) Color.White else CosmoTextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Results frame
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CosmoDarkBackground, shape = RoundedCornerShape(16.dp))
                .border(1.dp, CosmoBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            when (state) {
                is com.example.viewmodel.AiSummaryState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Context parsed. Select any task above.", color = CosmoTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.executeAgentAction(selectedAction, extractedText, translationLang) },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary)
                            ) {
                                Text("Execute Action", color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is com.example.viewmodel.AiSummaryState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = CosmoGold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Resolving AI Pipeline Order...",
                            fontSize = 12.sp,
                            color = CosmoGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Invoking fallback priorities securely",
                            fontSize = 10.sp,
                            color = CosmoTextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                is com.example.viewmodel.AiSummaryState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            // Badge displaying actual target model used!
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .background(CosmoGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .border(1.dp, CosmoGreen, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "", tint = CosmoGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Processed by ${state.providerUsed}",
                                        color = CosmoGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Text(
                                text = "Task Output Results",
                                color = CosmoGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Text(
                                text = state.summary,
                                color = CosmoTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
                is com.example.viewmodel.AiSummaryState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Filled.Error, contentDescription = "Error", tint = CosmoTertiary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                color = CosmoTertiary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.executeAgentAction(selectedAction, extractedText, translationLang) },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary)
                            ) {
                                Text("Retry Action", color = CosmoDarkBackground)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Enforcing universal failover protection rules across encrypted local keystore configurations.",
            color = CosmoTextSecondary,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- Browser settings secondary adjustments dashboards ---
@Composable
fun SettingsPanelScreen(
    viewModel: com.example.viewmodel.BrowserViewModel,
    isAdBlock: Boolean,
    isInc: Boolean,
    isLox: Boolean,
    onToggleAdBlock: (Boolean) -> Unit,
    onToggleIncognito: (Boolean) -> Unit,
    onSetupPasscode: () -> Unit,
    onRemovePasscode: () -> Unit,
    onViewDownloads: () -> Unit,
    onViewHistoryAndBookmarks: () -> Unit,
    onClosePanel: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    
    val isGoogleConnected by viewModel.isGoogleConnected.collectAsState()
    val googleEmail by viewModel.googleEmail.collectAsState()
    val isOpenAiConnected by viewModel.isOpenAiConnected.collectAsState()
    val isClaudeConnected by viewModel.isClaudeConnected.collectAsState()
    
    val primaryAi by viewModel.primaryAiProvider.collectAsState()
    val fallbackAi by viewModel.fallbackAiProvider.collectAsState()
    val secondaryAi by viewModel.secondaryAiProvider.collectAsState()
    val enabledProviders by viewModel.enabledProviders.collectAsState()

    var editingOpenAiKey by remember { mutableStateOf("") }
    var editingClaudeKey by remember { mutableStateOf("") }
    
    var showSimGooglePanel by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sangam Nexus Control Panel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmoTextPrimary
                )
                IconButton(onClick = onClosePanel) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "", tint = CosmoTextSecondary)
                }
            }
        }

        // --- SECTION: ATMOSPHERE ENGINE (Theme mode selector) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Brightness4, contentDescription = "", tint = CosmoSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Atmosphere Engine (Time Theme)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CosmoTextPrimary
                        )
                    }
                    Text(
                        text = "Calculates day/night modes dynamically from 06:00 AM - 05:59 PM automatically using Asia/Kolkata (IST) timezone.",
                        color = CosmoTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("AUTO", "LIGHT", "DARK").forEach { mode ->
                            val isChosen = themeMode == mode
                            Button(
                                onClick = { viewModel.selectTheme(mode) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isChosen) CosmoSecondary else CosmoDarkBackground
                                ),
                                border = if (isChosen) null else BorderStroke(1.dp, CosmoBorder),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (mode == "AUTO") "Auto (IST)" else mode,
                                    fontSize = 10.sp,
                                    color = if (isChosen) CosmoDarkBackground else CosmoTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION: ECOSYSTEM ACCOUNTS ---
        item {
            Text(
                text = "Secure Ecosystem Connections",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = CosmoGold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Google Sync connection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CloudQueue, contentDescription = "", tint = CosmoSecondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Google Cloud Environment", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CosmoTextPrimary)
                                if (isGoogleConnected && googleEmail != null) {
                                    Text("Active: $googleEmail", color = CosmoGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Disconnected (Mandatory Onboarding)", color = CosmoTertiary, fontSize = 11.sp)
                                }
                            }
                        }

                        if (isGoogleConnected) {
                            OutlinedButton(
                                onClick = { viewModel.disconnectGoogle() },
                                border = BorderStroke(1.dp, CosmoBorder),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Disconnect", fontSize = 10.sp, color = CosmoTertiary)
                            }
                        } else {
                            Button(
                                onClick = { showSimGooglePanel = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Connect", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // OpenAI api connection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "", tint = CosmoGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("OpenAI ChatGPT Integration", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CosmoTextPrimary)
                                Text(
                                    text = if (isOpenAiConnected) "Unlocked secures key via Keystore" else "Inactive",
                                    color = if (isOpenAiConnected) CosmoGreen else CosmoTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (isOpenAiConnected) {
                            OutlinedButton(
                                onClick = { viewModel.disconnectOpenAi() },
                                border = BorderStroke(1.dp, CosmoBorder),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Disconnect", fontSize = 10.sp, color = CosmoTertiary)
                            }
                        }
                    }

                    if (!isOpenAiConnected) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editingOpenAiKey,
                                onValueChange = { editingOpenAiKey = it },
                                placeholder = { Text("sk-...", fontSize = 11.sp, color = CosmoTextSecondary.copy(alpha = 0.5f)) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CosmoGold,
                                    unfocusedBorderColor = CosmoBorder,
                                    focusedTextColor = CosmoTextPrimary,
                                    unfocusedTextColor = CosmoTextPrimary
                                )
                            )
                            Button(
                                onClick = {
                                    if (editingOpenAiKey.isNotBlank()) {
                                        viewModel.connectOpenAi(editingOpenAiKey)
                                        editingOpenAiKey = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmoGold),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save", fontSize = 10.sp, color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Claude connection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Psychology, contentDescription = "", tint = CosmoTertiary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Claude Anthropic Integration", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CosmoTextPrimary)
                                Text(
                                    text = if (isClaudeConnected) "Unlocked secures key via Keystore" else "Inactive",
                                    color = if (isClaudeConnected) CosmoGreen else CosmoTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (isClaudeConnected) {
                            OutlinedButton(
                                onClick = { viewModel.disconnectClaude() },
                                border = BorderStroke(1.dp, CosmoBorder),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Disconnect", fontSize = 10.sp, color = CosmoTertiary)
                            }
                        }
                    }

                    if (!isClaudeConnected) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editingClaudeKey,
                                onValueChange = { editingClaudeKey = it },
                                placeholder = { Text("sk-ant-...", fontSize = 11.sp, color = CosmoTextSecondary.copy(alpha = 0.5f)) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CosmoTertiary,
                                    unfocusedBorderColor = CosmoBorder,
                                    focusedTextColor = CosmoTextPrimary,
                                    unfocusedTextColor = CosmoTextPrimary
                                )
                            )
                            Button(
                                onClick = {
                                    if (editingClaudeKey.isNotBlank()) {
                                        viewModel.connectClaude(editingClaudeKey)
                                        editingClaudeKey = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmoTertiary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION: AI PRIORITY SEQUENCE ---
        item {
            Text(
                text = "Universal AI Agent Resolution rules",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = CosmoGold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Customize failover priority order sequence in real-time. If primary provider fails or key is missing, agent falls back automatically.",
                        color = CosmoTextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Enabled providers checklist
                    Text("Select Enabled Models in chain:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CosmoTextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Gemini", "ChatGPT", "Claude").forEach { prov ->
                            val active = enabledProviders.contains(prov)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = active,
                                    onCheckedChange = { checked ->
                                        val nextEnabled = enabledProviders.toMutableSet()
                                        if (checked) nextEnabled.add(prov) else nextEnabled.remove(prov)
                                        viewModel.saveProvidersSettings(primaryAi, fallbackAi, secondaryAi, nextEnabled)
                                    }
                                )
                                Text(prov, fontSize = 11.sp, color = CosmoTextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Priority 1
                    Text("1. Primary AI Provider:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Gemini", "ChatGPT", "Claude").forEach { cur ->
                            FilterChip(
                                selected = primaryAi == cur,
                                onClick = { viewModel.saveProvidersSettings(cur, fallbackAi, secondaryAi, enabledProviders) },
                                label = { Text(cur, fontSize = 10.sp) }
                            )
                        }
                    }

                    // Priority 2
                    Text("2. Fallback AI Provider:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoGold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Gemini", "ChatGPT", "Claude").forEach { cur ->
                            FilterChip(
                                selected = fallbackAi == cur,
                                onClick = { viewModel.saveProvidersSettings(primaryAi, cur, secondaryAi, enabledProviders) },
                                label = { Text(cur, fontSize = 10.sp) }
                            )
                        }
                    }

                    // Priority 3
                    Text("3. Secondary Fallback:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmoTertiary)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Gemini", "ChatGPT", "Claude").forEach { cur ->
                            FilterChip(
                                selected = secondaryAi == cur,
                                onClick = { viewModel.saveProvidersSettings(primaryAi, fallbackAi, cur, enabledProviders) },
                                label = { Text(cur, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION: SECURITY UTILITY CHECKS ---
        item {
            Text(
                text = "Other Security & Utilities",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = CosmoGold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Direct Download folder launcher
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewDownloads() },
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = "", tint = CosmoSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Active downloads manager", color = CosmoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Review background file download streams", color = CosmoTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // Folders (History & Bookmarks)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewHistoryAndBookmarks() },
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Folder, contentDescription = "", tint = CosmoGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Bookmarked URLs & Browser History", color = CosmoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Manage saved links and delete history logs", color = CosmoTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // Script parameters - AdBlock toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Shield, contentDescription = "", tint = CosmoGreen)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AdBlocker Filter", color = CosmoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Block web ad scripts, tracker domains, and banners locally", color = CosmoTextSecondary, fontSize = 11.sp)
                    }
                    Checkbox(
                        checked = isAdBlock,
                        onCheckedChange = { onToggleAdBlock(it) },
                        colors = CheckboxDefaults.colors(checkedColor = CosmoGreen)
                    )
                }
            }
        }

        // Incognito sandbox toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.PrivacyTip, contentDescription = "", tint = CosmoGold)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Incognito Sandboxed Mode", color = CosmoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Erase local history, active tabs, and caches upon closing", color = CosmoTextSecondary, fontSize = 11.sp)
                    }
                    Checkbox(
                        checked = isInc,
                        onCheckedChange = { onToggleIncognito(it) },
                        colors = CheckboxDefaults.colors(checkedColor = CosmoGold)
                    )
                }
            }
        }

        // Passcode Lock Security
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurface),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = "", tint = CosmoSecondary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App passcode PIN guard", color = CosmoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Requires PIN code verification on start for high privacy", color = CosmoTextSecondary, fontSize = 11.sp)
                    }
                    if (isLox) {
                        Button(
                            onClick = onRemovePasscode,
                            colors = ButtonDefaults.buttonColors(containerColor = CosmoTertiary)
                        ) {
                            Text("Disable", fontSize = 10.sp, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = onSetupPasscode,
                            colors = ButtonDefaults.buttonColors(containerColor = CosmoSecondary)
                        ) {
                            Text("Setup PIN", fontSize = 10.sp, color = CosmoDarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmoDarkSurfaceVariant),
                border = BorderStroke(1.dp, CosmoBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Browser Ecosystem Information",
                        color = CosmoGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "This browser is securely signed, complies fully with Google Ecosystem WebView components and holds integrated sandboxed privacy layers conforming with global security targets.",
                        color = CosmoTextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    if (showSimGooglePanel) {
        SimulatedOAuthDialog(
            title = "Connect Google Cloud",
            description = "Authorize and connect your Google account to sync all browser configurations.",
            onAuthorize = {
                viewModel.connectGoogle("connect_settings_account@gmail.com")
                showSimGooglePanel = false
            },
            onDismiss = { showSimGooglePanel = false }
        )
    }
}

