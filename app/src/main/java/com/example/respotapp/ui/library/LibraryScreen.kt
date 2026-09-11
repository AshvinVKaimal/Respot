package com.example.respotapp.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.respotapp.domain.LibraryFilter
import com.example.respotapp.domain.LibraryItem
import com.example.respotapp.domain.LibrarySort
import com.example.respotapp.domain.SpotifyLibraryIds
import com.example.respotapp.domain.label
import com.example.respotapp.ui.RespotFilterChip
import com.example.respotapp.ui.RespotScreenBackground
import com.example.respotapp.ui.RespotSearchField
import com.example.respotapp.ui.SquareArtworkImage
import com.example.respotapp.ui.theme.RespotLikedGradientEnd
import com.example.respotapp.ui.theme.RespotLikedGradientStart
import com.example.respotapp.ui.theme.RespotTextSecondary

@Composable
fun LibraryScreen(
    items: List<LibraryItem>,
    isLoading: Boolean,
    hasLocalAccess: Boolean,
    localTrackCount: Int,
    searchQuery: String,
    selectedFilter: LibraryFilter,
    selectedSort: LibrarySort,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onRefresh: () -> Unit,
    onGrantStorageAccess: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    showBack: Boolean = false,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var searchVisible by remember { mutableStateOf(false) }
    var useFixedGrid by remember { mutableStateOf(true) }
    val searchFocusRequester = remember { FocusRequester() }

    val recentlyPlayed = remember(items) {
        items
            .filterIsInstance<LibraryItem.AlbumItem>()
            .filter { it.album.lastPlayedAt != null }
            .sortedByDescending { it.album.lastPlayedAt }
            .take(8)
    }

    RespotScreenBackground(modifier = modifier) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            items.isEmpty() && searchQuery.isBlank() && selectedFilter == LibraryFilter.ALL -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    LibraryHeader(
                        showBack = showBack,
                        onBack = onBack,
                        searchVisible = searchVisible,
                        onSearchClick = {
                            searchVisible = true
                        },
                        onAddClick = {
                            if (!hasLocalAccess) onGrantStorageAccess() else onRefresh()
                        }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No items yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Save albums from Spotify or add local music files.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RespotTextSecondary
                        )
                    }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    LibraryHeader(
                        showBack = showBack,
                        onBack = onBack,
                        searchVisible = searchVisible,
                        onSearchClick = {
                            searchVisible = true
                        },
                        onAddClick = {
                            if (!hasLocalAccess) onGrantStorageAccess() else onRefresh()
                        }
                    )

                    if (searchVisible || searchQuery.isNotBlank()) {
                        RespotSearchField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = "Search your library",
                            leadingIcon = Icons.Default.Search,
                            dark = true,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 12.dp)
                                .focusRequester(searchFocusRequester)
                        )
                        LaunchedEffect(searchVisible) {
                            if (searchVisible) {
                                runCatching { searchFocusRequester.requestFocus() }
                            }
                        }
                    }

                    LibraryFilterRow(
                        selectedFilter = selectedFilter,
                        onFilterChange = onFilterChange
                    )

                    if (!hasLocalAccess) {
                        Text(
                            text = "Grant storage access for local files",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGrantStorageAccess() }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (localTrackCount > 0) {
                        Text(
                            text = "$localTrackCount local tracks indexed",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = RespotTextSecondary
                        )
                    }

                    if (
                        recentlyPlayed.isNotEmpty() &&
                        searchQuery.isBlank() &&
                        selectedFilter == LibraryFilter.ALL
                    ) {
                        Text(
                            text = "Recently played",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            items(recentlyPlayed, key = { it.id }) { item ->
                                Column(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .clickable { onAlbumClick(item.album.id) }
                                ) {
                                    SquareArtworkImage(
                                        url = item.album.artworkUrl,
                                        contentDescription = item.album.title,
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 4.dp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.album.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Row(
                                modifier = Modifier.clickable { sortMenuExpanded = true },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = selectedSort.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                LibrarySort.entries.forEach { sort ->
                                    DropdownMenuItem(
                                        text = { Text(sort.label) },
                                        onClick = {
                                            onSortChange(sort)
                                            sortMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            if (selectedSort == sort) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { useFixedGrid = !useFixedGrid }) {
                            Icon(
                                imageVector = if (useFixedGrid) Icons.Default.Apps else Icons.Default.ViewModule,
                                contentDescription = if (useFixedGrid) "Adaptive grid" else "3-column grid",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    if (items.isEmpty()) {
                        Text(
                            text = "No matching items",
                            modifier = Modifier.padding(32.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = RespotTextSecondary
                        )
                    } else {
                        val gridState = rememberLazyGridState()

                        LaunchedEffect(selectedSort, selectedFilter, searchQuery) {
                            gridState.scrollToItem(0)
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = if (useFixedGrid) {
                                    GridCells.Fixed(3)
                                } else {
                                    GridCells.Adaptive(minSize = 120.dp)
                                },
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(items, key = { "${it::class.simpleName}_${it.id}" }) { item ->
                                    when (item) {
                                        is LibraryItem.AlbumItem -> AlbumGridItem(
                                            item = item,
                                            onClick = { onAlbumClick(item.album.id) }
                                        )
                                        is LibraryItem.PlaylistItem -> PlaylistGridItem(
                                            item = item,
                                            onClick = { onPlaylistClick(item.playlist.id) }
                                        )
                                    }
                                }
                            }

                            VerticalScrollbar(
                                gridState = gridState,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    showBack: Boolean,
    onBack: (() -> Unit)?,
    searchVisible: Boolean,
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 52.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack && onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSearchClick) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search library",
                tint = if (searchVisible) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground
                }
            )
        }
        IconButton(onClick = onAddClick) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add or refresh",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun LibraryFilterRow(
    selectedFilter: LibraryFilter,
    onFilterChange: (LibraryFilter) -> Unit
) {
    val chips = buildList {
        if (selectedFilter == LibraryFilter.ALL) add(LibraryFilter.ALL)
        add(LibraryFilter.PLAYLISTS)
        add(LibraryFilter.ALBUMS)
        add(LibraryFilter.ARTISTS)
        add(LibraryFilter.LOCAL)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { filter ->
            RespotFilterChip(
                label = filter.label,
                selected = selectedFilter == filter,
                onClick = {
                    if (selectedFilter == filter && filter != LibraryFilter.ALL) {
                        onFilterChange(LibraryFilter.ALL)
                    } else {
                        onFilterChange(filter)
                    }
                }
            )
        }
    }
}

@Composable
private fun VerticalScrollbar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    val layoutInfo = gridState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo.size

    if (totalItems <= visibleItems || visibleItems == 0) return

    val firstVisibleItem = gridState.firstVisibleItemIndex
    val totalHeight = totalItems.toFloat()
    val scrollOffset = firstVisibleItem.toFloat() / totalHeight
    val visibleFraction = visibleItems.toFloat() / totalHeight

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(6.dp)
            .padding(vertical = 4.dp, horizontal = 1.dp)
    ) {
        val viewHeight = maxHeight
        val barHeight = viewHeight * visibleFraction
        val barOffset = viewHeight * scrollOffset

        Box(
            modifier = Modifier
                .offset(y = barOffset)
                .height(barHeight)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(3.dp)
                )
        )
    }
}

private val LibraryFilter.label: String
    get() = when (this) {
        LibraryFilter.ALL -> "All"
        LibraryFilter.ALBUMS -> "Albums"
        LibraryFilter.PLAYLISTS -> "Playlists"
        LibraryFilter.LOCAL -> "Local"
        LibraryFilter.ARTISTS -> "Artists"
    }

@Composable
private fun AlbumGridItem(item: LibraryItem.AlbumItem, onClick: () -> Unit) {
    val album = item.album
    Column(modifier = Modifier.clickable(onClick = onClick)) {
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
            text = "Album • ${album.artist}",
            style = MaterialTheme.typography.bodySmall,
            color = RespotTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistGridItem(item: LibraryItem.PlaylistItem, onClick: () -> Unit) {
    val playlist = item.playlist
    val isLikedSongs =
        playlist.id == SpotifyLibraryIds.LIKED_SONGS_PLAYLIST_ID ||
            playlist.title.equals("Liked Songs", ignoreCase = true)

    Column(modifier = Modifier.clickable(onClick = onClick)) {
        if (isLikedSongs && playlist.artworkUrl.isNullOrBlank()) {
            LikedSongsArtwork(modifier = Modifier.fillMaxWidth())
        } else {
            SquareArtworkImage(
                url = playlist.artworkUrl,
                contentDescription = playlist.title,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 4.dp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Playlist • ${playlist.trackCount} songs",
            style = MaterialTheme.typography.bodySmall,
            color = RespotTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LikedSongsArtwork(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
            imageVector = Icons.Default.Favorite,
            contentDescription = "Liked Songs",
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}
