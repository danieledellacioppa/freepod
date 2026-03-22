package com.forteur.freepod.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.forteur.freepod.model.PodcastEpisode
import com.forteur.freepod.ui.episode.EpisodeListRoute
import com.forteur.freepod.ui.player.PlayerScreen

private const val EPISODE_LIST_ROUTE = "episode_list"
private const val PLAYER_ROUTE = "player"

@Composable
fun FreePodNavGraph(
    selectedEpisode: PodcastEpisode?,
    onEpisodeSelected: (PodcastEpisode) -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = EPISODE_LIST_ROUTE
    ) {
        composable(EPISODE_LIST_ROUTE) {
            EpisodeListRoute(
                onEpisodeClick = { episode ->
                    onEpisodeSelected(episode)
                    navController.navigate(PLAYER_ROUTE)
                }
            )
        }
        composable(PLAYER_ROUTE) {
            PlayerScreen(episode = selectedEpisode)
        }
    }
}
