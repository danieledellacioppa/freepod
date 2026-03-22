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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.forteur.freepod.model.PodcastEpisode
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(episode: PodcastEpisode?) {
    if (episode == null) {
        Scaffold(topBar = { TopAppBar(title = { Text("Player") }) }) { paddingValues ->
            Text(
                text = "Nessun episodio selezionato",
                modifier = Modifier.padding(paddingValues).padding(16.dp)
            )
        }
        return
    }

    val context = LocalContext.current
    val player = remember(episode.audioUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(episode.audioUrl))
            prepare()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var sliderValue by remember { mutableFloatStateOf(0f) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = if (player.duration > 0) player.duration else 0L
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            currentPosition = player.currentPosition
            duration = if (player.duration > 0) player.duration else 0L
            sliderValue = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
            delay(500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Player") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (episode.description.isNotBlank()) {
                Text(
                    text = episode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    val target = (duration * sliderValue).toLong()
                    player.seekTo(target)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(currentPosition))
                Text(formatTime(duration))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { player.seekTo((player.currentPosition - 15_000).coerceAtLeast(0)) }) {
                    Text("-15s")
                }
                Button(onClick = { if (isPlaying) player.pause() else player.play() }) {
                    Text(if (isPlaying) "Pause" else "Play")
                }
                Button(onClick = {
                    val target = if (duration > 0) {
                        (player.currentPosition + 15_000).coerceAtMost(duration)
                    } else {
                        player.currentPosition + 15_000
                    }
                    player.seekTo(target)
                }) {
                    Text("+15s")
                }
            }
        }
    }
}

private fun formatTime(valueMs: Long): String {
    if (valueMs <= 0L) return "00:00"
    val totalSeconds = valueMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
