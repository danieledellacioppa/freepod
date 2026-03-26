package com.forteur.freepod.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.forteur.freepod.model.PodcastSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    uiState: DiscoverUiState,
    onQueryChanged: (String) -> Unit,
    onRetry: () -> Unit,
    onPodcastClick: (PodcastSummary) -> Unit,
    onOpenMyPodcasts: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover") },
                actions = {
                    TextButton(onClick = onOpenMyPodcasts) {
                        Text("My Podcasts")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.query,
                onValueChange = onQueryChanged,
                singleLine = true,
                placeholder = { Text("Cerca podcast") },
                label = { Text("Search") }
            )

            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text("Caricamento podcast...", modifier = Modifier.padding(top = 12.dp))
                    }
                }

                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Errore durante la discover")
                        Text(uiState.errorMessage, modifier = Modifier.padding(top = 8.dp))
                        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Riprova")
                        }
                    }
                }

                uiState.podcasts.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Nessun podcast trovato")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.podcasts, key = { it.feedUrl }) { podcast ->
                            PodcastCard(
                                podcast = podcast,
                                onClick = { onPodcastClick(podcast) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastCard(
    podcast: PodcastSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = podcast.imageUrl,
                contentDescription = podcast.title,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!podcast.author.isNullOrBlank()) {
                    Text(
                        text = podcast.author,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (!podcast.description.isNullOrBlank()) {
                    Text(
                        text = podcast.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}
