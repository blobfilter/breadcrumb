package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CaptureEntity
import com.example.data.model.ItemType
import com.example.ui.theme.CrustBrown
import com.example.ui.theme.SnippetBoxLight
import com.example.ui.theme.SquiggleBorderLight
import com.example.ui.theme.TagBadgeBgLight
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CaptureItemCard(
    capture: CaptureEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isPro: Boolean,
    onSelect: () -> Unit,
    onLongClick: () -> Unit,
    onUpdate: (newContext: String, newTag: String?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showFullImagePreview by remember { mutableStateOf(false) }
    var editContextText by remember(capture.context) { mutableStateOf(capture.context) }
    var editTagText by remember(capture.tag) { mutableStateOf(capture.tag ?: "") }
    val cardSeed = remember(capture.id) { (abs(capture.id.hashCode()) % 100) * 0.13f }

    val formattedDate = remember(capture.updatedTime) {
        val diff = System.currentTimeMillis() - capture.updatedTime
        when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(capture.updatedTime))
        }
    }

    if (showFullImagePreview && capture.itemType == ItemType.IMAGE) {
        val file = File(capture.content)
        if (file.exists()) {
            FullScreenImageDialog(
                imageFile = file,
                contextNote = capture.context,
                tag = capture.tag,
                onDismiss = { showFullImagePreview = false }
            )
        }
    }

    val outerBorderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        SquiggleBorderLight
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .squigglyBorder(
                color = outerBorderColor,
                cornerRadius = 22.dp,
                strokeWidth = if (isSelected) 2.2.dp else 1.5.dp,
                wavelength = 24.dp,
                amplitude = 1.15.dp,
                seed = cardSeed
            )
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelect()
                    } else {
                        isExpanded = !isExpanded
                    }
                },
                onLongClick = onLongClick
            )
            .testTag("capture_card_${capture.id}"),
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // HEADER ROW: Compass Icon + "CONTEXT" label + optional #tag + relative timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelect() },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = Icons.Outlined.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONTEXT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.1.sp,
                        fontSize = 11.sp
                    )

                    // Single Tag chip (if tag exists)
                    if (!capture.tag.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .squigglyBorder(
                                    color = CrustBrown.copy(alpha = 0.55f),
                                    cornerRadius = 8.dp,
                                    strokeWidth = 1.1.dp,
                                    wavelength = 14.dp,
                                    amplitude = 0.7.dp,
                                    seed = cardSeed + 1.5f
                                )
                                .background(TagBadgeBgLight, RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#${capture.tag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrustBrown,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // THE PRIMARY OBJECT: CONTEXT NOTE
            if (capture.context.isNotBlank()) {
                Text(
                    text = capture.context,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        lineHeight = 24.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) 10 else 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // INNER BOX: CAPTURED SNIPPET / PHOTO / LINK with squiggly hand-drawn border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .squigglyBorder(
                        color = SquiggleBorderLight.copy(alpha = 0.9f),
                        cornerRadius = 14.dp,
                        strokeWidth = 1.3.dp,
                        wavelength = 20.dp,
                        amplitude = 1.0.dp,
                        seed = cardSeed + 3.1f
                    )
                    .background(SnippetBoxLight, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Snippet Box Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (capture.itemType) {
                                ItemType.TEXT -> Icons.Default.FormatQuote
                                ItemType.URL -> Icons.Default.Link
                                ItemType.IMAGE -> Icons.Outlined.PhotoCamera
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = when (capture.itemType) {
                                ItemType.TEXT -> "SNIPPET"
                                ItemType.URL -> "LINK"
                                ItemType.IMAGE -> "PHOTO"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (capture.itemType) {
                        ItemType.IMAGE -> {
                            val file = File(capture.content)
                            if (file.exists()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (isExpanded) 220.dp else 150.dp)
                                        .squigglyBorder(
                                            color = SquiggleBorderLight,
                                            cornerRadius = 10.dp,
                                            strokeWidth = 1.2.dp,
                                            seed = cardSeed + 5f
                                        )
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (isSelectionMode) {
                                                onSelect()
                                            } else {
                                                showFullImagePreview = true
                                            }
                                        }
                                        .testTag("capture_image_preview_${capture.id}")
                                ) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "Tap to preview full image",
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.62f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fullscreen,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "View full",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Image unavailable",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        ItemType.URL -> {
                            Text(
                                text = capture.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = if (isExpanded) 5 else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        ItemType.TEXT -> {
                            Text(
                                text = capture.content,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                                maxLines = if (isExpanded) 14 else 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // EXPANDED VIEW ACTIONS (Edit context, Edit tag, Copy/View content, Delete)
            AnimatedVisibility(visible = isExpanded && !isSelectionMode) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    SquigglyDivider(modifier = Modifier.padding(bottom = 10.dp))

                    if (isEditing) {
                        Text(
                            text = "Why you saved this (max 200 chars):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editContextText,
                            onValueChange = { if (it.length <= 200) editContextText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .squigglyBorder(
                                    color = MaterialTheme.colorScheme.primary,
                                    cornerRadius = 10.dp,
                                    strokeWidth = 1.4.dp
                                ),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (isPro) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tag (optional, max 50 chars):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CrustBrown
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = editTagText,
                                onValueChange = { if (it.length <= 50) editTagText = it.filter { ch -> !ch.isWhitespace() } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .squigglyBorder(
                                        color = CrustBrown,
                                        cornerRadius = 10.dp,
                                        strokeWidth = 1.3.dp
                                    ),
                                singleLine = true,
                                placeholder = { Text("e.g. design, receipts") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    editContextText = capture.context
                                    editTagText = capture.tag ?: ""
                                    isEditing = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .squigglyBorder(
                                        color = SquiggleBorderLight,
                                        cornerRadius = 10.dp
                                    ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    onUpdate(editContextText.trim(), editTagText.ifBlank { null })
                                    isEditing = false
                                },
                                enabled = editContextText.trim().isNotEmpty() || (isPro && editTagText.isNotBlank()),
                                modifier = Modifier
                                    .weight(1f)
                                    .squigglyBorder(
                                        color = MaterialTheme.colorScheme.primary,
                                        cornerRadius = 10.dp
                                    ),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Save")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (capture.itemType == ItemType.IMAGE) {
                                    TextButton(onClick = { showFullImagePreview = true }) {
                                        Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("View Full")
                                    }
                                } else {
                                    TextButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(capture.content))
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy")
                                    }
                                }

                                if (capture.itemType == ItemType.URL) {
                                    TextButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(capture.content))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open")
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        editContextText = capture.context
                                        editTagText = capture.tag ?: ""
                                        isEditing = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit")
                                }
                            }

                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
