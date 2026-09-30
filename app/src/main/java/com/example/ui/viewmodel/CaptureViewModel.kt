package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CaptureRepository
import com.example.data.SettingsManager
import com.example.data.model.CaptureEntity
import com.example.data.model.ItemType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    RECENT("Recent"),
    OLDEST("Oldest"),
    TEXT("Text"),
    IMAGE("Image"),
    URL("URL")
}

data class CaptureUiState(
    val captures: List<CaptureEntity> = emptyList(),
    val filteredCaptures: List<CaptureEntity> = emptyList(),
    val allTags: List<String> = emptyList(),
    val isPro: Boolean = false,
    val sortOption: SortOption = SortOption.RECENT,
    val searchQuery: String = "",
    val selectedTag: String? = null,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val floatingOverlayEnabled: Boolean = true,
    val textSelectionEnabled: Boolean = true
)

class CaptureViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CaptureRepository.getInstance(application)
    private val settingsManager = SettingsManager.getInstance(application)

    private val _sortOption = MutableStateFlow(SortOption.RECENT)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())

    private data class FilterParams(
        val sort: SortOption,
        val query: String,
        val tag: String?,
        val selected: Set<String>,
        val isPro: Boolean
    )

    private val filterParams = combine(
        _sortOption,
        _searchQuery,
        _selectedTag,
        _selectedIds,
        settingsManager.isPro
    ) { sort, query, tag, selected, isPro ->
        FilterParams(sort, query, tag, selected, isPro)
    }

    private data class SettingsSnapshot(
        val floatingOverlay: Boolean,
        val textSelection: Boolean
    )

    private val settingsSnapshot = combine(
        settingsManager.floatingOverlayEnabled,
        settingsManager.textSelectionEnabled
    ) { overlay, textSelection ->
        SettingsSnapshot(overlay, textSelection)
    }

    init {
        viewModelScope.launch {
            // Startup Cleanup: Scan image directory, compare against Room records, delete orphaned files
            repository.performStartupCleanup()
        }
    }

    val uiState: StateFlow<CaptureUiState> = combine(
        repository.allCaptures,
        repository.allTags,
        filterParams,
        settingsSnapshot
    ) { captures, tags, params, settings ->
        // Apply Pro search and tag filters (only if user has Pro)
        val filtered = captures.filter { item ->
            val matchesQuery = if (params.isPro && params.query.isNotBlank()) {
                item.context.contains(params.query, ignoreCase = true) ||
                    (item.tag?.contains(params.query, ignoreCase = true) == true)
            } else {
                true
            }
            val matchesTag = if (params.isPro && params.tag != null) {
                item.tag.equals(params.tag, ignoreCase = true)
            } else {
                true
            }
            matchesQuery && matchesTag
        }.let { list ->
            when (params.sort) {
                SortOption.RECENT -> list.sortedByDescending { it.createdTime }
                SortOption.OLDEST -> list.sortedBy { it.createdTime }
                SortOption.TEXT -> list.sortedWith(
                    compareByDescending<CaptureEntity> { it.itemType == ItemType.TEXT }
                        .thenByDescending { it.createdTime }
                )
                SortOption.IMAGE -> list.sortedWith(
                    compareByDescending<CaptureEntity> { it.itemType == ItemType.IMAGE }
                        .thenByDescending { it.createdTime }
                )
                SortOption.URL -> list.sortedWith(
                    compareByDescending<CaptureEntity> { it.itemType == ItemType.URL }
                        .thenByDescending { it.createdTime }
                )
            }
        }

        CaptureUiState(
            captures = captures,
            filteredCaptures = filtered,
            allTags = tags,
            isPro = params.isPro,
            sortOption = params.sort,
            searchQuery = params.query,
            selectedTag = params.tag,
            selectedIds = params.selected,
            isSelectionMode = params.selected.isNotEmpty(),
            floatingOverlayEnabled = settings.floatingOverlay,
            textSelectionEnabled = settings.textSelection
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CaptureUiState()
    )

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun toggleSelect(id: String) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedIds.value = current
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun selectAll() {
        val allIds = uiState.value.filteredCaptures.map { it.id }.toSet()
        _selectedIds.value = allIds
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val toDelete = uiState.value.captures.filter { _selectedIds.value.contains(it.id) }
            repository.deleteCaptures(toDelete)
            _selectedIds.value = emptySet()
        }
    }

    fun deleteCapture(capture: CaptureEntity) {
        viewModelScope.launch {
            repository.deleteCapture(capture)
            val current = _selectedIds.value.toMutableSet()
            current.remove(capture.id)
            _selectedIds.value = current
        }
    }

    fun updateCapture(capture: CaptureEntity, newContext: String, newTag: String?) {
        viewModelScope.launch {
            repository.updateCapture(capture, newContext, newTag)
        }
    }

    fun saveTextCapture(content: String, contextText: String, tag: String?, isUrl: Boolean = false) {
        viewModelScope.launch {
            val type = if (isUrl || content.startsWith("http://") || content.startsWith("https://")) {
                ItemType.URL
            } else {
                ItemType.TEXT
            }
            repository.saveCapture(type, content, contextText, tag)
        }
    }

    fun saveBitmapCapture(bitmap: Bitmap, contextText: String, tag: String?) {
        viewModelScope.launch {
            repository.saveBitmapCapture(bitmap, contextText, tag)
        }
    }

    fun saveMultipleImages(uris: List<Uri>, contextText: String, tag: String?) {
        viewModelScope.launch {
            repository.saveMultipleImageCaptures(uris, contextText, tag)
        }
    }

    // Settings actions
    fun setPro(enabled: Boolean) {
        settingsManager.setPro(enabled)
    }

    fun setFloatingOverlayEnabled(enabled: Boolean) {
        settingsManager.setFloatingOverlayEnabled(enabled)
    }

    fun setTextSelection(enabled: Boolean) {
        settingsManager.setTextSelection(enabled)
    }
}
