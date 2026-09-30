package com.example

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CaptureRepository
import com.example.data.SettingsManager
import com.example.data.model.ItemType
import com.example.service.BreadcrumbAccessibilityService
import com.example.ui.components.SaveFlowSheet
import com.example.ui.components.ScreenSnipDialog
import com.example.ui.theme.BreadcrumbTheme
import kotlinx.coroutines.launch

private data class ParsedCaptureIntent(
    val isLaunchSnipTool: Boolean,
    val itemType: ItemType,
    val initialContent: String,
    val initialUris: List<Uri>,
    val isProcessTextDisabled: Boolean = false
)

class CaptureActivity : ComponentActivity() {

    private var activeIntent by mutableStateOf<Intent?>(null)
    private var intentVersion by mutableIntStateOf(0)

    companion object {
        @Volatile
        var isCaptureOverlayVisible: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        isCaptureOverlayVisible = true
        activeIntent = intent
        intentVersion++
        setupComposeUI()
    }

    override fun onResume() {
        super.onResume()
        isCaptureOverlayVisible = true
    }

    override fun onPause() {
        super.onPause()
        isCaptureOverlayVisible = false
    }

    override fun onDestroy() {
        isCaptureOverlayVisible = false
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activeIntent = intent
        intentVersion++
    }

    private fun parseIncomingIntent(intent: Intent, textSelectionEnabled: Boolean): ParsedCaptureIntent {
        val action = intent.action
        val type = intent.type

        // 1. Launched from Floating Mascot Overlay -> Open Image Snip Tool
        if (intent.getBooleanExtra(BreadcrumbAccessibilityService.EXTRA_LAUNCH_SNIP_TOOL, false)) {
            return ParsedCaptureIntent(
                isLaunchSnipTool = true,
                itemType = ItemType.IMAGE,
                initialContent = "Snipped Region",
                initialUris = emptyList()
            )
        }

        // 2. Launched from Accessibility Service Text Selection Detection
        val accessibilitySelectedText = intent.getStringExtra(BreadcrumbAccessibilityService.EXTRA_ACCESSIBILITY_SELECTED_TEXT)
        if (!accessibilitySelectedText.isNullOrBlank()) {
            val trimmed = accessibilitySelectedText.trim()
            val detectedType = if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                ItemType.URL
            } else {
                ItemType.TEXT
            }
            return ParsedCaptureIntent(
                isLaunchSnipTool = false,
                itemType = detectedType,
                initialContent = trimmed,
                initialUris = emptyList()
            )
        }

        // 3. Android PROCESS_TEXT action (Text Selection Menu)
        if (action == Intent.ACTION_PROCESS_TEXT) {
            if (!textSelectionEnabled) {
                return ParsedCaptureIntent(
                    isLaunchSnipTool = false,
                    itemType = ItemType.TEXT,
                    initialContent = "",
                    initialUris = emptyList(),
                    isProcessTextDisabled = true
                )
            }
            val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                ?: intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
                ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
                ?: BreadcrumbAccessibilityService.detectedSelectedText.value
                ?: ""

            val trimmed = selectedText.trim()
            val detectedType = if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                ItemType.URL
            } else {
                ItemType.TEXT
            }
            return ParsedCaptureIntent(
                isLaunchSnipTool = false,
                itemType = detectedType,
                initialContent = trimmed,
                initialUris = emptyList()
            )
        }

        // 4. Android Share Sheet (ACTION_SEND)
        if (action == Intent.ACTION_SEND) {
            val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }

            if (streamUri != null || type?.startsWith("image/") == true) {
                val uris = buildList {
                    if (streamUri != null) {
                        add(streamUri)
                    } else {
                        intent.data?.let { add(it) }
                    }
                }
                return ParsedCaptureIntent(
                    isLaunchSnipTool = false,
                    itemType = ItemType.IMAGE,
                    initialContent = "Shared Image",
                    initialUris = uris
                )
            } else {
                val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                    ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()
                    ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
                    ?: intent.dataString
                    ?: ""

                val trimmed = sharedText.trim()
                val detectedType = if (trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true)
                ) {
                    ItemType.URL
                } else {
                    ItemType.TEXT
                }
                return ParsedCaptureIntent(
                    isLaunchSnipTool = false,
                    itemType = detectedType,
                    initialContent = trimmed,
                    initialUris = emptyList()
                )
            }
        }

        // 5. Android Share Sheet (ACTION_SEND_MULTIPLE)
        if (action == Intent.ACTION_SEND_MULTIPLE) {
            val streamUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            }

            val uris = mutableListOf<Uri>()
            if (!streamUris.isNullOrEmpty()) {
                uris.addAll(streamUris.filterNotNull())
            }
            val clip = intent.clipData
            if (uris.isEmpty() && clip != null) {
                for (i in 0 until clip.itemCount) {
                    clip.getItemAt(i).uri?.let { uris.add(it) }
                }
            }
            return ParsedCaptureIntent(
                isLaunchSnipTool = false,
                itemType = ItemType.IMAGE,
                initialContent = if (uris.size == 1) "Shared Image" else "${uris.size} Shared Images",
                initialUris = uris
            )
        }

        return ParsedCaptureIntent(
            isLaunchSnipTool = false,
            itemType = ItemType.TEXT,
            initialContent = "",
            initialUris = emptyList()
        )
    }

    private fun closeOverlay(resultCode: Int = Activity.RESULT_CANCELED) {
        setResult(resultCode)
        BreadcrumbAccessibilityService.latestCapturedBitmap = null
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun setupComposeUI() {
        val repository = CaptureRepository.getInstance(applicationContext)
        val settingsManager = SettingsManager.getInstance(applicationContext)

        setContent {
            BreadcrumbTheme {
                val coroutineScope = rememberCoroutineScope()
                val isPro by settingsManager.isPro.collectAsStateWithLifecycle()
                val textSelectionEnabled by settingsManager.textSelectionEnabled.collectAsStateWithLifecycle()
                val allTags by repository.allTags.collectAsStateWithLifecycle(initialValue = emptyList())

                val currentIntent = activeIntent ?: intent
                val parsed = remember(currentIntent, intentVersion, textSelectionEnabled) {
                    parseIncomingIntent(currentIntent, textSelectionEnabled)
                }

                var isSnipping by remember(intentVersion) {
                    mutableStateOf(parsed.isLaunchSnipTool)
                }
                var snippedBitmap by remember(intentVersion) {
                    mutableStateOf<Bitmap?>(null)
                }

                LaunchedEffect(intentVersion) {
                    if (parsed.isProcessTextDisabled) {
                        Toast.makeText(
                            this@CaptureActivity,
                            "Text selection is disabled in settings",
                            Toast.LENGTH_SHORT
                        ).show()
                        closeOverlay()
                        return@LaunchedEffect
                    }
                    if (!parsed.isLaunchSnipTool && parsed.initialContent.isBlank() && parsed.initialUris.isEmpty()) {
                        Toast.makeText(this@CaptureActivity, "Nothing to capture", Toast.LENGTH_SHORT).show()
                        closeOverlay()
                    }
                }

                // Step 1: If launched from Floating Mascot Overlay, show ScreenSnipDialog first
                if (isSnipping) {
                    ScreenSnipDialog(
                        initialBitmap = BreadcrumbAccessibilityService.latestCapturedBitmap,
                        onDismiss = { closeOverlay(Activity.RESULT_CANCELED) },
                        onSnipConfirmed = { croppedBitmap ->
                            snippedBitmap = croppedBitmap
                            isSnipping = false
                        }
                    )
                }

                // Step 2: Show SaveFlowSheet for snipped bitmap, selected text, or shared media
                val activeItemType = if (snippedBitmap != null) ItemType.IMAGE else parsed.itemType
                val activeInitialContent = if (snippedBitmap != null) "Screen Snip" else parsed.initialContent
                val activeUris = if (snippedBitmap != null) emptyList() else parsed.initialUris
                val showSheet = !isSnipping &&
                    (snippedBitmap != null || activeInitialContent.isNotBlank() || activeUris.isNotEmpty())

                if (showSheet) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { closeOverlay() }
                            )
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .imePadding(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { /* Consume click so sheet stays open */ }
                            )
                        ) {
                            SaveFlowSheet(
                                itemType = activeItemType,
                                initialContent = activeInitialContent,
                                initialImageUri = activeUris.firstOrNull(),
                                initialBitmap = snippedBitmap,
                                existingTags = allTags,
                                isPro = isPro,
                                onSave = { savedContent, contextText, tag ->
                                    val bmpToSave = snippedBitmap
                                    coroutineScope.launch {
                                        if (bmpToSave != null) {
                                            repository.saveBitmapCapture(bmpToSave, contextText, tag)
                                        } else if (activeItemType == ItemType.IMAGE && activeUris.isNotEmpty()) {
                                            repository.saveMultipleImageCaptures(activeUris, contextText, tag)
                                        } else {
                                            repository.saveCapture(activeItemType, savedContent, contextText, tag)
                                        }
                                        Toast.makeText(applicationContext, "Saved to Breadcrumb", Toast.LENGTH_SHORT).show()
                                        closeOverlay(Activity.RESULT_OK)
                                    }
                                },
                                onDiscard = { closeOverlay(Activity.RESULT_CANCELED) },
                                onNavigateToUpgrade = {
                                    val upgradeIntent = Intent(applicationContext, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        putExtra("navigate_to", "upgrade")
                                    }
                                    startActivity(upgradeIntent)
                                    closeOverlay(Activity.RESULT_CANCELED)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
