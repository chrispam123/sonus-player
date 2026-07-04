package com.sonus.player.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sonus.player.ui.history.HistoryScreen
import com.sonus.player.ui.history.HistoryViewModel
import com.sonus.player.ui.library.LibraryScreen
import com.sonus.player.ui.library.LibraryViewModel
import com.sonus.player.ui.lyrics.LyricsScreen
import com.sonus.player.ui.lyrics.LyricsViewModel
import com.sonus.player.ui.player.MiniPlayerBar
import com.sonus.player.ui.player.NowPlayingScreen
import com.sonus.player.ui.player.PlayerViewModel
import com.sonus.player.ui.playlist.PlaylistDetailScreen
import com.sonus.player.ui.playlist.PlaylistViewModel
import com.sonus.player.ui.playlist.PlaylistsScreen
import com.sonus.player.ui.settings.SettingsScreen
import com.sonus.player.ui.settings.SettingsViewModel
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("library", "Música", Icons.Rounded.LibraryMusic),
    BottomNavItem("playlists", "Playlists", Icons.Rounded.QueueMusic),
    BottomNavItem("history", "Historial", Icons.Rounded.History),
    BottomNavItem("settings", "Ajustes", Icons.Rounded.Settings)
)

@Composable
fun SonusNavigation() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isFullScreen = currentRoute == "now_playing" || currentRoute == "lyrics"

    Column(modifier = Modifier.fillMaxSize()) {
        // Main content area
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = "library"
            ) {
                composable("library") {
                    val libraryViewModel: LibraryViewModel = hiltViewModel()
                    val playlistViewModel: PlaylistViewModel = hiltViewModel()
                    val playlistState by playlistViewModel.uiState.collectAsState()
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        playlists = playlistState.playlists,
                        onTrackClick = { track, queue ->
                            playerViewModel.playTrack(track, queue)
                            navController.navigate("now_playing")
                        },
                        onAddToPlaylist = { trackId, playlistId ->
                            playlistViewModel.addTrackToPlaylist(playlistId, trackId)
                        }
                    )
                }
                composable("now_playing") {
                    val playlistViewModel: PlaylistViewModel = hiltViewModel()
                    val playlistState by playlistViewModel.uiState.collectAsState()
                    NowPlayingScreen(
                        viewModel = playerViewModel,
                        onLyricsClick = { navController.navigate("lyrics") },
                        playlists = playlistState.playlists,
                        onAddToPlaylist = { trackId, playlistId ->
                            playlistViewModel.addTrackToPlaylist(playlistId, trackId)
                        }
                    )
                }
                composable("lyrics") {
                    val lyricsViewModel: LyricsViewModel = hiltViewModel()
                    LyricsScreen(viewModel = lyricsViewModel)
                }
                composable("playlists") {
                    val playlistViewModel: PlaylistViewModel = hiltViewModel()
                    PlaylistsScreen(
                        viewModel = playlistViewModel,
                        onPlaylistClick = { playlistId ->
                            navController.navigate("playlist_detail/$playlistId")
                        }
                    )
                }
                composable("playlist_detail/{playlistId}") { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull() ?: return@composable
                    val playlistViewModel: PlaylistViewModel = hiltViewModel()
                    playlistViewModel.selectPlaylist(playlistId)
                    PlaylistDetailScreen(
                        viewModel = playlistViewModel,
                        onTrackClick = { track, queue ->
                            playerViewModel.playTrack(track, queue)
                            navController.navigate("now_playing")
                        }
                    )
                }
                composable("history") {
                    val historyViewModel: HistoryViewModel = hiltViewModel()
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onTrackClick = { track ->
                            playerViewModel.playTrack(track, listOf(track))
                            navController.navigate("now_playing")
                        }
                    )
                }
                composable("settings") {
                    val settingsViewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(viewModel = settingsViewModel)
                }
            }
        }

        // Mini player (visible when not in full-screen player views)
        if (!isFullScreen && playerState.currentTrack != null) {
            val progress = if (playerState.progress.durationMs > 0) {
                playerState.progress.positionMs.toFloat() / playerState.progress.durationMs.toFloat()
            } else 0f

            MiniPlayerBar(
                track = playerState.currentTrack,
                isPlaying = playerState.isPlaying,
                progress = progress,
                onTogglePlayPause = { playerViewModel.togglePlayPause() },
                onClick = { navController.navigate("now_playing") }
            )
        }

        // Bottom navigation (hidden in full-screen views)
        if (!isFullScreen) {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
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
    }
}
