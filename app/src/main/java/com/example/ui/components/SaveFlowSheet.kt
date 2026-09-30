package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ItemType
import com.example.ui.theme.CrustBrown
import com.example.ui.theme.SnippetBoxLight
import com.example.ui.theme.SquiggleBorderLight
import java.io.File

@Composable
fun SaveFlowSheet(
    itemType: ItemType,
    initialContent: String,
    initialImageUri: Uri? = null,
    initialBitmap: Bitmap? = null,
    initialContext: String = "",
    initialTag: String? = null,
    existingTags: List<String> = emptyList(),
    isPro: Boolean = false,
    onSave: (content: String, contextText: String, tag: String?) -> Unit,
    onDiscard: () -> Unit,
    onNavigateToUpgrade: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var contentText by remember(initialContent) { mutableStateOf(initialContent) }
    var isEditingContent by remember { mutableStateOf(initialContent.isBlank() && itemType != ItemType.IMAGE) }
    var contextText by remember { mutableStateOf(initialContext.take(200)) }
    var selectedTag by remember { mutableStateOf(initialTag?.take(50)) }
    var newTagInput by remember { mutableStateOf(initialTag?.take(50) ?: "") }
    var showFullImagePreview by remember { mutableStateOf(false) }

    if (showFullImagePreview && itemType == ItemType.IMAGE) {
        val file = if (initialContent.isNotBlank()) File(initialContent).takeIf { it.exists() } else null
        FullScreenImageDialog(
            imageFile = file,
            imageUri = initialImageUri,
            bitmap = initialBitmap,
            contextNote = contextText,
            tag = selectedTag,
            onDismiss = { showFullImagePreview = false }
        )
    }

    // Validation Rules
    val isContentValid = if (itemType == ItemType.IMAGE) {
        initialBitmap != null || initialImageUri != null || contentText.isNotBlank()
    } else {
        contentText.trim().isNotEmpty()
    }
    val isContextValid = contextText.trim().isNotEmpty() && contextText.length <= 200
    val hasTag = !selectedTag.isNullOrBlank()

    val canSave = if (isPro) {
        isContentValid && (isContextValid || hasTag) && contextText.length <= 200
    } else {
        isContentValid && isContextValid
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .squigglyBorder(
                color = CrustBrown.copy(alpha = 0.7f),
                cornerRadius = 24.dp,
                strokeWidth = 1.8.dp,
                wavelength = 24.dp,
                amplitude = 1.15.dp,
                seed = 2.1f
            )
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .testTag("save_flow_sheet"),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Mascot + Captured Content title & Discard button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BreadcrumbMascot(modifier = Modifier.size(34.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Save Breadcrumb",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (itemType) {
                                ItemType.TEXT -> "SNIPPET"
                                ItemType.URL -> "LINK"
                                ItemType.IMAGE -> "IMAGE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp
                        )
                    }
                }
                IconButton(
                    onClick = onDiscard,
                    modifier = Modifier.testTag("discard_capture_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Discard",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            SquigglyDivider(color = SquiggleBorderLight)
            Spacer(modifier = Modifier.height(12.dp))

            // CAPTURED CONTENT PREVIEW BOX (with hand-drawn squiggly border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .squigglyBorder(
                        color = SquiggleBorderLight,
                        cornerRadius = 14.dp,
                        strokeWidth = 1.4.dp,
                        seed = 3.5f
                    )
                    .background(SnippetBoxLight, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (itemType) {
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
                            text = when (itemType) {
                                ItemType.TEXT -> "SNIPPET"
                                ItemType.URL -> "LINK"
                                ItemType.IMAGE -> "PHOTO (TAP TO EXPAND)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (itemType) {
                        ItemType.IMAGE -> {
                            if (initialBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = initialBitmap.asImageBitmap(),
                                    contentDescription = "Tap to view full captured image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .squigglyBorder(
                                            color = SquiggleBorderLight,
                                            cornerRadius = 10.dp,
                                            strokeWidth = 1.2.dp
                                        )
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showFullImagePreview = true },
                                    contentScale = ContentScale.Crop
                                )
                            } else if (initialImageUri != null) {
                                AsyncImage(
                                    model = initialImageUri,
                                    contentDescription = "Tap to view full captured image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .squigglyBorder(
                                            color = SquiggleBorderLight,
                                            cornerRadius = 10.dp,
                                            strokeWidth = 1.2.dp
                                        )
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showFullImagePreview = true },
                                    contentScale = ContentScale.Crop
                                )
                            } else if (initialContent.isNotBlank()) {
                                val file = File(initialContent)
                                if (file.exists()) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "Tap to view full captured image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .squigglyBorder(
                                                color = SquiggleBorderLight,
                                                cornerRadius = 10.dp,
                                                strokeWidth = 1.2.dp
                                            )
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { showFullImagePreview = true },
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(text = "Image: $initialContent", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        ItemType.URL -> {
                            if (isEditingContent) {
                                OutlinedTextField(
                                    value = contentText,
                                    onValueChange = { contentText = it },
                                    label = { Text("URL Link") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = contentText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { isEditingContent = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit link", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        ItemType.TEXT -> {
                            if (isEditingContent) {
                                OutlinedTextField(
                                    value = contentText,
                                    onValueChange = { contentText = it },
                                    label = { Text("Captured Text") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 6
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = contentText.ifBlank { "(No text content)" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { isEditingContent = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit text", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CONTEXT FIELD: "Why are you saving this?" (Max 200 chars)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        letterSpacing = 1.0.sp
                    )
                }
                Text(
                    text = "${contextText.length}/200",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (contextText.length > 200) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = contextText,
                onValueChange = { if (it.length <= 200) contextText = it },
                placeholder = {
                    Text(
                        "Why are you saving this?${if (isPro) " (or add #tag)" else " (required)"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .squigglyBorder(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        cornerRadius = 14.dp,
                        strokeWidth = 1.5.dp,
                        seed = 4.4f
                    )
                    .testTag("context_input"),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // TAG FIELD: Pro only, single tag only, max 50 chars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocalOffer,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = CrustBrown
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TAG",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrustBrown,
                        letterSpacing = 1.0.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isPro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 9.sp
                        )
                    }
                }
                if (selectedTag != null) {
                    Text(
                        text = "${selectedTag!!.length}/50",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (!isPro) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .squigglyBorder(
                            color = SquiggleBorderLight,
                            cornerRadius = 12.dp,
                            strokeWidth = 1.3.dp,
                            seed = 6.1f
                        )
                        .background(SnippetBoxLight, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateToUpgrade?.invoke() }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = CrustBrown,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tags are included with Pro ($10 one-time)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { raw ->
                            val clean = raw.filter { !it.isWhitespace() }.take(50)
                            newTagInput = clean
                            selectedTag = if (clean.isNotBlank()) clean.removePrefix("#") else null
                        },
                        placeholder = { Text("e.g. work, receipt, ideas") },
                        leadingIcon = {
                            Text(
                                text = "#",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CrustBrown,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        trailingIcon = {
                            if (newTagInput.isNotBlank()) {
                                IconButton(onClick = {
                                    newTagInput = ""
                                    selectedTag = null
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear tag", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .squigglyBorder(
                                color = CrustBrown.copy(alpha = 0.65f),
                                cornerRadius = 12.dp,
                                strokeWidth = 1.4.dp
                            )
                            .testTag("inline_tag_input")
                    )

                    if (existingTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            existingTags.forEach { existing ->
                                val isSelected = selectedTag.equals(existing, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            selectedTag = null
                                            newTagInput = ""
                                        } else {
                                            selectedTag = existing
                                            newTagInput = existing
                                        }
                                    },
                                    label = { Text("#$existing", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors()
                                )
                            }
                        }
                    }
                }
            }

            if (!canSave) {
                Spacer(modifier = Modifier.height(10.dp))
                val hint = when {
                    !isContentValid -> "Content is required"
                    contextText.trim().isEmpty() && !hasTag -> "Add a note explaining why you're saving this"
                    contextText.length > 200 -> "Context note exceeds 200 characters"
                    else -> "Complete required fields above"
                }
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // BUTTONS: Save & Discard with hand-drawn squiggly borders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .squigglyBorder(
                            color = SquiggleBorderLight,
                            cornerRadius = 14.dp,
                            strokeWidth = 1.4.dp
                        )
                        .testTag("discard_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = {
                        if (canSave) {
                            onSave(contentText.trim(), contextText.trim(), selectedTag)
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .squigglyBorder(
                            color = if (canSave) CrustBrown else SquiggleBorderLight,
                            cornerRadius = 14.dp,
                            strokeWidth = 1.6.dp
                        )
                        .testTag("save_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
