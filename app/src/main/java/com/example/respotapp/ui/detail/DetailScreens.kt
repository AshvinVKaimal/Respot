package com.example.respotapp.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.respotapp.domain.AlbumDetail
import com.example.respotapp.domain.AlbumTrackItem
import com.example.respotapp.domain.ArtistDetail
import com.example.respotapp.domain.LibraryAlbum
import com.example.respotapp.domain.PlaylistDetail
import com.example.respotapp.domain.SpotifyLibraryIds
import com.example.respotapp.ui.AsyncArtworkImage
import com.example.respotapp.ui.RespotScreenBackground
import com.example.respotapp.ui.SquareArtworkImage
import com.example.respotapp.ui.theme.RespotLikedGradientEnd
import com.example.respotapp.ui.theme.RespotLikedGradientStart
import com.example.respotapp.ui.theme.RespotTextSecondary

@Composable
fun ArtistDetailScreen(
    artist: ArtistDetail?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    RespotScreenBackground(modifier = modifier) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            artist != null -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    DetailTopBar(onBack = onBack)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ArtistHero(artist = artist)
                        }
                        items(artist.albums, key = { it.id }) { album ->
                            ArtistAlbumItem(
                                album = album,
                                onClick = { onAlbumClick(album.id) }
                            )
                        }
                    }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    DetailTopBar(onBack = onBack)
                }
            }
        }
    }
}

@Composable
private fun ArtistHero(artist: ArtistDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncArtworkImage(
            url = artist.artworkUrl,
            contentDescription = artist.name,
            modifier = Modifier.size(180.dp),
            circular = true
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        val meta = buildList {
            if (artist.followerCount > 0) {
                add(formatFollowers(artist.followerCount))
            }
            if (artist.genres.isNotEmpty()) {
                add(artist.genres.take(3).joinToString(" • "))
            }
        }.joinToString(" • ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = RespotTextSecondary,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Albums",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun ArtistAlbumItem(
    album: LibraryAlbum,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        SquareArtworkImage(
            url = album.artworkUrl,
            contentDescription = album.title,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 4.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = RespotTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlbumDetailScreen(
    album: AlbumDetail?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    onTrackClick: (String) -> Unit,
    onSaveAlbum: (String) -> Unit,
    onEditAlbum: (AlbumDetail) -> Unit,
    onEditTrack: (AlbumTrackItem) -> Unit,
    onRemoveTrack: (AlbumDetail, AlbumTrackItem) -> Unit,
    onToggleLike: (AlbumTrackItem) -> Unit,
    onAddTrackToPlaylist: (AlbumTrackItem) -> Unit,
    modifier: Modifier = Modifier
) {
    RespotScreenBackground(modifier = modifier) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            album != null -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        DetailTopBar(
                            onBack = onBack,
                            trailing = {
                                IconButton(onClick = { onEditAlbum(album) }) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = "Edit metadata",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        )
                    }
                    item {
                        AlbumHero(
                            album = album,
                            onArtistClick = onArtistClick,
                            onSaveAlbum = { onSaveAlbum(album.id) },
                            onPlay = {
                                album.tracks.firstOrNull()?.let { onTrackClick(it.id) }
                            },
                            onEditAlbum = { onEditAlbum(album) }
                        )
                    }
                    itemsIndexed(album.tracks, key = { _, track -> track.id }) { index, track ->
                        TrackRow(
                            track = track,
                            trackNumber = track.trackNumber.takeIf { it > 0 } ?: (index + 1),
                            onClick = { onTrackClick(track.id) },
                            onEdit = { onEditTrack(track) },
                            onRemove = { onRemoveTrack(album, track) },
                            onToggleLike = { onToggleLike(track) },
                            onAddToPlaylist = { onAddTrackToPlaylist(track) },
                            showRemove = true
                        )
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    DetailTopBar(onBack = onBack)
                }
            }
        }
    }
}

@Composable
private fun AlbumHero(
    album: AlbumDetail,
    onArtistClick: (String) -> Unit,
    onSaveAlbum: () -> Unit,
    onPlay: () -> Unit,
    onEditAlbum: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SquareArtworkImage(
            url = album.artworkUrl,
            contentDescription = album.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            cornerRadius = 4.dp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clickable {
                    album.artistId?.let { onArtistClick(it) }
                }
        )
        Spacer(modifier = Modifier.height(6.dp))
        val release = album.releaseDate?.takeIf { it.isNotBlank() }
        Text(
            text = buildString {
                if (release != null) append(release)
                if (release != null) append(" • ")
                append("${album.tracks.size} tracks")
            },
            style = MaterialTheme.typography.bodySmall,
            color = RespotTextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSaveAlbum) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Save to library",
                    tint = RespotTextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onEditAlbum) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Edit metadata",
                    tint = RespotTextSecondary
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistDetail?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onEditPlaylist: (PlaylistDetail) -> Unit,
    onTrackClick: (String) -> Unit,
    onEditTrack: (AlbumTrackItem) -> Unit,
    onToggleLike: (AlbumTrackItem) -> Unit,
    onAddTrackToPlaylist: (AlbumTrackItem) -> Unit,
    modifier: Modifier = Modifier
) {
    RespotScreenBackground(modifier = modifier) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            playlist != null -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        DetailTopBar(
                            onBack = onBack,
                            trailing = {
                                IconButton(onClick = { onEditPlaylist(playlist) }) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = "Edit metadata",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        )
                    }
                    item {
                        PlaylistHero(
                            playlist = playlist,
                            onPlay = {
                                playlist.tracks.firstOrNull()?.let { onTrackClick(it.id) }
                            },
                            onEditPlaylist = { onEditPlaylist(playlist) }
                        )
                    }
                    itemsIndexed(playlist.tracks, key = { _, track -> track.id }) { _, track ->
                        TrackRow(
                            track = track,
                            trackNumber = null,
                            onClick = { onTrackClick(track.id) },
                            onEdit = { onEditTrack(track) },
                            onRemove = { },
                            onToggleLike = { onToggleLike(track) },
                            onAddToPlaylist = { onAddTrackToPlaylist(track) },
                            showRemove = false
                        )
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    DetailTopBar(onBack = onBack)
                }
            }
        }
    }
}

@Composable
private fun PlaylistHero(
    playlist: PlaylistDetail,
    onPlay: () -> Unit,
    onEditPlaylist: () -> Unit
) {
    val isLikedSongs =
        playlist.id == SpotifyLibraryIds.LIKED_SONGS_PLAYLIST_ID ||
            playlist.title.equals("Liked Songs", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLikedSongs && playlist.artworkUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(RespotLikedGradientStart, RespotLikedGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Liked Songs",
                    tint = Color.White,
                    modifier = Modifier.size(96.dp)
                )
            }
        } else {
            SquareArtworkImage(
                url = playlist.artworkUrl,
                contentDescription = playlist.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                cornerRadius = 4.dp
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Playlist by ${playlist.owner}",
            style = MaterialTheme.typography.titleMedium,
            color = RespotTextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${playlist.tracks.size} tracks",
            style = MaterialTheme.typography.bodySmall,
            color = RespotTextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEditPlaylist) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Edit metadata",
                    tint = RespotTextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onEditPlaylist) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Edit playlist",
                    tint = RespotTextSecondary
                )
            }
        }
    }
}

@Composable
private fun DetailTopBar(
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 44.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun TrackRow(
    track: AlbumTrackItem,
    trackNumber: Int?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    showRemove: Boolean
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (trackNumber != null) {
            Text(
                text = trackNumber.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = RespotTextSecondary,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = RespotTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = RespotTextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(onClick = onToggleLike) {
            Icon(
                imageVector = if (track.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (track.isLiked) "Unlike" else "Like",
                tint = if (track.isLiked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    RespotTextSecondary
                }
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = RespotTextSecondary
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Edit metadata") },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    }
                )
                if (!track.isLocal) {
                    DropdownMenuItem(
                        text = { Text("Add to playlist") },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                }
                if (showRemove) {
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

private fun formatFollowers(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM followers", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK followers", count / 1_000.0)
        else -> "$count followers"
    }
}
