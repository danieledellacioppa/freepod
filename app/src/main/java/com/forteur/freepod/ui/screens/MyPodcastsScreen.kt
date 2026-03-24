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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.forteur.freepod.model.SubscribedPodcast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPodcastsScreen(
    uiState: MyPodcastsUiState,
    onRefresh: () -> Unit,
    onPodcastClick: (SubscribedPodcast) -> Unit
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Podcasts") }) }
    ) { innerPadding ->
        if (uiState.podcasts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Nessun podcast sottoscritto")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.podcasts, key = { it.feedUrl }) { podcast ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPodcastClick(podcast) }
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            AsyncImage(
                                model = podcast.imageUrl,
                                contentDescription = podcast.title,
                                modifier = Modifier.size(64.dp),
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
                            }
                        }
                    }
                }
            }
        }
    }
}
