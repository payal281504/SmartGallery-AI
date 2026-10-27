package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.Album
import com.example.data.model.AlbumType
import com.example.data.model.DuplicateGroup
import com.example.data.model.MediaItemEntity
import com.example.data.model.PersonGroup
import com.example.data.model.SimilarGroup
import com.example.data.model.StorageBreakdown
import com.example.domain.CleanerManager
import com.example.domain.MediaScanner
import com.example.domain.VaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GalleryNavTab {
    PHOTOS,
    ALBUMS,
    EXPLORE,
    CLEANER,
    VAULT,
    SETTINGS
}

enum class MediaFilter {
    ALL,
    PHOTOS,
    VIDEOS,
    DOCUMENTS,
    FAVORITES
}

data class GalleryUiState(
    val currentTab: GalleryNavTab = GalleryNavTab.PHOTOS,
    val mediaFilter: MediaFilter = MediaFilter.ALL,
    val isScanning: Boolean = false,
    val scanMessage: String = "",
    val selectedItemIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val selectedAlbum: Album? = null,
    val viewerItem: MediaItemEntity? = null,
    val viewerList: List<MediaItemEntity> = emptyList(),
    val isSlideshowActive: Boolean = false,
    val isVaultUnlocked: Boolean = false,
    val vaultPinError: String? = null,
    val searchQuery: String = "",
    val gridColumns: Int = 3,
    val themeMode: String = "SYSTEM",
    val isAiEnabled: Boolean = true,
    val isPeopleRecognitionEnabled: Boolean = true,
    val lastScanTime: Long = 0L,
    val storageBreakdown: StorageBreakdown = StorageBreakdown(),
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val similarGroups: List<SimilarGroup> = emptyList(),
    val largeFiles: List<MediaItemEntity> = emptyList(),
    val infoMessage: String? = null
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val dao = database.mediaDao()
    private val prefs = PreferencesManager(application)
    private val scanner = MediaScanner(application, dao, prefs)
    private val vaultManager = VaultManager(application, dao, prefs)
    private val cleanerManager = CleanerManager(dao)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    // Database flows
    val allVisibleMedia: StateFlow<List<MediaItemEntity>> = dao.getAllVisibleMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photosMedia: StateFlow<List<MediaItemEntity>> = dao.getPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videosMedia: StateFlow<List<MediaItemEntity>> = dao.getVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documentsMedia: StateFlow<List<MediaItemEntity>> = dao.getDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritesMedia: StateFlow<List<MediaItemEntity>> = dao.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashMedia: StateFlow<List<MediaItemEntity>> = dao.getTrashMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultMedia: StateFlow<List<MediaItemEntity>> = dao.getVaultMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var slideshowJob: Job? = null

    init {
        // Collect user preferences
        viewModelScope.launch {
            prefs.gridColumns.collect { cols ->
                _uiState.value = _uiState.value.copy(gridColumns = cols)
            }
        }
        viewModelScope.launch {
            prefs.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            prefs.isAiEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isAiEnabled = enabled)
            }
        }
        viewModelScope.launch {
            prefs.isPeopleRecognitionEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isPeopleRecognitionEnabled = enabled)
            }
        }
        viewModelScope.launch {
            prefs.lastScanTime.collect { time ->
                _uiState.value = _uiState.value.copy(lastScanTime = time)
            }
        }

        // Trigger initial scan
        scanMedia(force = false)
        refreshCleanerData()
    }

    fun setNavTab(tab: GalleryNavTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab, selectedAlbum = null)
        if (tab == GalleryNavTab.CLEANER) {
            refreshCleanerData()
        }
    }

    fun setFilter(filter: MediaFilter) {
        _uiState.value = _uiState.value.copy(mediaFilter = filter)
    }

    fun scanMedia(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanMessage = "Scanning media...")
            try {
                val scannedCount = scanner.scanDeviceMedia(force)
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanMessage = "Scan complete: $scannedCount items indexed",
                    infoMessage = "Media library updated ($scannedCount items)"
                )
                refreshCleanerData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanMessage = "Scan completed with warnings",
                    infoMessage = "Library ready"
                )
            }
        }
    }

    fun refreshCleanerData() {
        viewModelScope.launch(Dispatchers.IO) {
            val breakdown = cleanerManager.getStorageBreakdown()
            val duplicates = cleanerManager.findExactDuplicates()
            val similar = cleanerManager.findSimilarPhotos()
            val large = cleanerManager.getLargeFiles()

            _uiState.value = _uiState.value.copy(
                storageBreakdown = breakdown,
                duplicateGroups = duplicates,
                similarGroups = similar,
                largeFiles = large
            )
        }
    }

    fun toggleFavorite(item: MediaItemEntity) {
        viewModelScope.launch {
            dao.setFavorite(item.id, !item.isFavorite)
        }
    }

    fun moveToTrash(item: MediaItemEntity) {
        viewModelScope.launch {
            dao.moveToTrash(item.id)
            if (_uiState.value.viewerItem?.id == item.id) {
                closeViewer()
            }
            refreshCleanerData()
        }
    }

    fun restoreFromTrash(item: MediaItemEntity) {
        viewModelScope.launch {
            dao.restoreFromTrash(item.id)
            refreshCleanerData()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            dao.emptyTrash()
            refreshCleanerData()
            _uiState.value = _uiState.value.copy(infoMessage = "Trash emptied")
        }
    }

    fun moveToVault(item: MediaItemEntity) {
        viewModelScope.launch {
            vaultManager.moveToVault(item)
            if (_uiState.value.viewerItem?.id == item.id) {
                closeViewer()
            }
            _uiState.value = _uiState.value.copy(infoMessage = "Moved to Private Vault")
        }
    }

    fun restoreFromVault(item: MediaItemEntity) {
        viewModelScope.launch {
            vaultManager.restoreFromVault(item)
            _uiState.value = _uiState.value.copy(infoMessage = "Restored from Private Vault")
        }
    }

    fun unlockVault(pin: String): Boolean {
        var success = false
        viewModelScope.launch {
            success = vaultManager.verifyPin(pin)
            if (success) {
                _uiState.value = _uiState.value.copy(isVaultUnlocked = true, vaultPinError = null)
            } else {
                _uiState.value = _uiState.value.copy(vaultPinError = "Incorrect PIN. Default is 1234")
            }
        }
        return success
    }

    fun lockVault() {
        _uiState.value = _uiState.value.copy(isVaultUnlocked = false)
    }

    fun setNewVaultPin(newPin: String) {
        viewModelScope.launch {
            vaultManager.setPin(newPin)
            _uiState.value = _uiState.value.copy(infoMessage = "PIN successfully updated")
        }
    }

    fun rotateMedia(item: MediaItemEntity, degrees: Int = 90) {
        viewModelScope.launch {
            dao.rotateMedia(item.id, degrees)
            // Update viewerItem state if open
            _uiState.value.viewerItem?.let { current ->
                if (current.id == item.id) {
                    val updated = (current.rotationDegrees + degrees) % 360
                    _uiState.value = _uiState.value.copy(viewerItem = current.copy(rotationDegrees = updated))
                }
            }
        }
    }

    fun renameMedia(item: MediaItemEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            dao.renameMedia(item.id, newName.trim())
            _uiState.value.viewerItem?.let { current ->
                if (current.id == item.id) {
                    _uiState.value = _uiState.value.copy(viewerItem = current.copy(displayName = newName.trim()))
                }
            }
        }
    }

    fun renamePersonGroup(oldName: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            dao.renamePersonCluster(oldName, newName.trim())
            _uiState.value = _uiState.value.copy(infoMessage = "Person renamed to $newName")
        }
    }

    fun cleanDuplicateGroup(group: DuplicateGroup) {
        viewModelScope.launch {
            // Keep the first item, delete redundant copies
            val redundantIds = group.items.drop(1).map { it.id }
            dao.deleteByIds(redundantIds)
            refreshCleanerData()
            _uiState.value = _uiState.value.copy(infoMessage = "Removed ${redundantIds.size} duplicates")
        }
    }

    // Selection mode
    fun toggleSelection(id: Long) {
        val current = _uiState.value.selectedItemIds.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _uiState.value = _uiState.value.copy(
            selectedItemIds = current,
            isSelectionMode = current.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedItemIds = emptySet(),
            isSelectionMode = false
        )
    }

    fun selectAll(ids: List<Long>) {
        _uiState.value = _uiState.value.copy(
            selectedItemIds = ids.toSet(),
            isSelectionMode = true
        )
    }

    fun moveSelectedToTrash() {
        val ids = _uiState.value.selectedItemIds.toList()
        viewModelScope.launch {
            dao.moveToTrashBatch(ids)
            clearSelection()
            refreshCleanerData()
            _uiState.value = _uiState.value.copy(infoMessage = "Moved ${ids.size} items to Trash")
        }
    }

    fun moveSelectedToVault() {
        val ids = _uiState.value.selectedItemIds.toList()
        viewModelScope.launch {
            vaultManager.moveToVaultBatch(ids)
            clearSelection()
            _uiState.value = _uiState.value.copy(infoMessage = "Moved ${ids.size} items to Private Vault")
        }
    }

    // Viewer
    fun openViewer(item: MediaItemEntity, list: List<MediaItemEntity>) {
        _uiState.value = _uiState.value.copy(
            viewerItem = item,
            viewerList = list,
            isSlideshowActive = false
        )
    }

    fun closeViewer() {
        slideshowJob?.cancel()
        slideshowJob = null
        _uiState.value = _uiState.value.copy(
            viewerItem = null,
            viewerList = emptyList(),
            isSlideshowActive = false
        )
    }

    fun nextViewerItem() {
        val currentList = _uiState.value.viewerList
        val currentItem = _uiState.value.viewerItem ?: return
        val currentIndex = currentList.indexOfFirst { it.id == currentItem.id }
        if (currentIndex != -1 && currentIndex < currentList.size - 1) {
            _uiState.value = _uiState.value.copy(viewerItem = currentList[currentIndex + 1])
        } else if (currentIndex == currentList.size - 1 && _uiState.value.isSlideshowActive) {
            // Loop in slideshow
            _uiState.value = _uiState.value.copy(viewerItem = currentList.firstOrNull())
        }
    }

    fun prevViewerItem() {
        val currentList = _uiState.value.viewerList
        val currentItem = _uiState.value.viewerItem ?: return
        val currentIndex = currentList.indexOfFirst { it.id == currentItem.id }
        if (currentIndex > 0) {
            _uiState.value = _uiState.value.copy(viewerItem = currentList[currentIndex - 1])
        }
    }

    fun toggleSlideshow() {
        val active = !_uiState.value.isSlideshowActive
        _uiState.value = _uiState.value.copy(isSlideshowActive = active)
        if (active) {
            slideshowJob?.cancel()
            slideshowJob = viewModelScope.launch {
                while (_uiState.value.isSlideshowActive) {
                    delay(3000)
                    nextViewerItem()
                }
            }
        } else {
            slideshowJob?.cancel()
            slideshowJob = null
        }
    }

    // Album navigation
    fun openAlbum(album: Album) {
        _uiState.value = _uiState.value.copy(selectedAlbum = album)
    }

    fun closeAlbum() {
        _uiState.value = _uiState.value.copy(selectedAlbum = null)
    }

    // Search
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    // Settings
    fun setGridColumns(cols: Int) {
        viewModelScope.launch { prefs.setGridColumns(cols) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setAiEnabled(enabled) }
    }

    fun setPeopleRecognitionEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setPeopleRecognitionEnabled(enabled) }
    }
}
