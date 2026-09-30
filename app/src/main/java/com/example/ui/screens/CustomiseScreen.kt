package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FeatureStatus
import com.example.service.BreadcrumbAccessibilityService
import com.example.ui.components.BreadcrumbMascot
import com.example.ui.components.SquigglyDivider
import com.example.ui.components.squigglyBorder
import com.example.ui.theme.CrustBrown
import com.example.ui.theme.SquiggleBorderLight
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomiseScreen(
    floatingOverlayEnabled: Boolean,
    onToggleFloatingOverlay: (Boolean) -> Unit,
    textSelectionEnabled: Boolean,
    onToggleTextSelection: (Boolean) -> Unit,
    onLaunchSnipTool: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isServiceConnected by BreadcrumbAccessibilityService.isServiceConnected.collectAsStateWithLifecycle()
    val detectedSelectedText by BreadcrumbAccessibilityService.detectedSelectedText.collectAsStateWithLifecycle()
    var isAccessibilityEnabledInSettings by remember {
        mutableStateOf(BreadcrumbAccessibilityService.isAccessibilityServiceEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabledInSettings =
                    BreadcrumbAccessibilityService.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val accessibilityActive = isServiceConnected || isAccessibilityEnabledInSettings
    val floatingOverlayStatus = when {
        !floatingOverlayEnabled -> FeatureStatus.DISABLED
        else -> FeatureStatus.ENABLED
    }
    val textSelectionStatus = when {
        !textSelectionEnabled -> FeatureStatus.DISABLED
        else -> FeatureStatus.ENABLED
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BreadcrumbMascot(modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Customise",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("customise_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                SquigglyDivider(color = SquiggleBorderLight)
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // SECTION 1: SYSTEM PERMISSION & STATUS
            Text(
                text = "CAPTURE STATUS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CrustBrown,
                letterSpacing = 0.9.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .squigglyBorder(
                        color = SquiggleBorderLight,
                        cornerRadius = 18.dp,
                        strokeWidth = 1.5.dp,
                        seed = 1.2f
                    ),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureStatusRow(
                        name = "Floating Mascot",
                        status = floatingOverlayStatus,
                        icon = Icons.Default.Crop,
                        subtitle = if (accessibilityActive) {
                            "Edge-docked for instant screen snips"
                        } else {
                            "Enable Accessibility for cross-app snips"
                        }
                    )
                    FeatureStatusRow(
                        name = "Text Selection Pill",
                        status = textSelectionStatus,
                        icon = Icons.Default.TextFields,
                        subtitle = if (accessibilityActive) {
                            "Detects selected text across apps"
                        } else {
                            "Shows quick Save pill on highlight"
                        }
                    )
                    FeatureStatusRow(
                        name = "Photo Import",
                        status = FeatureStatus.ENABLED,
                        icon = Icons.Default.Image,
                        subtitle = "Pick from photo library"
                    )
                    FeatureStatusRow(
                        name = "Quick Notes",
                        status = FeatureStatus.ENABLED,
                        icon = Icons.Default.Edit,
                        subtitle = "Direct in-app capture"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ACCESSIBILITY SERVICE SHORTCUT CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .squigglyBorder(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        cornerRadius = 16.dp,
                        strokeWidth = 1.4.dp,
                        seed = 2.5f
                    )
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Accessibility Service",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (accessibilityActive) {
                                "Active • Screen snips & text detection ready"
                            } else {
                                "Enable in Settings to capture across other apps"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Could not open Accessibility Settings", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .squigglyBorder(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                cornerRadius = 10.dp
                            )
                            .testTag("open_accessibility_settings_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (accessibilityActive) "Manage" else "Enable", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 2: CAPTURE PREFERENCES
            Text(
                text = "OVERLAY SETTINGS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CrustBrown,
                letterSpacing = 0.9.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .squigglyBorder(
                        color = SquiggleBorderLight,
                        cornerRadius = 18.dp,
                        strokeWidth = 1.5.dp,
                        seed = 3.9f
                    ),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Toggle 1: Floating Mascot Overlay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BreadcrumbMascot(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Floating Mascot",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Edge-docked mascot. Tap anytime to snip screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = floatingOverlayEnabled,
                            onCheckedChange = onToggleFloatingOverlay,
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("switch_floating_overlay")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    SquigglyDivider(color = SquiggleBorderLight)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle 2: Accessibility Text Selection Detection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Text Selection Detection",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Shows a quick 1-tap Save pill when text is highlighted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = textSelectionEnabled,
                            onCheckedChange = onToggleTextSelection,
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("switch_text_selection")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureStatusRow(
    name: String,
    status: FeatureStatus,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = CrustBrown
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = when (status) {
                FeatureStatus.ENABLED -> SuccessGreen.copy(alpha = 0.15f)
                FeatureStatus.DISABLED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                FeatureStatus.UNSUPPORTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            }
        ) {
            Text(
                text = when (status) {
                    FeatureStatus.ENABLED -> "Active"
                    FeatureStatus.DISABLED -> "Disabled"
                    FeatureStatus.UNSUPPORTED -> "Permission Needed"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when (status) {
                    FeatureStatus.ENABLED -> SuccessGreen
                    FeatureStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                    FeatureStatus.UNSUPPORTED -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
