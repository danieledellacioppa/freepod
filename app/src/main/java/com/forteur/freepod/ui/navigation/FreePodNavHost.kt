package com.forteur.freepod.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.forteur.freepod.ui.player.PlayerScreen
import com.forteur.freepod.ui.screens.EpisodeListScreen
import com.forteur.freepod.ui.screens.EpisodeListViewModel
import androidx.navigation.NavHostController

private const val LIST_ROUTE = "episodes"
private const val PLAYER_ROUTE = "player"
private const val ARG_AUDIO_URL = "audioUrl"

@Composable
fun FreePodNavHost(
    navController: NavHostController,
    episodeListViewModel: EpisodeListViewModel
) {
    val uiState by episodeListViewModel.uiState.collectAsStateWithLifecycle()
    val player = remember(navController.context) { ExoPlayer.Builder(navController.context).build() }

    NavHost(
        navController = navController,
        startDestination = LIST_ROUTE
    ) {
        composable(LIST_ROUTE) {
            EpisodeListScreen(
                uiState = uiState,
                onRetry = episodeListViewModel::loadEpisodes,
                onEpisodeClick = { episode ->
                    navController.navigate("$PLAYER_ROUTE/${Uri.encode(episode.audioUrl)}")
                }
            )
        }

        composable(
            route = "$PLAYER_ROUTE/{$ARG_AUDIO_URL}",
            arguments = listOf(navArgument(ARG_AUDIO_URL) { type = NavType.StringType })
        ) { backStackEntry ->
            val audioUrl = Uri.decode(backStackEntry.arguments?.getString(ARG_AUDIO_URL).orEmpty())
            val episode = episodeListViewModel.findEpisodeByAudioUrl(audioUrl)

            if (episode != null) {
                PlayerScreen(
                    episode = episode,
                    player = player
                )
            }
        }
    }
}
