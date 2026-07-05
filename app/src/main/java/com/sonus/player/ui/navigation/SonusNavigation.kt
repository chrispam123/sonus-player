package com.sonus.player.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
import com.sonus.player.ui.theme.HankenGrotesk

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

// Tabs matching your mockups: Player | Librería | Listas | Sistema
val bottomNavItems = listOf(
    BottomNavItem("now_playing", "Player", Icons.Rounded.PlayCircle),
    BottomNavItem("library", "Librería", Icons.Rounded.LibraryMusic),
    BottomNavItem("playlists", "Listas", Icons.Rounded.QueueMusic),
    BottomNavItem("settings", "Sistema", Icons.Rounded.Equalizer)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SonusNavigation() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isFullScreen = currentRoute == "lyrics" || currentRoute?.startsWith("playlist_detail") == true

    Scaffold(
        topBar = {
            if (!isFullScreen) {
                TopAppBar(
                    title = {
                        Text(
                            text = "SONUS",
                            fontFamily = HankenGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 2.sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        bottomBar = {
            if (!isFullScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(item.icon, contentDescription = item.label)
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content
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

            // Mini player (visible when not on Player tab and something is playing)
            if (currentRoute != "now_playing" && !isFullScreen && playerState.currentTrack != null) {
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
        }
    }
}
