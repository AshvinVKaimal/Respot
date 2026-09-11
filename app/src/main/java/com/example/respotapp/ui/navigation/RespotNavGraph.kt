package com.example.respotapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class RespotRoute(val route: String) {
    data object Home : RespotRoute("home")
    data object Library : RespotRoute("library")
    data object Search : RespotRoute("search")
    data object Settings : RespotRoute("settings")
    data object ArtistDetail : RespotRoute("artist/{artistId}") {
        fun createRoute(artistId: String) = "artist/$artistId"
    }
    data object AlbumDetail : RespotRoute("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }
    data object PlaylistDetail : RespotRoute("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = RespotRoute.Search.route,
        label = "Search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    BottomNavItem(
        route = RespotRoute.Library.route,
        label = "Library",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic
    ),
    BottomNavItem(
        route = RespotRoute.Settings.route,
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)

val mainTabRoutes = bottomNavItems.map { it.route }
