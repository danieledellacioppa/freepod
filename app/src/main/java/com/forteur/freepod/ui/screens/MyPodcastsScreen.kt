package com.forteur.freepod.ui.screens

import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.forteur.freepod.model.SubscribedPodcast
import com.forteur.freepod.util.LOG_TAG_UI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPodcastsScreen(
    uiState: MyPodcastsUiState,
    onRefresh: () -> Unit,
    onPodcastClick: (SubscribedPodcast) -> Unit,
    onRemovePodcasts: (Set<String>) -> Unit
) {
    var selectedFeedUrls by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    LaunchedEffect(uiState.podcasts) {
        val availableFeedUrls = uiState.podcasts.mapTo(mutableSetOf()) { it.feedUrl }
        selectedFeedUrls = selectedFeedUrls.filterTo(mutableSetOf()) { it in availableFeedUrls }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Podcasts") },
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirmation = true },
                        enabled = selectedFeedUrls.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Rimuovi selezionati"
                        )
                    }
                }
            )
        }
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Checkbox(
                                checked = podcast.feedUrl in selectedFeedUrls,
                                onCheckedChange = { isChecked ->
                                    selectedFeedUrls = if (isChecked) {
                                        selectedFeedUrls + podcast.feedUrl
                                    } else {
                                        selectedFeedUrls - podcast.feedUrl
                                    }
                                }
                            )
                            AsyncImage(
                                model = podcast.imageUrl,
                                contentDescription = podcast.title,
                                modifier = Modifier.size(64.dp),
                                contentScale = ContentScale.Crop,
                                onLoading = {
                                    Log.d(
                                        LOG_TAG_UI,
                                        "Podcast cover loading | title=${podcast.title}, imageUrl=${podcast.imageUrl}"
                                    )
                                },
                                onError = {
                                    Log.w(
                                        LOG_TAG_UI,
                                        "Podcast cover load failed (audio playback unaffected) | title=${podcast.title}, imageUrl=${podcast.imageUrl}"
                                    )
                                }
                            )
                            Column(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .clickable { onPodcastClick(podcast) }
                            ) {
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

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Conferma eliminazione") },
                text = {
                    Text("Vuoi davvero rimuovere ${selectedFeedUrls.size} podcast dai salvati?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onRemovePodcasts(selectedFeedUrls)
                            selectedFeedUrls = emptySet()
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text("Rimuovi")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Annulla")
                    }
                }
            )
        }
    }
}
