package com.forteur.freepod.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.forteur.freepod.model.PodcastEpisode
import kotlinx.coroutines.delay

private const val SEEK_MS = 15_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    episode: PodcastEpisode
) {
    val context = LocalContext.current
    val player = remember(episode.audioUrl) { ExoPlayer.Builder(context).build() }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(player, episode.audioUrl) {
        player.setMediaItem(MediaItem.fromUri(episode.audioUrl))
        player.prepare()
        player.playWhenReady = true

        while (true) {
            isPlaying = player.isPlaying
            currentPositionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0 } ?: 0L
            delay(500)
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Player") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!episode.description.isNullOrBlank()) {
                Text(
                    text = episode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = {
                    player.seekTo((player.currentPosition - SEEK_MS).coerceAtLeast(0L))
                }) {
                    Text("-15s")
                }
                Button(onClick = {
                    if (player.isPlaying) player.pause() else player.play()
                }) {
                    Text(if (isPlaying) "Pause" else "Play")
                }
                Button(onClick = {
                    val target = player.currentPosition + SEEK_MS
                    val safeTarget = if (player.duration > 0) {
                        target.coerceAtMost(player.duration)
                    } else {
                        target
                    }
                    player.seekTo(safeTarget)
                }) {
                    Text("+15s")
                }
            }

            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = currentPositionMs.toFloat(),
                onValueChange = { player.seekTo(it.toLong()) },
                valueRange = 0f..(durationMs.takeIf { it > 0 }?.toFloat() ?: 1f)
            )

            Text(
                text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
