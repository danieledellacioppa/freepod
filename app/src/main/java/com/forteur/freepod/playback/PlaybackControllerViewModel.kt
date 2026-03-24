package com.forteur.freepod.playback

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.forteur.freepod.model.PodcastEpisode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

private const val SEEK_MS = 15_000L
private const val POSITION_REFRESH_MS = 500L

@UnstableApi
class PlaybackControllerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(appContext)

    private var controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
    ).buildAsync()

    private var controller: MediaController? = null
    private var positionJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishState()
        }
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        controllerFuture.addListener(
            {
                controller = controllerFuture.get().also {
                    it.addListener(listener)
                    publishState()
                    startPositionUpdates()
                }
            },
            mainExecutor
        )
    }

    fun playEpisode(episode: PodcastEpisode) {
        ensureServiceStarted()
        val activeController = controller ?: return

        val currentUri = activeController.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUri == episode.audioUrl) {
            activeController.play()
            return
        }

        activeController.setMediaItem(
            MediaItem.Builder()
                .setUri(episode.audioUrl)
                .setMediaId(episode.audioUrl)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(episode.title)
                        .setArtist("Easy Catalan")
                        .setAlbumTitle("FreePod")
                        .setDescription(episode.description)
                        .build()
                )
                .build()
        )
        activeController.prepare()
        activeController.playWhenReady = true
        activeController.play()
        publishState()
    }

    fun togglePlayPause() {
        val activeController = controller ?: return
        if (activeController.isPlaying) activeController.pause() else activeController.play()
    }

    fun seekBack() {
        val activeController = controller ?: return
        val target = (activeController.currentPosition - SEEK_MS).coerceAtLeast(0L)
        activeController.seekTo(target)
    }

    fun seekForward() {
        val activeController = controller ?: return
        val duration = activeController.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        val target = (activeController.currentPosition + SEEK_MS).coerceAtMost(duration)
        activeController.seekTo(target)
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    private fun publishState() {
        val activeController = controller
        if (activeController == null) {
            _uiState.value = PlayerUiState(isConnected = false)
            return
        }

        _uiState.value = PlayerUiState(
            isConnected = true,
            isPlaying = activeController.isPlaying,
            currentPositionMs = activeController.currentPosition.coerceAtLeast(0L),
            durationMs = activeController.duration.takeIf { it > 0 } ?: 0L,
            title = activeController.mediaMetadata.title?.toString().orEmpty(),
            description = activeController.mediaMetadata.description?.toString()
        )
    }

    private fun startPositionUpdates() {
        if (positionJob != null) return
        positionJob = viewModelScope.launch {
            while (true) {
                publishState()
                delay(POSITION_REFRESH_MS)
            }
        }
    }

    private fun ensureServiceStarted() {
        val serviceIntent = Intent(appContext, PlaybackService::class.java)
        appContext.startForegroundService(serviceIntent)
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        positionJob?.cancel()
        MediaController.releaseFuture(controllerFuture)
        controller = null
        super.onCleared()
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return PlaybackControllerViewModel(application) as T
                }
            }
        }
    }
}

data class PlayerUiState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val title: String = "",
    val description: String? = null
)
