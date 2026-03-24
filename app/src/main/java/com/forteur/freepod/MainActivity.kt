package com.forteur.freepod

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import com.forteur.freepod.data.PodcastIndexAuthInterceptor
import com.forteur.freepod.data.PodcastIndexService
import com.forteur.freepod.data.PodcastRepository
import com.forteur.freepod.data.SubscriptionLocalDataSource
import com.forteur.freepod.data.SubscriptionRepository
import com.forteur.freepod.playback.PlaybackControllerViewModel
import com.forteur.freepod.ui.navigation.FreePodNavHost
import com.forteur.freepod.ui.screens.DiscoverViewModel
import com.forteur.freepod.ui.screens.EpisodeListViewModel
import com.forteur.freepod.ui.screens.MyPodcastsViewModel
import com.forteur.freepod.ui.theme.FreePodTheme
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FreePodTheme {
                val rssClient = OkHttpClient()
                val podcastIndexClient = OkHttpClient.Builder()
                    .addInterceptor(PodcastIndexAuthInterceptor())
                    .build()

                val rssRepository = PodcastRepository(rssClient)
                val podcastIndexService = PodcastIndexService(podcastIndexClient)
                val subscriptionRepository = SubscriptionRepository(
                    SubscriptionLocalDataSource(applicationContext)
                )

                val discoverViewModel: DiscoverViewModel = viewModel(
                    factory = DiscoverViewModel.factory(
                        podcastIndexService = podcastIndexService,
                        subscriptionRepository = subscriptionRepository
                    )
                )
                val myPodcastsViewModel: MyPodcastsViewModel = viewModel(
                    factory = MyPodcastsViewModel.factory(subscriptionRepository)
                )
                val episodeListViewModel: EpisodeListViewModel = viewModel(
                    factory = EpisodeListViewModel.factory(rssRepository)
                )
                val playbackControllerViewModel: PlaybackControllerViewModel = viewModel(
                    factory = PlaybackControllerViewModel.factory(application)
                )

                FreePodApp(
                    discoverViewModel = discoverViewModel,
                    myPodcastsViewModel = myPodcastsViewModel,
                    episodeListViewModel = episodeListViewModel,
                    playbackControllerViewModel = playbackControllerViewModel
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun FreePodApp(
    discoverViewModel: DiscoverViewModel,
    myPodcastsViewModel: MyPodcastsViewModel,
    episodeListViewModel: EpisodeListViewModel,
    playbackControllerViewModel: PlaybackControllerViewModel
) {
    val navController = rememberNavController()
    FreePodNavHost(
        navController = navController,
        discoverViewModel = discoverViewModel,
        myPodcastsViewModel = myPodcastsViewModel,
        episodeListViewModel = episodeListViewModel,
        playbackControllerViewModel = playbackControllerViewModel
    )
}

@Preview(showBackground = true)
@Composable
private fun FreePodPreview() {
    FreePodTheme {
    }
}
