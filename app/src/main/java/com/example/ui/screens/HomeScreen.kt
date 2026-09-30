package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CaptureEntity
import com.example.data.model.ItemType
import com.example.ui.components.BreadcrumbMascot
import com.example.ui.components.CaptureItemCard
import com.example.ui.components.CrownIcon
import com.example.ui.components.SaveFlowSheet
import com.example.ui.components.SquigglyDivider
import com.example.ui.components.squigglyBorder
import com.example.ui.theme.CrustBrown
import com.example.ui.theme.PillTrackLight
import com.example.ui.theme.SquiggleBorderLight
import com.example.ui.viewmodel.CaptureUiState
import com.example.ui.viewmodel.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: CaptureUiState,
    onSortChange: (SortOption) -> Unit,
    onSearchChange: (String) -> Unit,
    onTagSelect: (String?) -> Unit,
    onToggleSelect: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onUpdateCapture: (CaptureEntity, String, String?) -> Unit,
    onDeleteCapture: (CaptureEntity) -> Unit,
    onSaveTextCapture: (content: String, context: String, tag: String?) -> Unit,
    onSaveMultipleImages: (uris: List<Uri>, context: String, tag: String?) -> Unit,
    onSaveBitmap: (Bitmap, String, String?) -> Unit,
    onTriggerSnip: (() -> Unit)? = null,
    onNavigateToUpgrade: () -> Unit,
    onNavigateToCustomise: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddMenuSheet by remember { mutableStateOf(false) }
    var showCreateTextDialog by remember { mutableStateOf(false) }
    var enteredTextContent by remember { mutableStateOf("") }
    var showTagFilterTray by remember { mutableStateOf(false) }

    var showSaveFlowSheet by remember { mutableStateOf(false) }
    var saveFlowType by remember { mutableStateOf(ItemType.TEXT) }
    var saveFlowContent by remember { mutableStateOf("") }
    var saveFlowUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var saveFlowBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            saveFlowUris = uris
            saveFlowType = ItemType.IMAGE
            saveFlowContent = if (uris.size == 1) "1 image selected" else "${uris.size} images selected"
            saveFlowBitmap = null
            showSaveFlowSheet = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                if (uiState.isSelectionMode) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "${uiState.selectedIds.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Default.Close, contentDescription = "Close selection")
                            }
                        },
                        actions = {
                            IconButton(onClick = onSelectAll) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                            }
                            IconButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier.testTag("delete_selected_button")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete selected",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        )
                    )
                } else {
                    // Header matching reference image: Mascot + "Breadcrumb" on left, Crown + Gear on right
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BreadcrumbMascot(
                                    modifier = Modifier.size(36.dp),
                                    squigglyOutline = true
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Breadcrumb",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (uiState.isPro) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .squigglyBorder(
                                                color = CrustBrown,
                                                cornerRadius = 6.dp,
                                                strokeWidth = 1.2.dp,
                                                amplitude = 0.7.dp
                                            )
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = onNavigateToUpgrade,
                                modifier = Modifier.testTag("top_bar_upgrade_button")
                            ) {
                                CrownIcon(
                                    modifier = Modifier.size(24.dp),
                                    color = CrustBrown
                                )
                            }
                            IconButton(
                                onClick = onNavigateToCustomise,
                                modifier = Modifier.testTag("top_bar_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
                SquigglyDivider(
                    color = SquiggleBorderLight.copy(alpha = 0.75f),
                    strokeWidth = 1.2.dp
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMenuSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .squigglyBorder(
                        color = CrustBrown,
                        cornerRadius = 18.dp,
                        strokeWidth = 1.8.dp,
                        amplitude = 1.0.dp,
                        seed = 4.2f
                    )
                    .testTag("fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add capture", modifier = Modifier.size(26.dp))
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // ROW 1: [Recent | Tag] pill group  +  [Search...] pill  +  "Select" button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Segmented Pill: Recent / Oldest + Tag
                Row(
                    modifier = Modifier
                        .squigglyBorder(
                            color = SquiggleBorderLight,
                            cornerRadius = 24.dp,
                            strokeWidth = 1.4.dp,
                            wavelength = 20.dp,
                            amplitude = 0.95.dp,
                            seed = 1.1f
                        )
                        .background(PillTrackLight, RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isTimeSelected = !showTagFilterTray && uiState.selectedTag == null
                    val timeLabel = if (uiState.sortOption == SortOption.OLDEST) "Oldest" else "Recent"

                    // Recent / Oldest Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isTimeSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                showTagFilterTray = false
                                onTagSelect(null)
                                if (uiState.sortOption == SortOption.RECENT) {
                                    onSortChange(SortOption.OLDEST)
                                } else {
                                    onSortChange(SortOption.RECENT)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("sort_chip_oldest")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isTimeSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isTimeSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tag Pill
                    val isTagActive = showTagFilterTray || uiState.selectedTag != null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isTagActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                if (uiState.isPro) {
                                    showTagFilterTray = !showTagFilterTray
                                } else {
                                    onNavigateToUpgrade()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LocalOffer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isTagActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = uiState.selectedTag?.let { "#$it" } ?: "Tag",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isTagActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Middle: Search Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .squigglyBorder(
                            color = SquiggleBorderLight,
                            cornerRadius = 22.dp,
                            strokeWidth = 1.3.dp,
                            wavelength = 18.dp,
                            amplitude = 0.9.dp,
                            seed = 2.4f
                        )
                        .background(PillTrackLight.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                        .clickable(enabled = !uiState.isPro) {
                            onNavigateToUpgrade()
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.isPro) Icons.Default.Search else Icons.Default.Lock,
                            contentDescription = "Search",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (uiState.isPro) {
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = onSearchChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_search_input"),
                                decorationBox = { innerTextField ->
                                    if (uiState.searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (uiState.searchQuery.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onSearchChange("") },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Search...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                modifier = Modifier.testTag("home_search_input")
                            )
                        }
                    }
                }

                // Right: "Select" text button
                Text(
                    text = if (uiState.isSelectionMode) "Done" else "Select",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (uiState.isSelectionMode) {
                                onClearSelection()
                            } else if (uiState.filteredCaptures.isNotEmpty()) {
                                onToggleSelect(uiState.filteredCaptures.first().id)
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }

            // Expandable Tag Filter Carousel
            AnimatedVisibility(visible = uiState.isPro && (showTagFilterTray || uiState.selectedTag != null)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.selectedTag == null,
                        onClick = { onTagSelect(null) },
                        label = { Text("All Tags", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    if (uiState.allTags.isEmpty()) {
                        Text(
                            text = "No tags yet. Add a #tag when saving.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    } else {
                        uiState.allTags.forEach { tag ->
                            FilterChip(
                                selected = uiState.selectedTag == tag,
                                onClick = { onTagSelect(if (uiState.selectedTag == tag) null else tag) },
                                label = { Text("#$tag", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // ROW 2: Content Type Filter / Sort Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .squigglyBorder(
                        color = SquiggleBorderLight,
                        cornerRadius = 26.dp,
                        strokeWidth = 1.4.dp,
                        wavelength = 22.dp,
                        amplitude = 1.0.dp,
                        seed = 3.7f
                    )
                    .background(PillTrackLight, RoundedCornerShape(26.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeSegmentPill(
                    label = "All",
                    icon = Icons.Outlined.Layers,
                    selected = uiState.sortOption == SortOption.RECENT || uiState.sortOption == SortOption.OLDEST,
                    onClick = { onSortChange(SortOption.RECENT) },
                    testTag = "sort_chip_recent"
                )
                TypeSegmentPill(
                    label = "Photos",
                    icon = Icons.Outlined.PhotoCamera,
                    selected = uiState.sortOption == SortOption.IMAGE,
                    onClick = { onSortChange(SortOption.IMAGE) },
                    testTag = "sort_chip_image"
                )
                TypeSegmentPill(
                    label = "Text",
                    icon = Icons.Outlined.Description,
                    selected = uiState.sortOption == SortOption.TEXT,
                    onClick = { onSortChange(SortOption.TEXT) },
                    testTag = "sort_chip_text"
                )
                TypeSegmentPill(
                    label = "Links",
                    icon = Icons.Outlined.Link,
                    selected = uiState.sortOption == SortOption.URL,
                    onClick = { onSortChange(SortOption.URL) },
                    testTag = "sort_chip_url"
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // CAPTURES LIST
            if (uiState.filteredCaptures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .squigglyBorder(
                                color = SquiggleBorderLight,
                                cornerRadius = 22.dp,
                                strokeWidth = 1.5.dp,
                                seed = 5.5f
                            )
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BreadcrumbMascot(
                            modifier = Modifier.size(68.dp),
                            squigglyOutline = true
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank() || uiState.selectedTag != null)
                                "No results found"
                            else
                                "Nothing saved yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Capture from any app or tap + to save your first thought.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .testTag("captures_list"),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp)
                ) {
                    items(
                        items = uiState.filteredCaptures,
                        key = { it.id }
                    ) { capture ->
                        CaptureItemCard(
                            capture = capture,
                            isSelected = uiState.selectedIds.contains(capture.id),
                            isSelectionMode = uiState.isSelectionMode,
                            isPro = uiState.isPro,
                            onSelect = { onToggleSelect(capture.id) },
                            onLongClick = { onToggleSelect(capture.id) },
                            onUpdate = { newContext, newTag ->
                                onUpdateCapture(capture, newContext, newTag)
                            },
                            onDelete = { onDeleteCapture(capture) }
                        )
                    }
                }
            }
        }
    }

    // ADD CAPTURE ACTION MENU SHEET (+ Button at bottom)
    if (showAddMenuSheet) {
        val addMenuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddMenuSheet = false },
            sheetState = addMenuSheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BreadcrumbMascot(modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "New Breadcrumb",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Save a quick note or import photos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Create Note
                Surface(
                    onClick = {
                        showAddMenuSheet = false
                        showCreateTextDialog = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .squigglyBorder(
                            color = SquiggleBorderLight,
                            cornerRadius = 16.dp,
                            strokeWidth = 1.5.dp,
                            seed = 1.8f
                        )
                        .testTag("action_create_new")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Write Note",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Type a note, thought, or link",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Import Images
                Surface(
                    onClick = {
                        showAddMenuSheet = false
                        multiplePhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .squigglyBorder(
                            color = SquiggleBorderLight,
                            cornerRadius = 16.dp,
                            strokeWidth = 1.5.dp,
                            seed = 3.2f
                        )
                        .testTag("action_import")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(PillTrackLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = CrustBrown,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Import Photos",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose screenshots or pictures from device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // CREATE NEW DIALOG (Text only)
    if (showCreateTextDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTextDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text("New Note", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "What do you want to save?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = enteredTextContent,
                        onValueChange = { enteredTextContent = it },
                        placeholder = { Text("Paste or write here…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .squigglyBorder(
                                color = SquiggleBorderLight,
                                cornerRadius = 12.dp,
                                strokeWidth = 1.4.dp
                            ),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = enteredTextContent.trim()
                        if (text.isNotBlank()) {
                            showCreateTextDialog = false
                            saveFlowContent = text
                            saveFlowType = if (text.startsWith("http://") || text.startsWith("https://")) ItemType.URL else ItemType.TEXT
                            saveFlowUris = emptyList()
                            saveFlowBitmap = null
                            showSaveFlowSheet = true
                        }
                    },
                    enabled = enteredTextContent.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Next: Add Context")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // UNIVERSAL SAVE FLOW MODAL SHEET
    if (showSaveFlowSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showSaveFlowSheet = false
                enteredTextContent = ""
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            SaveFlowSheet(
                itemType = saveFlowType,
                initialContent = saveFlowContent,
                initialImageUri = saveFlowUris.firstOrNull(),
                initialBitmap = saveFlowBitmap,
                existingTags = uiState.allTags,
                isPro = uiState.isPro,
                onSave = { savedContent, contextText, tag ->
                    if (saveFlowBitmap != null) {
                        onSaveBitmap(saveFlowBitmap!!, contextText, tag)
                    } else if (saveFlowUris.isNotEmpty()) {
                        onSaveMultipleImages(saveFlowUris, contextText, tag)
                    } else {
                        onSaveTextCapture(savedContent, contextText, tag)
                    }
                    showSaveFlowSheet = false
                    enteredTextContent = ""
                },
                onDiscard = {
                    showSaveFlowSheet = false
                    enteredTextContent = ""
                },
                onNavigateToUpgrade = onNavigateToUpgrade
            )
        }
    }

    // MULTI-DELETE CONFIRMATION DIALOG
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text("Delete ${uiState.selectedIds.size} items?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove the selected captures.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteSelected()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TypeSegmentPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}
