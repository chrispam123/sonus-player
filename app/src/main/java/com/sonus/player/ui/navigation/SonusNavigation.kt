package com.sonus.player.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sonus.player.ui.library.LibraryScreen
import com.sonus.player.ui.library.LibraryViewModel
import com.sonus.player.ui.player.MiniPlayerBar
import com.sonus.player.ui.player.NowPlayingScreen
import com.sonus.player.ui.player.PlayerViewModel

@Composable
fun SonusNavigation() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Main content area
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = "library"
            ) {
                composable("library") {
                    val libraryViewModel: LibraryViewModel = hiltViewModel()
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        onTrackClick = { track, queue ->
                            playerViewModel.playTrack(track, queue)
                            navController.navigate("now_playing")
                        }
                    )
                }
                composable("now_playing") {
                    NowPlayingScreen(viewModel = playerViewModel)
                }
            }
        }

        // Mini player bar (visible when on library screen and something is playing)
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute != "now_playing") {
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
