package com.example.respotapp.ui.metadata

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.respotapp.domain.EditableMetadata
import com.example.respotapp.domain.LibraryPlaylist
import com.example.respotapp.domain.MetadataItemType
import com.example.respotapp.ui.AsyncArtworkImage
import com.example.respotapp.ui.RespotPrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMetadataSheet(
    metadata: EditableMetadata,
    onSave: (EditableMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(metadata.itemId) { mutableStateOf(metadata.title) }
    var artist by remember(metadata.itemId) { mutableStateOf(metadata.artist) }
    var albumName by remember(metadata.itemId) { mutableStateOf(metadata.albumName.orEmpty()) }
    var artworkUrl by remember(metadata.itemId) { mutableStateOf(metadata.artworkUrl.orEmpty()) }
    var trackNumber by remember(metadata.itemId) {
        mutableStateOf(metadata.trackNumber?.toString().orEmpty())
    }
    var discNumber by remember(metadata.itemId) {
        mutableStateOf(metadata.discNumber?.toString().orEmpty())
    }
    var releaseDate by remember(metadata.itemId) {
        mutableStateOf(metadata.releaseDate.orEmpty())
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Edit metadata")

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true
            )

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        when (metadata.itemType) {
                            MetadataItemType.PLAYLIST -> "Owner"
                            else -> "Artist"
                        }
                    )
                },
                singleLine = true
            )

            if (metadata.itemType == MetadataItemType.TRACK) {
                OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Album") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = trackNumber,
                    onValueChange = { trackNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Track number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = discNumber,
                    onValueChange = { discNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Disc number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            if (metadata.itemType == MetadataItemType.ALBUM) {
                OutlinedTextField(
                    value = releaseDate,
                    onValueChange = { releaseDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Release date") },
                    placeholder = { Text("YYYY, YYYY-MM, or YYYY-MM-DD") },
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = artworkUrl,
                onValueChange = { artworkUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cover art URL") },
                singleLine = true
            )

            RespotPrimaryButton(
                text = "Save changes",
                onClick = {
                    onSave(
                        metadata.copy(
                            title = title.trim(),
                            artist = artist.trim(),
                            albumName = albumName.trim().ifBlank { null },
                            artworkUrl = artworkUrl.trim().ifBlank { null },
                            trackNumber = trackNumber.toIntOrNull(),
                            discNumber = discNumber.toIntOrNull(),
                            releaseDate = releaseDate.trim().ifBlank { null }
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerSheet(
    playlists: List<LibraryPlaylist>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = "Add to playlist",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
            items(playlists, key = { it.id }) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(playlist.id) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncArtworkImage(
                        url = playlist.artworkUrl,
                        modifier = Modifier.size(48.dp)
                    )
                    Column {
                        Text(text = playlist.title)
                        Text(text = playlist.owner)
                    }
                }
            }
        }
    }
}
