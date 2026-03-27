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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.forteur.freepod.playback.PlayerUiState
import com.forteur.freepod.util.LOG_TAG_UI
import com.forteur.freepod.util.debugLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playRequestId: String,
    playerUiState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    debugLog(
        LOG_TAG_UI,
        "PlayerScreen composed | playRequestId=$playRequestId, currentMediaId=${playerUiState.currentMediaId}"
    )

    val positionBucket = playerUiState.currentPositionMs / 5_000L

    LaunchedEffect(
        playerUiState.isConnected,
        playerUiState.playbackState,
        playerUiState.isPlaying,
        playerUiState.currentMediaId,
        playerUiState.title,
        playerUiState.durationMs,
        positionBucket
    ) {
        debugLog(
            LOG_TAG_UI,
            "PlayerScreen UI state | playRequestId=$playRequestId, currentMediaId=${playerUiState.currentMediaId}, positionBucket=${positionBucket * 5}s, isPlaying=${playerUiState.isPlaying}, isConnected=${playerUiState.isConnected}, playbackState=${playerUiState.playbackState}"
        )
        if (playerUiState.title.isBlank()) {
            Log.w(
                LOG_TAG_UI,
                "PlayerScreen metadata title missing from MediaController | playRequestId=$playRequestId"
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
                text = playerUiState.title.ifBlank { "Caricamento episodio..." },
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!playerUiState.artist.isNullOrBlank()) {
                Text(
                    text = playerUiState.artist,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            if (!playerUiState.description.isNullOrBlank()) {
                Text(
                    text = playerUiState.description,
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
                Button(
                    onClick = {
                        debugLog(
                            LOG_TAG_UI,
                            "FREEPOD_RESET_SOURCE ui.seekBackClick | current=${playerUiState.currentPositionMs}, isPlaying=${playerUiState.isPlaying}, playbackState=${playerUiState.playbackState}"
                        )
                        onSeekBack()
                    },
                    enabled = playerUiState.isConnected
                ) {
                    Text("-15s")
                }
                Button(
                    onClick = {
                        debugLog(
                            LOG_TAG_UI,
                            "FREEPOD_PAUSE_SOURCE ui.togglePlayPauseClick | current=${playerUiState.currentPositionMs}, isPlaying=${playerUiState.isPlaying}, playbackState=${playerUiState.playbackState}"
                        )
                        onTogglePlayPause()
                    },
                    enabled = playerUiState.isConnected
                ) {
                    Text(if (playerUiState.isPlaying) "Pause" else "Play")
                }
                Button(
                    onClick = {
                        debugLog(
                            LOG_TAG_UI,
                            "FREEPOD_RESET_SOURCE ui.seekForwardClick | current=${playerUiState.currentPositionMs}, isPlaying=${playerUiState.isPlaying}, playbackState=${playerUiState.playbackState}"
                        )
                        onSeekForward()
                    },
                    enabled = playerUiState.isConnected
                ) {
                    Text("+15s")
                }
            }

            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = playerUiState.currentPositionMs.toFloat(),
                onValueChange = {
                    val target = it.toLong()
                    debugLog(
                        LOG_TAG_UI,
                        "FREEPOD_RESET_SOURCE ui.sliderSeek | target=$target, currentBefore=${playerUiState.currentPositionMs}, isPlaying=${playerUiState.isPlaying}, playbackState=${playerUiState.playbackState}"
                    )
                    onSeekTo(target)
                },
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
