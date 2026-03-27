package com.forteur.freepod.ui.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.forteur.freepod.playback.PlaybackControllerViewModel
import com.forteur.freepod.ui.player.PlayerScreen
import com.forteur.freepod.ui.screens.DiscoverViewModel
import com.forteur.freepod.ui.screens.DiscoverScreen
import com.forteur.freepod.ui.screens.EpisodeListScreen
import com.forteur.freepod.ui.screens.EpisodeListViewModel
import com.forteur.freepod.ui.screens.MyPodcastsScreen
import com.forteur.freepod.ui.screens.MyPodcastsViewModel
import com.forteur.freepod.ui.screens.PodcastDetailScreen
import com.forteur.freepod.util.LOG_TAG_NAV
import com.forteur.freepod.util.newPlayRequestId

private const val DISCOVER_ROUTE = "discover"
private const val PODCAST_DETAIL_ROUTE = "podcastDetail"
private const val MY_PODCASTS_ROUTE = "myPodcasts"
private const val EPISODES_ROUTE = "episodes"
private const val PLAYER_ROUTE = "player"
private const val ARG_FEED_URL = "feedUrl"
private const val ARG_PODCAST_TITLE = "podcastTitle"
private const val ARG_PLAY_REQUEST_ID = "playRequestId"

@Composable
fun FreePodNavHost(
    navController: NavHostController,
    discoverViewModel: DiscoverViewModel,
    myPodcastsViewModel: MyPodcastsViewModel,
    episodeListViewModel: EpisodeListViewModel,
    playbackControllerViewModel: PlaybackControllerViewModel
) {
    val discoverUiState by discoverViewModel.uiState.collectAsStateWithLifecycle()
    val myPodcastsUiState by myPodcastsViewModel.uiState.collectAsStateWithLifecycle()
    val episodesUiState by episodeListViewModel.uiState.collectAsStateWithLifecycle()
    val playerUiState by playbackControllerViewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = DISCOVER_ROUTE
    ) {
        composable(DISCOVER_ROUTE) {
            DiscoverScreen(
                uiState = discoverUiState,
                onQueryChanged = discoverViewModel::onQueryChanged,
                onRetry = discoverViewModel::retry,
                onOpenMyPodcasts = { navController.navigate(MY_PODCASTS_ROUTE) },
                onPodcastClick = { podcast ->
                    discoverViewModel.selectPodcast(podcast)
                    navController.navigate(PODCAST_DETAIL_ROUTE)
                }
            )
        }

        composable(PODCAST_DETAIL_ROUTE) {
            val selectedPodcast = discoverUiState.selectedPodcast
            if (selectedPodcast != null) {
                PodcastDetailScreen(
                    podcast = selectedPodcast,
                    isSubscribed = discoverViewModel.isSelectedPodcastSubscribed(),
                    onSubscribeClick = {
                        discoverViewModel.subscribeSelectedPodcast()
                        myPodcastsViewModel.refresh()
                    }
                )
            }
        }

        composable(MY_PODCASTS_ROUTE) {
            MyPodcastsScreen(
                uiState = myPodcastsUiState,
                onRefresh = myPodcastsViewModel::refresh,
                onPodcastClick = { podcast ->
                    navController.navigate(
                        "$EPISODES_ROUTE/${Uri.encode(podcast.feedUrl)}/${Uri.encode(podcast.title)}"
                    )
                },
                onRemovePodcast = { podcast ->
                    myPodcastsViewModel.unsubscribe(podcast.feedUrl)
                }
            )
        }

        composable(
            route = "$EPISODES_ROUTE/{$ARG_FEED_URL}/{$ARG_PODCAST_TITLE}",
            arguments = listOf(
                navArgument(ARG_FEED_URL) { type = NavType.StringType },
                navArgument(ARG_PODCAST_TITLE) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val feedUrl = Uri.decode(backStackEntry.arguments?.getString(ARG_FEED_URL).orEmpty())
            val podcastTitle = Uri.decode(backStackEntry.arguments?.getString(ARG_PODCAST_TITLE).orEmpty())

            if (episodesUiState.feedUrl != feedUrl && feedUrl.isNotBlank()) {
                episodeListViewModel.loadEpisodes(feedUrl = feedUrl, podcastTitle = podcastTitle)
            }

            EpisodeListScreen(
                uiState = episodesUiState,
                onRetry = { episodeListViewModel.loadEpisodes(feedUrl, podcastTitle) },
                onEpisodeClick = { episode ->
                    val playRequestId = newPlayRequestId()
                    playbackControllerViewModel.playEpisode(
                        episode = episode,
                        podcastTitle = podcastTitle,
                        feedUrl = feedUrl,
                        playRequestId = playRequestId
                    )
                    Log.d(
                        LOG_TAG_NAV,
                        "Navigate to player | playRequestId=$playRequestId, title=${episode.title}, mediaSource=MediaController, feedUrl=$feedUrl, podcastTitle=$podcastTitle"
                    )
                    navController.navigate("$PLAYER_ROUTE/${Uri.encode(playRequestId)}")
                }
            )
        }

        composable(
            route = "$PLAYER_ROUTE/{$ARG_PLAY_REQUEST_ID}",
            arguments = listOf(
                navArgument(ARG_PLAY_REQUEST_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val playRequestId =
                Uri.decode(backStackEntry.arguments?.getString(ARG_PLAY_REQUEST_ID).orEmpty())
            Log.d(
                LOG_TAG_NAV,
                "Player destination entered | playRequestId=$playRequestId, sourceOfTruth=MediaController, currentMediaId=${playerUiState.currentMediaId}, current=${playerUiState.currentMediaItemSummary}"
            )

            PlayerScreen(
                playRequestId = playRequestId,
                playerUiState = playerUiState,
                onTogglePlayPause = playbackControllerViewModel::togglePlayPause,
                onSeekBack = playbackControllerViewModel::seekBack,
                onSeekForward = playbackControllerViewModel::seekForward,
                onSeekTo = playbackControllerViewModel::seekTo
            )
        }
    }
}
