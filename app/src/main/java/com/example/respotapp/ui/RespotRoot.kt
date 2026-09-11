package com.example.respotapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.respotapp.domain.SearchResultType
import com.example.respotapp.ui.detail.AlbumDetailScreen
import com.example.respotapp.ui.detail.ArtistDetailScreen
import com.example.respotapp.ui.detail.PlaylistDetailScreen
import com.example.respotapp.ui.home.ClientIdGuideScreen
import com.example.respotapp.ui.home.LoginScreen
import com.example.respotapp.ui.library.LibraryScreen
import com.example.respotapp.ui.metadata.EditMetadataSheet
import com.example.respotapp.ui.metadata.PlaylistPickerSheet
import com.example.respotapp.ui.navigation.RespotRoute
import com.example.respotapp.ui.navigation.bottomNavItems
import com.example.respotapp.ui.navigation.mainTabRoutes
import com.example.respotapp.ui.player.MiniPlayerBar
import com.example.respotapp.ui.player.NowPlayingScreen
import com.example.respotapp.ui.search.SearchScreen
import com.example.respotapp.ui.settings.SettingsScreen
import com.example.respotapp.ui.theme.RespotBlack
import com.example.respotapp.ui.theme.RespotTextSecondary
import com.example.respotapp.ui.viewmodel.MainViewModel

@Composable
fun RespotRoot(
    viewModel: MainViewModel = viewModel(),
    onLoginClick: () -> Unit,
    onRequestStoragePermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            navController.navigate(RespotRoute.Library.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            }
        }
    }

    if (!uiState.isAuthenticated) {
        var showClientIdGuide by remember { mutableStateOf(false) }
        if (showClientIdGuide) {
            ClientIdGuideScreen(onBack = { showClientIdGuide = false })
        } else {
            LoginScreen(
                isLoading = uiState.isAuthLoading,
                hasClientId = uiState.hasClientId,
                clientIdInput = uiState.clientIdInput,
                errorMessage = uiState.authError,
                onClientIdChange = viewModel::onClientIdChange,
                onSaveClientId = viewModel::saveClientId,
                onLoginClick = onLoginClick,
                onOpenClientIdGuide = { showClientIdGuide = true }
            )
        }
    } else {
        val showBottomBar = currentRoute in mainTabRoutes
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.snackbarMessage) {
            val message = uiState.snackbarMessage
            if (message != null) {
                snackbarHostState.showSnackbar(message)
                viewModel.clearSnackbar()
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = RespotBlack,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(RespotBlack)
                        .navigationBarsPadding()
                ) {
                    MiniPlayerBar(
                        playerState = uiState.playerState,
                        onBarClick = viewModel::showNowPlaying,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onSkipNext = viewModel::skipNext
                    )
                    if (showBottomBar) {
                        RespotTabBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = RespotRoute.Library.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(RespotRoute.Library.route) {
                        LibraryScreen(
                            items = uiState.filteredLibraryItems,
                            isLoading = uiState.isLibraryLoading,
                            hasLocalAccess = uiState.hasLocalAccess,
                            localTrackCount = uiState.localTrackCount,
                            searchQuery = uiState.librarySearchQuery,
                            selectedFilter = uiState.libraryFilter,
                            selectedSort = uiState.librarySort,
                            onSearchQueryChange = viewModel::onLibrarySearchChange,
                            onFilterChange = viewModel::onLibraryFilterChange,
                            onSortChange = viewModel::onLibrarySortChange,
                            onRefresh = { viewModel.refreshLibrary() },
                            onGrantStorageAccess = onRequestStoragePermission,
                            onAlbumClick = { albumId ->
                                navController.navigate(RespotRoute.AlbumDetail.createRoute(albumId))
                            },
                            onPlaylistClick = { playlistId ->
                                navController.navigate(RespotRoute.PlaylistDetail.createRoute(playlistId))
                            }
                        )
                    }
                    composable(RespotRoute.Search.route) {
                        SearchScreen(
                            query = uiState.searchQuery,
                            results = uiState.searchResults,
                            isSearching = uiState.isSearching,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onItemClick = { item ->
                                when (item.type) {
                                    SearchResultType.ARTIST ->
                                        navController.navigate(RespotRoute.ArtistDetail.createRoute(item.id))
                                    SearchResultType.ALBUM ->
                                        navController.navigate(RespotRoute.AlbumDetail.createRoute(item.id))
                                    SearchResultType.TRACK -> viewModel.playSearchTrack(item)
                                }
                            },
                            onSaveAlbum = viewModel::saveAlbum
                        )
                    }
                    composable(RespotRoute.Settings.route) {
                        SettingsScreen(
                            uiSettings = uiState.uiSettings,
                            displayName = uiState.displayName,
                            onDarkModeChange = viewModel::updateDarkMode,
                            onDynamicColorChange = viewModel::updateDynamicColor,
                            onAccentColorChange = viewModel::updateAccentColor,
                            onCompactLayoutChange = viewModel::updateCompactLayout,
                            onShowAlbumArtChange = viewModel::updateShowAlbumArt,
                            onNowPlayingLayoutChange = viewModel::updateNowPlayingLayout,
                            onNowPlayingShowArtBackgroundChange = viewModel::updateNowPlayingShowArtBackground,
                            onNowPlayingShowProgressChange = viewModel::updateNowPlayingShowProgress,
                            onNowPlayingLargeArtworkChange = viewModel::updateNowPlayingLargeArtwork,
                            onLogout = viewModel::logout
                        )
                    }
                    composable(
                        route = RespotRoute.ArtistDetail.route,
                        arguments = listOf(navArgument("artistId") { type = NavType.StringType })
                    ) { entry ->
                        val artistId = entry.arguments?.getString("artistId") ?: return@composable
                        LaunchedEffect(artistId) {
                            viewModel.loadArtistDetail(artistId)
                        }
                        ArtistDetailScreen(
                            artist = uiState.artistDetail,
                            isLoading = uiState.isDetailLoading,
                            onBack = {
                                viewModel.clearDetailState()
                                navController.popBackStack()
                            },
                            onAlbumClick = { albumId ->
                                navController.navigate(RespotRoute.AlbumDetail.createRoute(albumId))
                            }
                        )
                    }
                    composable(
                        route = RespotRoute.AlbumDetail.route,
                        arguments = listOf(navArgument("albumId") { type = NavType.StringType })
                    ) { entry ->
                        val albumId = entry.arguments?.getString("albumId") ?: return@composable
                        LaunchedEffect(albumId) {
                            viewModel.loadAlbumDetail(albumId)
                        }
                        AlbumDetailScreen(
                            album = uiState.albumDetail,
                            isLoading = uiState.isDetailLoading,
                            onBack = {
                                viewModel.clearDetailState()
                                navController.popBackStack()
                            },
                            onArtistClick = { artistId ->
                                navController.navigate(RespotRoute.ArtistDetail.createRoute(artistId))
                            },
                            onSaveAlbum = viewModel::saveAlbum,
                            onTrackClick = { trackId ->
                                uiState.albumDetail?.let { album ->
                                    viewModel.playAlbumTrack(album, trackId)
                                }
                            },
                            onEditAlbum = viewModel::openEditAlbumMetadata,
                            onEditTrack = viewModel::openEditTrackMetadata,
                            onRemoveTrack = viewModel::removeTrackFromAlbum,
                            onToggleLike = viewModel::toggleTrackLike,
                            onAddTrackToPlaylist = { track ->
                                viewModel.openPlaylistPicker(track.id)
                            }
                        )
                    }
                    composable(
                        route = RespotRoute.PlaylistDetail.route,
                        arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                    ) { entry ->
                        val playlistId = entry.arguments?.getString("playlistId") ?: return@composable
                        LaunchedEffect(playlistId) {
                            viewModel.loadPlaylistDetail(playlistId)
                        }
                        PlaylistDetailScreen(
                            playlist = uiState.playlistDetail,
                            isLoading = uiState.isDetailLoading,
                            onBack = {
                                viewModel.clearDetailState()
                                navController.popBackStack()
                            },
                            onEditPlaylist = viewModel::openEditPlaylistMetadata,
                            onTrackClick = { trackId ->
                                val track = uiState.playlistDetail?.tracks?.find { it.id == trackId }
                                track?.let { viewModel.playSearchTrackFromDetail(it) }
                            },
                            onEditTrack = viewModel::openEditTrackMetadata,
                            onToggleLike = viewModel::toggleTrackLike,
                            onAddTrackToPlaylist = { track ->
                                viewModel.openPlaylistPicker(track.id)
                            }
                        )
                    }
                }

                uiState.editingMetadata?.let { metadata ->
                    EditMetadataSheet(
                        metadata = metadata,
                        onSave = viewModel::saveMetadata,
                        onDismiss = viewModel::dismissEditMetadata
                    )
                }

                if (uiState.showPlaylistPicker) {
                    PlaylistPickerSheet(
                        playlists = uiState.playlistPickerItems,
                        onSelect = viewModel::addTrackToPlaylist,
                        onDismiss = viewModel::dismissPlaylistPicker
                    )
                }

                if (uiState.playerState.showExpandedPlayer) {
                    NowPlayingScreen(
                        playerState = uiState.playerState,
                        uiSettings = uiState.uiSettings,
                        onBack = viewModel::hideNowPlaying,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onSkipNext = viewModel::skipNext,
                        onSkipPrevious = viewModel::skipPrevious,
                        onSeek = viewModel::seekTo,
                        onDownload = viewModel::downloadCurrentTrack,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun RespotTabBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        RespotBlack.copy(alpha = 0.85f),
                        RespotBlack
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RespotBlack)
                .padding(horizontal = 50.dp, vertical = 8.dp)
                .height(52.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentRoute == item.route
                Column(
                    modifier = Modifier
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (item.route == RespotRoute.Settings.route) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) Color.White.copy(alpha = 0.15f)
                                    else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (selected) Color.White else RespotTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = if (selected) Color.White else RespotTextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) Color.White else RespotTextSecondary
                    )
                }
            }
        }
    }
}
