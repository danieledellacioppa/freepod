package com.forteur.freepod.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
fun PodcastDetailScreen(
    podcast: PodcastSummary,
    isSubscribed: Boolean,
    onSubscribeClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Podcast detail") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = podcast.imageUrl,
                contentDescription = podcast.title,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (!podcast.author.isNullOrBlank()) {
                Text(
                    text = podcast.author,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = podcast.description ?: "Nessuna descrizione disponibile",
                style = MaterialTheme.typography.bodyMedium
            )
            if (isSubscribed) {
                Text(
                    text = "Già sottoscritto",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(onClick = onSubscribeClick) {
                    Text("Subscribe")
                }
            }
        }
    }
}
