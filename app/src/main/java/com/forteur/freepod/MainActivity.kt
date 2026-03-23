package com.forteur.freepod

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.forteur.freepod.data.PodcastRepository
import com.forteur.freepod.playback.PlaybackControllerViewModel
import com.forteur.freepod.ui.navigation.FreePodNavHost
import com.forteur.freepod.ui.screens.EpisodeListViewModel
import com.forteur.freepod.ui.theme.FreePodTheme
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FreePodTheme {
                val repository = PodcastRepository(OkHttpClient())
                val episodeListViewModel: EpisodeListViewModel = viewModel(
                    factory = EpisodeListViewModel.factory(repository)
                )
                val playbackControllerViewModel: PlaybackControllerViewModel = viewModel(
                    factory = PlaybackControllerViewModel.factory(application)
                )
                FreePodApp(
                    episodeListViewModel = episodeListViewModel,
                    playbackControllerViewModel = playbackControllerViewModel
                )
            }
        }
    }
}

@Composable
private fun FreePodApp(
    episodeListViewModel: EpisodeListViewModel,
    playbackControllerViewModel: PlaybackControllerViewModel
) {
    val navController = rememberNavController()
    FreePodNavHost(
        navController = navController,
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
