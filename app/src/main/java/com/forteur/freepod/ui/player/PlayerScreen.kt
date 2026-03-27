package com.forteur.freepod.ui.player

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.forteur.freepod.model.PodcastEpisode
import com.forteur.freepod.playback.PlayerUiState
import com.forteur.freepod.util.LOG_TAG_UI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    episode: PodcastEpisode,
    playRequestId: String,
    playerUiState: PlayerUiState,
    onStartEpisode: (PodcastEpisode, String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    Log.d(
        LOG_TAG_UI,
        "PlayerScreen composed | playRequestId=$playRequestId, selectedEpisodeTitle=${episode.title}, selectedEpisodeAudioUrl=${episode.audioUrl}"
    )

    LaunchedEffect(episode.audioUrl) {
        Log.d(
            LOG_TAG_UI,
            "PlayerScreen LaunchedEffect -> onStartEpisode | playRequestId=$playRequestId, episodeTitle=${episode.title}, audioUrl=${episode.audioUrl}, imageUrl=null"
        )
        onStartEpisode(episode, playRequestId)
    }

    SideEffect {
        Log.d(
            LOG_TAG_UI,
            "PlayerScreen UI state | playRequestId=$playRequestId, isConnected=${playerUiState.isConnected}, playbackState=${playerUiState.playbackState}, isPlaying=${playerUiState.isPlaying}, currentMediaId=${playerUiState.currentMediaId}, currentMedia=${playerUiState.currentMediaItemSummary}, titleShown=${playerUiState.title.ifBlank { episode.title }}, position=${playerUiState.currentPositionMs}, duration=${playerUiState.durationMs}"
        )
        if (playerUiState.title.isBlank() && episode.title.isBlank()) {
            Log.w(
                LOG_TAG_UI,
                "PlayerScreen metadata missing -> possible empty UI | playRequestId=$playRequestId"
            )
        }
        if (!playerUiState.isConnected) {
            Log.w(LOG_TAG_UI, "PlayerScreen controller not available yet | playRequestId=$playRequestId")
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
                text = playerUiState.title.ifBlank { episode.title },
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val descriptionText = playerUiState.description ?: episode.description
            if (!descriptionText.isNullOrBlank()) {
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!playerUiState.isConnected) {
                CircularProgressIndicator()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onSeekBack, enabled = playerUiState.isConnected) {
                    Text("-15s")
                }
                Button(onClick = onTogglePlayPause, enabled = playerUiState.isConnected) {
                    Text(if (playerUiState.isPlaying) "Pause" else "Play")
                }
                Button(onClick = onSeekForward, enabled = playerUiState.isConnected) {
                    Text("+15s")
                }
            }

            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = playerUiState.currentPositionMs.toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                enabled = playerUiState.isConnected,
                valueRange = 0f..(playerUiState.durationMs.takeIf { it > 0 }?.toFloat() ?: 1f)
            )

            Text(
                text = "${formatTime(playerUiState.currentPositionMs)} / ${formatTime(playerUiState.durationMs)}",
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
