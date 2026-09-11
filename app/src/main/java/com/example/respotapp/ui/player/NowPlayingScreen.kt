package com.example.respotapp.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.respotapp.domain.NowPlayingLayout
import com.example.respotapp.domain.UiSettings
import com.example.respotapp.playback.PlayerUiState
import com.example.respotapp.ui.AsyncArtworkImage
import com.example.respotapp.ui.theme.RespotTextSecondary

@Composable
fun NowPlayingScreen(
    playerState: PlayerUiState,
    uiSettings: UiSettings,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientTop = if (uiSettings.nowPlayingShowArtBackground) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        gradientTop,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 40.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = playerState.albumTitle.ifBlank { "Now Playing" },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = "Download for offline",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (uiSettings.nowPlayingLayout) {
                NowPlayingLayout.MINIMAL -> MinimalNowPlayingContent(playerState, uiSettings)
                NowPlayingLayout.IMMERSIVE -> ImmersiveNowPlayingContent(playerState, uiSettings)
                NowPlayingLayout.CLASSIC -> ClassicNowPlayingContent(playerState, uiSettings)
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiSettings.nowPlayingShowProgress && playerState.durationMs > 0) {
                Slider(
                    value = playerState.positionMs.toFloat().coerceIn(
                        0f,
                        playerState.durationMs.toFloat().coerceAtLeast(1f)
                    ),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..playerState.durationMs.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = formatDuration(playerState.positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = RespotTextSecondary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatDuration(playerState.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = RespotTextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = RespotTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onSkipPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Black
                        )
                    }
                }
                IconButton(onClick = onSkipNext, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = RespotTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassicNowPlayingContent(playerState: PlayerUiState, uiSettings: UiSettings) {
    val artModifier = if (uiSettings.nowPlayingLargeArtwork) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .aspectRatio(1f)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .aspectRatio(1f)
    }

    AsyncArtworkImage(
        url = playerState.artworkUrl,
        contentDescription = playerState.title,
        modifier = artModifier,
        cornerRadius = 8.dp
    )
    Spacer(modifier = Modifier.height(28.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playerState.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = playerState.artist,
                style = MaterialTheme.typography.titleMedium,
                color = RespotTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { }) {
            Icon(
                Icons.Default.FavoriteBorder,
                contentDescription = "Like",
                tint = RespotTextSecondary
            )
        }
    }
}

@Composable
private fun MinimalNowPlayingContent(playerState: PlayerUiState, uiSettings: UiSettings) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiSettings.nowPlayingLargeArtwork) {
            AsyncArtworkImage(
                url = playerState.artworkUrl,
                contentDescription = playerState.title,
                modifier = Modifier.size(180.dp),
                cornerRadius = 8.dp
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
        Text(
            text = playerState.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playerState.artist,
            style = MaterialTheme.typography.titleLarge,
            color = RespotTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ImmersiveNowPlayingContent(playerState: PlayerUiState, uiSettings: UiSettings) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (uiSettings.nowPlayingLargeArtwork) 360.dp else 280.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        AsyncArtworkImage(
            url = playerState.artworkUrl,
            contentDescription = playerState.title,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 8.dp
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = playerState.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = playerState.artist,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
