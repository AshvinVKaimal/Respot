package com.example.respotapp.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.respotapp.domain.HomeModule
import com.example.respotapp.domain.HomeModuleType
import com.example.respotapp.domain.LibraryAlbum
import com.example.respotapp.domain.LibraryPlaylist
import com.example.respotapp.domain.ListeningStats
import com.example.respotapp.domain.RecentPlayItem
import com.example.respotapp.ui.AsyncArtworkImage
import com.example.respotapp.ui.RespotEmptyCard
import com.example.respotapp.ui.RespotModuleCard
import com.example.respotapp.ui.RespotScreenBackground
import com.example.respotapp.ui.RespotSectionHeader
import com.example.respotapp.ui.SourceBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    displayName: String?,
    modules: List<HomeModule>,
    recentlyPlayed: List<RecentPlayItem>,
    virtualAlbums: List<LibraryAlbum>,
    playlists: List<LibraryPlaylist>,
    localAlbums: List<LibraryAlbum>,
    savedAlbums: List<LibraryAlbum>,
    stats: ListeningStats,
    isLoading: Boolean,
    showCustomize: Boolean,
    editingModules: List<HomeModule>,
    onRefresh: () -> Unit,
    onCustomizeClick: () -> Unit,
    onDismissCustomize: () -> Unit,
    onToggleModuleVisibility: (HomeModuleType) -> Unit,
    onReorderModules: (Int, Int) -> Unit,
    onSaveLayout: () -> Unit,
    onBrowseLibrary: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onRecentTrackClick: (RecentPlayItem) -> Unit,
    modifier: Modifier = Modifier
) {
    RespotScreenBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = greeting(displayName),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Your music at a glance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCustomizeClick) {
                        Icon(Icons.Default.Tune, contentDescription = "Customize home")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            if (isLoading && modules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        modules.filter { it.isVisible },
                        key = { it.type }
                    ) { module ->
                        when (module.type) {
                            HomeModuleType.RECENTLY_PLAYED -> RecentlyPlayedModule(
                                items = recentlyPlayed,
                                onTrackClick = onRecentTrackClick
                            )
                            HomeModuleType.VIRTUAL_ALBUMS -> AlbumRowModule(
                                title = "Virtual albums",
                                albums = virtualAlbums,
                                emptyTitle = "No virtual albums yet",
                                emptyMessage = "Create custom albums in the Virtual Album Editor.",
                                emptyIcon = Icons.Outlined.Album,
                                onAlbumClick = onAlbumClick
                            )
                            HomeModuleType.CUSTOM_PLAYLISTS -> PlaylistRowModule(
                                playlists = playlists,
                                onBrowseLibrary = onBrowseLibrary
                            )
                            HomeModuleType.LOCAL_TRACKS -> AlbumRowModule(
                                title = "Local music",
                                albums = localAlbums,
                                emptyTitle = "No local tracks",
                                emptyMessage = "Grant storage access in Settings to index files on your device.",
                                emptyIcon = Icons.Outlined.MusicNote,
                                onAlbumClick = onAlbumClick
                            )
                            HomeModuleType.LISTENING_STATS -> StatsModule(
                                stats = stats,
                                onBrowseLibrary = onBrowseLibrary
                            )
                        }
                    }

                    item {
                        SavedAlbumsPeek(
                            albums = savedAlbums,
                            onBrowseLibrary = onBrowseLibrary,
                            onAlbumClick = onAlbumClick
                        )
                    }
                }
            }
        }

        if (showCustomize) {
            HomeCustomizeSheet(
                modules = editingModules,
                onToggleVisibility = onToggleModuleVisibility,
                onReorder = onReorderModules,
                onSave = onSaveLayout,
                onDismiss = onDismissCustomize
            )
        }
    }
}

private fun greeting(displayName: String?): String {
    val name = displayName?.trim().orEmpty()
    return if (name.isNotBlank()) "Hello, $name" else "Hello"
}

@Composable
private fun RecentlyPlayedModule(
    items: List<RecentPlayItem>,
    onTrackClick: (RecentPlayItem) -> Unit
) {
    RespotModuleCard {
        RespotSectionHeader(title = "Recently played")
        if (items.isEmpty()) {
            RespotEmptyCard(
                title = "Nothing played yet",
                message = "Start listening and your recent tracks will appear here.",
                icon = Icons.Outlined.GraphicEq
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEach { item ->
                    Column(
                        modifier = Modifier
                            .width(120.dp)
                            .clickable { onTrackClick(item) }
                    ) {
                        AsyncArtworkImage(
                            url = item.artworkUrl,
                            modifier = Modifier
                                .size(120.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumRowModule(
    title: String,
    albums: List<LibraryAlbum>,
    emptyTitle: String,
    emptyMessage: String,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onAlbumClick: (String) -> Unit
) {
    RespotModuleCard {
        RespotSectionHeader(title = title)
        if (albums.isEmpty()) {
            RespotEmptyCard(
                title = emptyTitle,
                message = emptyMessage,
                icon = emptyIcon
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                albums.take(12).forEach { album ->
                    Column(
                        modifier = Modifier
                            .width(130.dp)
                            .clickable { onAlbumClick(album.id) }
                    ) {
                        AsyncArtworkImage(
                            url = album.artworkUrl,
                            modifier = Modifier
                                .size(130.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SourceBadge(source = album.source)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRowModule(
    playlists: List<LibraryPlaylist>,
    onBrowseLibrary: () -> Unit
) {
    RespotModuleCard {
        RespotSectionHeader(
            title = "Playlists",
            actionLabel = "Browse all",
            onAction = onBrowseLibrary
        )
        if (playlists.isEmpty()) {
            RespotEmptyCard(
                title = "No playlists synced",
                message = "Refresh your library to pull playlists from Spotify.",
                icon = Icons.Outlined.PlaylistPlay,
                actionLabel = "Open library",
                onAction = onBrowseLibrary
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                playlists.take(10).forEach { playlist ->
                    Column(modifier = Modifier.width(130.dp)) {
                        AsyncArtworkImage(
                            url = playlist.artworkUrl,
                            modifier = Modifier.size(130.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${playlist.trackCount} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsModule(
    stats: ListeningStats,
    onBrowseLibrary: () -> Unit
) {
    RespotModuleCard {
        RespotSectionHeader(title = "Listening stats")
        if (stats.totalPlays == 0) {
            RespotEmptyCard(
                title = "No listening history",
                message = "Play a track to start tracking your stats.",
                icon = Icons.Outlined.GraphicEq
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCell(label = "Plays", value = stats.totalPlays.toString())
                StatCell(label = "Tracks", value = stats.uniqueTracks.toString())
                StatCell(
                    label = "Hours",
                    value = if (stats.hoursListened < 0.1f) "< 0.1" else "%.1f".format(stats.hoursListened)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onBrowseLibrary) {
                Text("Explore your library")
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun SavedAlbumsPeek(
    albums: List<LibraryAlbum>,
    onBrowseLibrary: () -> Unit,
    onAlbumClick: (String) -> Unit
) {
    if (albums.isEmpty()) return

    RespotModuleCard(modifier = Modifier.padding(top = 8.dp)) {
        RespotSectionHeader(
            title = "Saved albums",
            actionLabel = "Browse library",
            onAction = onBrowseLibrary,
            icon = Icons.Outlined.LibraryMusic
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            albums.take(8).forEach { album ->
                Column(
                    modifier = Modifier
                        .width(110.dp)
                        .clickable { onAlbumClick(album.id) }
                ) {
                    AsyncArtworkImage(
                        url = album.artworkUrl,
                        modifier = Modifier.size(110.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
