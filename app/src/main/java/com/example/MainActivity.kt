package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GalleryNavTab
import com.example.ui.GalleryViewModel
import com.example.ui.MediaFilter
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.CleanerScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.MediaViewerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.AuraGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GalleryViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Resolve theme mode
            val isDarkTheme = when (uiState.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            AuraGalleryTheme(darkTheme = isDarkTheme) {
                GalleryApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun GalleryApp(viewModel: GalleryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allMedia by viewModel.allVisibleMedia.collectAsStateWithLifecycle()
    val photos by viewModel.photosMedia.collectAsStateWithLifecycle()
    val videos by viewModel.videosMedia.collectAsStateWithLifecycle()
    val documents by viewModel.documentsMedia.collectAsStateWithLifecycle()
    val favorites by viewModel.favoritesMedia.collectAsStateWithLifecycle()
    val trashItems by viewModel.trashMedia.collectAsStateWithLifecycle()
    val vaultItems by viewModel.vaultMedia.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var isSearchActive by remember { mutableStateOf(false) }
    var isSettingsActive by remember { mutableStateOf(false) }

    // Media permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val anyGranted = permissions.values.any { it }
        if (anyGranted) {
            viewModel.scanMedia(force = true)
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    // Show info messages in snackbar
    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearInfoMessage()
        }
    }

    // Fullscreen Media Viewer takes highest precedence
    if (uiState.viewerItem != null) {
        MediaViewerScreen(
            viewModel = viewModel,
            item = uiState.viewerItem!!,
            isSlideshowActive = uiState.isSlideshowActive,
            onBack = { viewModel.closeViewer() },
            onNext = { viewModel.nextViewerItem() },
            onPrev = { viewModel.prevViewerItem() },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // Search Screen
    if (isSearchActive) {
        SearchScreen(
            viewModel = viewModel,
            allMedia = allMedia,
            onBack = { isSearchActive = false },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // Settings Screen
    if (isSettingsActive) {
        SettingsScreen(
            viewModel = viewModel,
            uiState = uiState,
            onBack = { isSettingsActive = false },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // Album Detail View
    if (uiState.selectedAlbum != null) {
        AlbumDetailScreen(
            viewModel = viewModel,
            uiState = uiState,
            album = uiState.selectedAlbum!!,
            allMedia = allMedia,
            onBack = { viewModel.closeAlbum() },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // Main App Scaffold with Bottom Navigation
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!uiState.isSelectionMode) {
                NavigationBar(
                    modifier = Modifier.testTag("gallery_bottom_navigation")
                ) {
                    NavigationBarItem(
                        selected = uiState.currentTab == GalleryNavTab.PHOTOS,
                        onClick = { viewModel.setNavTab(GalleryNavTab.PHOTOS) },
                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Photos") },
                        label = { Text("Photos") },
                        modifier = Modifier.testTag("tab_nav_photos")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == GalleryNavTab.ALBUMS,
                        onClick = { viewModel.setNavTab(GalleryNavTab.ALBUMS) },
                        icon = { Icon(Icons.Default.Collections, contentDescription = "Albums") },
                        label = { Text("Albums") },
                        modifier = Modifier.testTag("tab_nav_albums")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == GalleryNavTab.EXPLORE,
                        onClick = { viewModel.setNavTab(GalleryNavTab.EXPLORE) },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Explore") },
                        label = { Text("Explore") },
                        modifier = Modifier.testTag("tab_nav_explore")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == GalleryNavTab.CLEANER,
                        onClick = { viewModel.setNavTab(GalleryNavTab.CLEANER) },
                        icon = { Icon(Icons.Default.CleaningServices, contentDescription = "Cleaner") },
                        label = { Text("Cleaner") },
                        modifier = Modifier.testTag("tab_nav_cleaner")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == GalleryNavTab.VAULT,
                        onClick = { viewModel.setNavTab(GalleryNavTab.VAULT) },
                        icon = { Icon(Icons.Default.Lock, contentDescription = "Vault") },
                        label = { Text("Vault") },
                        modifier = Modifier.testTag("tab_nav_vault")
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (uiState.currentTab) {
            GalleryNavTab.PHOTOS -> {
                val currentItems = when (uiState.mediaFilter) {
                    MediaFilter.ALL -> allMedia
                    MediaFilter.PHOTOS -> photos
                    MediaFilter.VIDEOS -> videos
                    MediaFilter.DOCUMENTS -> documents
                    MediaFilter.FAVORITES -> favorites
                }
                TimelineScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    items = currentItems,
                    onOpenSearch = { isSearchActive = true },
                    onOpenSettings = { isSettingsActive = true },
                    modifier = contentModifier
                )
            }
            GalleryNavTab.ALBUMS -> {
                AlbumsScreen(
                    viewModel = viewModel,
                    allMedia = allMedia,
                    trashCount = trashItems.size,
                    vaultCount = vaultItems.size,
                    onNavigateToTrash = {
                        viewModel.setNavTab(GalleryNavTab.CLEANER)
                    },
                    onNavigateToVault = {
                        viewModel.setNavTab(GalleryNavTab.VAULT)
                    },
                    modifier = contentModifier
                )
            }
            GalleryNavTab.EXPLORE -> {
                ExploreScreen(
                    viewModel = viewModel,
                    allMedia = allMedia,
                    modifier = contentModifier
                )
            }
            GalleryNavTab.CLEANER -> {
                CleanerScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    trashItems = trashItems,
                    modifier = contentModifier
                )
            }
            GalleryNavTab.VAULT -> {
                VaultScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    vaultItems = vaultItems,
                    modifier = contentModifier
                )
            }
            GalleryNavTab.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onBack = { viewModel.setNavTab(GalleryNavTab.PHOTOS) },
                    modifier = contentModifier
                )
            }
        }
    }
}
