package com.forteur.freepod.ui.episode

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.forteur.freepod.model.PodcastEpisode

@Composable
fun EpisodeListRoute(
    onEpisodeClick: (PodcastEpisode) -> Unit,
    viewModel: EpisodeListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    EpisodeListScreen(
        state = state,
        onEpisodeClick = onEpisodeClick,
        onRetryClick = viewModel::loadEpisodes
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(
    state: EpisodeListUiState,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onRetryClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "FreePod")
                        Text(
                            text = state.podcastName,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            state.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Errore caricamento feed")
                    Text(text = state.errorMessage)
                    Button(onClick = onRetryClick, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Riprova")
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.episodes) { episode ->
                        EpisodeItem(episode = episode, onClick = { onEpisodeClick(episode) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeItem(
    episode: PodcastEpisode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            episode.pubDate?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (episode.description.isNotBlank()) {
                Text(
                    text = episode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
