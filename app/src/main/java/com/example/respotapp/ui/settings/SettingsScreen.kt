package com.example.respotapp.ui.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.respotapp.domain.DarkModePreference
import com.example.respotapp.domain.NowPlayingLayout
import com.example.respotapp.domain.UiSettings
import com.example.respotapp.ui.RespotScreenBackground
import com.example.respotapp.ui.parseAccentColor
import com.example.respotapp.ui.theme.RespotDanger
import com.example.respotapp.ui.theme.RespotSurfaceSecondary
import com.example.respotapp.ui.theme.RespotTextSecondary

private val accentPresets = listOf(
    "#1EEBD8" to "Respot Teal",
    "#1DB954" to "Spotify Green",
    "#6366F1" to "Indigo",
    "#F59E0B" to "Amber",
    "#EF4444" to "Red",
    "#8B5CF6" to "Violet"
)

@Composable
fun SettingsScreen(
    uiSettings: UiSettings,
    displayName: String?,
    onDarkModeChange: (DarkModePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAccentColorChange: (String) -> Unit,
    onCompactLayoutChange: (Boolean) -> Unit,
    onShowAlbumArtChange: (Boolean) -> Unit,
    onNowPlayingLayoutChange: (NowPlayingLayout) -> Unit,
    onNowPlayingShowArtBackgroundChange: (Boolean) -> Unit,
    onNowPlayingShowProgressChange: (Boolean) -> Unit,
    onNowPlayingLargeArtworkChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    RespotScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 56.dp, bottom = 32.dp)
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(RespotSurfaceSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Column {
                    Text(
                        text = displayName ?: "Respot user",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "View account",
                        style = MaterialTheme.typography.bodySmall,
                        color = RespotTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            SectionTitle("Appearance")

            SettingRow(
                title = "Dark mode",
                subtitle = uiSettings.darkMode.name.lowercase().replaceFirstChar { it.uppercase() }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkModePreference.entries.forEach { mode ->
                        val selected = uiSettings.darkMode == mode
                        Text(
                            text = mode.name.lowercase(),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primary else RespotSurfaceSecondary)
                                .clickable { onDarkModeChange(mode) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White
                        )
                    }
                }
            }

            SettingToggle(
                title = "Dynamic color",
                subtitle = "Use system Material You colors (overrides accent)",
                checked = uiSettings.useDynamicColor,
                onCheckedChange = onDynamicColorChange
            )
            SettingToggle(
                title = "Compact layout",
                subtitle = "Reduce spacing in lists",
                checked = uiSettings.compactLayout,
                onCheckedChange = onCompactLayoutChange
            )
            SettingToggle(
                title = "Show album art in lists",
                subtitle = "Display artwork thumbnails",
                checked = uiSettings.showAlbumArtInList,
                onCheckedChange = onShowAlbumArtChange
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("Accent color")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                accentPresets.forEach { (hex, _) ->
                    val color = parseAccentColor(hex)
                    val selected = uiSettings.accentColorHex.equals(hex, ignoreCase = true) ||
                        (uiSettings.accentColorHex.isBlank() && hex == "#1EEBD8")
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onAccentColorChange(hex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Now Playing")
            SettingRow(
                title = "Layout style",
                subtitle = uiSettings.nowPlayingLayout.name.lowercase()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NowPlayingLayout.entries.forEach { layout ->
                        val selected = uiSettings.nowPlayingLayout == layout
                        Text(
                            text = layout.name.lowercase(),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primary else RespotSurfaceSecondary)
                                .clickable { onNowPlayingLayoutChange(layout) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White
                        )
                    }
                }
            }
            SettingToggle(
                title = "Artwork background",
                subtitle = "Gradient background from album art",
                checked = uiSettings.nowPlayingShowArtBackground,
                onCheckedChange = onNowPlayingShowArtBackgroundChange
            )
            SettingToggle(
                title = "Show progress bar",
                subtitle = "Display track timeline in Now Playing",
                checked = uiSettings.nowPlayingShowProgress,
                onCheckedChange = onNowPlayingShowProgressChange
            )
            SettingToggle(
                title = "Large artwork",
                subtitle = "Use bigger album art in Now Playing",
                checked = uiSettings.nowPlayingLargeArtwork,
                onCheckedChange = onNowPlayingLargeArtworkChange
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Account")
            Text(
                text = "Log out",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = RespotDanger
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = RespotTextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = RespotTextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
