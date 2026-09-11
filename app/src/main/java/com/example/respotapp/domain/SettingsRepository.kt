package com.example.respotapp.domain

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ui_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val COMPACT_LAYOUT = booleanPreferencesKey("compact_layout")
        val SHOW_ALBUM_ART = booleanPreferencesKey("show_album_art")
        val NOW_PLAYING_LAYOUT = stringPreferencesKey("now_playing_layout")
        val NOW_PLAYING_ART_BG = booleanPreferencesKey("now_playing_art_bg")
        val NOW_PLAYING_PROGRESS = booleanPreferencesKey("now_playing_progress")
        val NOW_PLAYING_LARGE_ART = booleanPreferencesKey("now_playing_large_art")
    }

    val uiSettings: Flow<UiSettings> = context.dataStore.data.map { prefs ->
        UiSettings(
            darkMode = prefs[Keys.DARK_MODE]?.let {
                runCatching { DarkModePreference.valueOf(it) }.getOrDefault(DarkModePreference.SYSTEM)
            } ?: DarkModePreference.SYSTEM,
            useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            accentColorHex = prefs[Keys.ACCENT_COLOR] ?: "#1EEBD8",
            compactLayout = prefs[Keys.COMPACT_LAYOUT] ?: false,
            showAlbumArtInList = prefs[Keys.SHOW_ALBUM_ART] ?: true,
            nowPlayingLayout = prefs[Keys.NOW_PLAYING_LAYOUT]?.let {
                runCatching { NowPlayingLayout.valueOf(it) }.getOrDefault(NowPlayingLayout.CLASSIC)
            } ?: NowPlayingLayout.CLASSIC,
            nowPlayingShowArtBackground = prefs[Keys.NOW_PLAYING_ART_BG] ?: true,
            nowPlayingShowProgress = prefs[Keys.NOW_PLAYING_PROGRESS] ?: true,
            nowPlayingLargeArtwork = prefs[Keys.NOW_PLAYING_LARGE_ART] ?: true
        )
    }

    suspend fun updateDarkMode(mode: DarkModePreference) {
        context.dataStore.edit { it[Keys.DARK_MODE] = mode.name }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun updateAccentColor(hex: String) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = hex }
    }

    suspend fun updateCompactLayout(enabled: Boolean) {
        context.dataStore.edit { it[Keys.COMPACT_LAYOUT] = enabled }
    }

    suspend fun updateShowAlbumArt(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_ALBUM_ART] = enabled }
    }

    suspend fun updateNowPlayingLayout(layout: NowPlayingLayout) {
        context.dataStore.edit { it[Keys.NOW_PLAYING_LAYOUT] = layout.name }
    }

    suspend fun updateNowPlayingShowArtBackground(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOW_PLAYING_ART_BG] = enabled }
    }

    suspend fun updateNowPlayingShowProgress(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOW_PLAYING_PROGRESS] = enabled }
    }

    suspend fun updateNowPlayingLargeArtwork(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOW_PLAYING_LARGE_ART] = enabled }
    }
}
