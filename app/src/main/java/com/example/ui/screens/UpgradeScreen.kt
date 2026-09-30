package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BreadcrumbMascot
import com.example.ui.components.CrownIcon
import com.example.ui.components.SquigglyDivider
import com.example.ui.components.squigglyBorder
import com.example.ui.theme.CrustBrown
import com.example.ui.theme.SnippetBoxLight
import com.example.ui.theme.SquiggleBorderLight
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    isPro: Boolean,
    onUpgrade: () -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
    billingStatus: String? = null,
    onResetPro: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CrownIcon(modifier = Modifier.size(20.dp), color = CrustBrown)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Breadcrumb Pro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("upgrade_back_button")) {
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mascot Header with Crown
            BreadcrumbMascot(
                modifier = Modifier.size(76.dp),
                squigglyOutline = true
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Breadcrumb Pro",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "One-time • $10 • Lifetime access",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            // FEATURE COMPARISON CARD with hand-drawn squiggly border
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .squigglyBorder(
                        color = SquiggleBorderLight,
                        cornerRadius = 20.dp,
                        strokeWidth = 1.6.dp,
                        seed = 1.4f
                    ),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "FREE VS PRO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CrustBrown,
                        letterSpacing = 1.0.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ComparisonRow(
                        title = "Capture Anywhere",
                        subtitle = "Text, links, and screenshots",
                        free = true,
                        pro = true
                    )
                    ComparisonRow(
                        title = "Context Notes",
                        subtitle = "Save the why behind each item",
                        free = true,
                        pro = true
                    )
                    ComparisonRow(
                        title = "Full Control",
                        subtitle = "Copy, edit, and delete anytime",
                        free = true,
                        pro = true
                    )
                    ComparisonRow(
                        title = "Sorting",
                        subtitle = "Newest, oldest, or content type",
                        free = true,
                        pro = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    SquigglyDivider(color = SquiggleBorderLight)
                    Spacer(modifier = Modifier.height(8.dp))

                    ComparisonRow(
                        title = "Search Notes",
                        subtitle = "Find anything across your notes",
                        free = false,
                        pro = true,
                        icon = Icons.Default.Search
                    )
                    ComparisonRow(
                        title = "Search Tags",
                        subtitle = "Locate captures by tag",
                        free = false,
                        pro = true,
                        icon = Icons.Default.Tag
                    )
                    ComparisonRow(
                        title = "Custom Tags",
                        subtitle = "Tag captures for quick grouping",
                        free = false,
                        pro = true,
                        icon = Icons.Default.Tag
                    )
                    ComparisonRow(
                        title = "Filter Chips",
                        subtitle = "1-tap tag filters on home screen",
                        free = false,
                        pro = true,
                        icon = Icons.Default.FilterList
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ACTION BUTTONS: UPGRADE & RESTORE PURCHASE
            if (isPro) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .squigglyBorder(
                            color = SuccessGreen,
                            cornerRadius = 16.dp,
                            strokeWidth = 1.5.dp
                        )
                        .background(SuccessGreen.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pro Active — You're all set!",
                                style = MaterialTheme.typography.titleSmall,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (onResetPro != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onResetPro,
                                modifier = Modifier.squigglyBorder(
                                    color = SquiggleBorderLight,
                                    cornerRadius = 10.dp
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset to Free (Testing)", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .squigglyBorder(
                            color = CrustBrown,
                            cornerRadius = 16.dp,
                            strokeWidth = 1.8.dp
                        )
                        .testTag("upgrade_pro_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Unlock Pro for $10", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .squigglyBorder(
                            color = SquiggleBorderLight,
                            cornerRadius = 16.dp,
                            strokeWidth = 1.4.dp
                        )
                        .testTag("restore_purchase_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Restore Purchase")
                }
            }

            if (!billingStatus.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .squigglyBorder(
                            color = SquiggleBorderLight,
                            cornerRadius = 12.dp,
                            strokeWidth = 1.2.dp
                        )
                        .background(SnippetBoxLight, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = billingStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    title: String,
    subtitle: String,
    free: Boolean,
    pro: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                if (free) {
                    Icon(Icons.Default.Check, contentDescription = "Included in Free", modifier = Modifier.size(16.dp), tint = SuccessGreen)
                } else {
                    Icon(Icons.Default.Close, contentDescription = "Not in Free", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                if (pro) {
                    Icon(Icons.Default.Check, contentDescription = "Included in Pro", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
