package com.example.respotapp.ui.search

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.respotapp.domain.SearchResultItem
import com.example.respotapp.domain.SearchResultType
import com.example.respotapp.ui.AsyncArtworkImage
import com.example.respotapp.ui.RespotScreenBackground
import com.example.respotapp.ui.RespotSearchField
import com.example.respotapp.ui.theme.RespotTextSecondary

@Composable
fun SearchScreen(
    query: String,
    results: List<SearchResultItem>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onItemClick: (SearchResultItem) -> Unit,
    onSaveAlbum: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val showFocusedChrome = focused || query.isNotBlank()

    RespotScreenBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showFocusedChrome) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 56.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RespotSearchField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = "What do you want to play?",
                        leadingIcon = Icons.Default.Search,
                        dark = false,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focused = it.isFocused }
                    )
                    TextButton(
                        onClick = {
                            onQueryChange("")
                            focused = false
                        }
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 56.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    RespotSearchField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = "What do you want to play?",
                        leadingIcon = Icons.Default.Search,
                        dark = false,
                        modifier = Modifier.onFocusChanged { focused = it.isFocused }
                    )
                }
            }

            when {
                isSearching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                results.isEmpty() && query.isBlank() && showFocusedChrome -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Play what you love",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Search for artist, song, podcast and more.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RespotTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                results.isEmpty() && query.isBlank() -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Recent searches",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your recent searches will show up here.",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = RespotTextSecondary
                        )
                    }
                }
                results.isEmpty() -> {
                    Text(
                        text = "No results found",
                        modifier = Modifier.padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = RespotTextSecondary
                    )
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
                        items(results, key = { "${it.type}_${it.id}" }) { item ->
                            SearchResultRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onSaveAlbum = onSaveAlbum
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    item: SearchResultItem,
    onClick: () -> Unit,
    onSaveAlbum: (String) -> Unit
) {
    val circular = item.type == SearchResultType.ARTIST
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncArtworkImage(
            url = item.artworkUrl,
            modifier = Modifier.size(48.dp),
            circular = circular,
            cornerRadius = if (circular) 0.dp else 4.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when (item.type) {
                    SearchResultType.ARTIST -> "Artist"
                    SearchResultType.ALBUM -> "Album • ${item.subtitle}"
                    SearchResultType.TRACK -> "Song • ${item.subtitle}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = RespotTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (item.type == SearchResultType.ALBUM) {
            IconButton(onClick = { onSaveAlbum(item.id) }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Save to library",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
