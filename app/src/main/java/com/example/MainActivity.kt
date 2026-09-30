package com.example

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing.RevenueCatManager
import com.example.data.model.ItemType
import com.example.service.BreadcrumbAccessibilityService
import com.example.ui.components.FloatingMascotOverlay
import com.example.ui.components.SaveFlowSheet
import com.example.ui.components.ScreenSnipDialog
import com.example.ui.screens.CustomiseScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.UpgradeScreen
import com.example.ui.theme.BreadcrumbTheme
import com.example.ui.viewmodel.CaptureViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MainDestination {
    HOME,
    UPGRADE,
    CUSTOMISE
}

class MainActivity : ComponentActivity() {

    private val viewModel: CaptureViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /**
     * Captures a crisp, hardware-accelerated screenshot of the current window using PixelCopy
     * (with software Canvas fallback) after hiding the floating mascot overlay.
     */
    private fun captureCleanWindowBitmap(onComplete: (Bitmap?) -> Unit) {
        try {
            val rootView = window.decorView.rootView
            val width = rootView.width.coerceAtLeast(1)
            val height = rootView.height.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    PixelCopy.request(
                        window,
                        bitmap,
                        { copyResult ->
                            if (copyResult == PixelCopy.SUCCESS) {
                                onComplete(bitmap)
                            } else {
                                val canvas = Canvas(bitmap)
                                rootView.draw(canvas)
                                onComplete(bitmap)
                            }
                        },
                        Handler(Looper.getMainLooper())
                    )
                } catch (_: Exception) {
                    val canvas = Canvas(bitmap)
                    rootView.draw(canvas)
                    onComplete(bitmap)
                }
            } else {
                val canvas = Canvas(bitmap)
                rootView.draw(canvas)
                onComplete(bitmap)
            }
        } catch (_: Exception) {
            onComplete(null)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialDestination = if (intent.getStringExtra("navigate_to") == "upgrade") {
            MainDestination.UPGRADE
        } else {
            MainDestination.HOME
        }

        setContent {
            BreadcrumbTheme {
                val coroutineScope = rememberCoroutineScope()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val isAccessibilityServiceConnected by BreadcrumbAccessibilityService.isServiceConnected.collectAsStateWithLifecycle()
                val detectedSelectedText by BreadcrumbAccessibilityService.detectedSelectedText.collectAsStateWithLifecycle()
                var currentDestination by rememberSaveable { mutableStateOf(initialDestination) }
                val revenueCatManager = remember { RevenueCatManager.getInstance(applicationContext) }

                // State for Image Snip Tool and Save Flow launched from the Floating Mascot Overlay
                var isCapturingScreen by remember { mutableStateOf(false) }
                var showSnipDialog by remember { mutableStateOf(false) }
                var snipSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }

                var showOverlaySaveSheet by remember { mutableStateOf(false) }
                var overlaySaveType by remember { mutableStateOf(ItemType.IMAGE) }
                var overlaySaveContent by remember { mutableStateOf("") }
                var overlaySaveBitmap by remember { mutableStateOf<Bitmap?>(null) }

                // Register clean window capture fallback for AccessibilityService rapid-snip rate limits
                DisposableEffect(Unit) {
                    BreadcrumbAccessibilityService.inAppWindowCaptureProvider = { callback ->
                        captureCleanWindowBitmap(callback)
                    }
                    onDispose {
                        BreadcrumbAccessibilityService.inAppWindowCaptureProvider = null
                    }
                }

                val triggerSnipTool: () -> Unit = {
                    val handledByService = BreadcrumbAccessibilityService.requestScreenshotSnipFromService()
                    if (!handledByService) {
                        // Hide the in-app floating mascot first so it never appears inside the screenshot
                        isCapturingScreen = true
                        coroutineScope.launch {
                            delay(65L)
                            captureCleanWindowBitmap { captured ->
                                snipSourceBitmap = captured
                                isCapturingScreen = false
                                showSnipDialog = true
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentDestination) {
                        MainDestination.HOME -> {
                            HomeScreen(
                                uiState = uiState,
                                onSortChange = viewModel::setSortOption,
                                onSearchChange = viewModel::setSearchQuery,
                                onTagSelect = viewModel::setSelectedTag,
                                onToggleSelect = viewModel::toggleSelect,
                                onClearSelection = viewModel::clearSelection,
                                onSelectAll = viewModel::selectAll,
                                onDeleteSelected = viewModel::deleteSelected,
                                onUpdateCapture = { capture, newCtx, newTag ->
                                    viewModel.updateCapture(capture, newCtx, newTag)
                                },
                                onDeleteCapture = viewModel::deleteCapture,
                                onSaveTextCapture = { content, contextText, tag ->
                                    viewModel.saveTextCapture(content, contextText, tag)
                                },
                                onSaveMultipleImages = { uris, contextText, tag ->
                                    viewModel.saveMultipleImages(uris, contextText, tag)
                                },
                                onSaveBitmap = { bitmap, contextText, tag ->
                                    viewModel.saveBitmapCapture(bitmap, contextText, tag)
                                },
                                onTriggerSnip = triggerSnipTool,
                                onNavigateToUpgrade = { currentDestination = MainDestination.UPGRADE },
                                onNavigateToCustomise = { currentDestination = MainDestination.CUSTOMISE },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        MainDestination.UPGRADE -> {
                            UpgradeScreen(
                                isPro = uiState.isPro,
                                onUpgrade = {
                                    revenueCatManager.purchasePro(this@MainActivity as Activity) { success, err ->
                                        if (success) {
                                            Toast.makeText(this@MainActivity, "Upgraded to Pro!", Toast.LENGTH_SHORT).show()
                                        } else if (err != null) {
                                            Toast.makeText(this@MainActivity, err, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onRestore = {
                                    revenueCatManager.restorePurchases { _, msg ->
                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                onBack = { currentDestination = MainDestination.HOME },
                                billingStatus = revenueCatManager.getBillingStatusDescription(),
                                onResetPro = {
                                    revenueCatManager.resetProForTesting()
                                    Toast.makeText(this@MainActivity, "Reset to Free", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        MainDestination.CUSTOMISE -> {
                            CustomiseScreen(
                                floatingOverlayEnabled = uiState.floatingOverlayEnabled,
                                onToggleFloatingOverlay = viewModel::setFloatingOverlayEnabled,
                                textSelectionEnabled = uiState.textSelectionEnabled,
                                onToggleTextSelection = viewModel::setTextSelection,
                                onLaunchSnipTool = triggerSnipTool,
                                onBack = { currentDestination = MainDestination.HOME },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Draggable Floating Mascot Overlay (or standalone Text Selection Pill when mascot is off)
                    val showPillWhenMascotOff = uiState.textSelectionEnabled && !detectedSelectedText.isNullOrBlank()
                    FloatingMascotOverlay(
                        visible = (uiState.floatingOverlayEnabled || showPillWhenMascotOff) &&
                            !isAccessibilityServiceConnected &&
                            !showSnipDialog &&
                            !isCapturingScreen,
                        mascotVisible = uiState.floatingOverlayEnabled,
                        detectedSelectedText = if (uiState.textSelectionEnabled) detectedSelectedText else null,
                        onMascotClick = triggerSnipTool,
                        onSaveSelectedTextClick = { selectedText ->
                            BreadcrumbAccessibilityService.clearDetectedSelection()
                            val trimmed = selectedText.trim()
                            overlaySaveType = if (trimmed.startsWith("http://", ignoreCase = true) ||
                                trimmed.startsWith("https://", ignoreCase = true)
                            ) {
                                ItemType.URL
                            } else {
                                ItemType.TEXT
                            }
                            overlaySaveContent = trimmed
                            overlaySaveBitmap = null
                            showOverlaySaveSheet = true
                        }
                    )
                }

                // Image Snip Tool Dialog launched by clicking the Floating Mascot Overlay
                if (showSnipDialog) {
                    ScreenSnipDialog(
                        initialBitmap = snipSourceBitmap,
                        onDismiss = { showSnipDialog = false },
                        onSnipConfirmed = { croppedBitmap ->
                            showSnipDialog = false
                            overlaySaveType = ItemType.IMAGE
                            overlaySaveContent = "Screen Snip"
                            overlaySaveBitmap = croppedBitmap
                            showOverlaySaveSheet = true
                        }
                    )
                }

                // Universal Save Flow Sheet for Snipped Image or Accessibility-detected Selected Text
                if (showOverlaySaveSheet) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = {
                            showOverlaySaveSheet = false
                            overlaySaveBitmap = null
                        },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.background
                    ) {
                        SaveFlowSheet(
                            itemType = overlaySaveType,
                            initialContent = overlaySaveContent,
                            initialImageUri = null,
                            initialBitmap = overlaySaveBitmap,
                            existingTags = uiState.allTags,
                            isPro = uiState.isPro,
                            onSave = { savedContent, contextText, tag ->
                                val bmp = overlaySaveBitmap
                                if (bmp != null) {
                                    viewModel.saveBitmapCapture(bmp, contextText, tag)
                                } else {
                                    viewModel.saveTextCapture(savedContent, contextText, tag)
                                }
                                showOverlaySaveSheet = false
                                overlaySaveBitmap = null
                                Toast.makeText(this@MainActivity, "Saved to Breadcrumb", Toast.LENGTH_SHORT).show()
                            },
                            onDiscard = {
                                showOverlaySaveSheet = false
                                overlaySaveBitmap = null
                            },
                            onNavigateToUpgrade = {
                                showOverlaySaveSheet = false
                                currentDestination = MainDestination.UPGRADE
                            }
                        )
                    }
                }
            }
        }
    }
}
