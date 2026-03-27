package com.forteur.freepod.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.forteur.freepod.model.PodcastEpisode
import com.forteur.freepod.util.LOG_TAG_UI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(
    uiState: EpisodeListUiState,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Episodes")
                        Text(
                            text = uiState.podcastTitle,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(innerPadding))
            uiState.errorMessage != null -> ErrorState(
                errorMessage = uiState.errorMessage,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding)
            )
            else -> EpisodeList(
                episodes = uiState.episodes,
                podcastTitle = uiState.podcastTitle,
                onEpisodeClick = onEpisodeClick,
                innerPadding = innerPadding
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = "Caricamento episodi...",
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Errore caricamento feed")
        Text(
            text = errorMessage,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Riprova")
        }
    }
}

@Composable
private fun EpisodeList(
    episodes: List<PodcastEpisode>,
    podcastTitle: String,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    innerPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = innerPadding.calculateTopPadding() + 12.dp,
            end = 16.dp,
            bottom = innerPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(episodes, key = { it.audioUrl }) { episode ->
            EpisodeCard(
                episode = episode,
                onClick = {
                    val mediaId = episode.audioUrl
                    Log.d(
                        LOG_TAG_UI,
                        "Episode click | title=${episode.title}, audioUrl=${episode.audioUrl}, podcastTitle=$podcastTitle, imageUrl=null, mediaId=$mediaId"
                    )
                    onEpisodeClick(episode)
                }
            )
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: PodcastEpisode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleMedium
            )
            if (!episode.pubDateRaw.isNullOrBlank()) {
                Text(
                    text = episode.pubDateRaw,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (!episode.description.isNullOrBlank()) {
                Text(
                    text = episode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
