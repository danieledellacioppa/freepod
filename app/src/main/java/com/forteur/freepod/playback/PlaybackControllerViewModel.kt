package com.forteur.freepod.playback

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.forteur.freepod.util.EXTRA_FEED_URL
import com.forteur.freepod.util.EXTRA_IMAGE_URL
import com.forteur.freepod.util.EXTRA_PLAY_REQUEST_ID
import com.forteur.freepod.util.EXTRA_PODCAST_TITLE
import com.forteur.freepod.util.LOG_TAG_CONTROLLER
import com.forteur.freepod.util.playerStateToString
import com.forteur.freepod.util.safeMediaItemSummary
import com.forteur.freepod.util.safeMediaMetadataSummary
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
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(
                LOG_TAG_CONTROLLER,
                "Controller onPlaybackStateChanged | state=${playerStateToString(playbackState)}($playbackState), current=${safeMediaItemSummary(controller?.currentMediaItem)}"
            )
            publishState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(
                LOG_TAG_CONTROLLER,
                "Controller onIsPlayingChanged | isPlaying=$isPlaying, playbackState=${controller?.playbackState?.let(::playerStateToString)}"
            )
            publishState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d(
                LOG_TAG_CONTROLLER,
                "Controller onMediaItemTransition | reason=$reason, item=${safeMediaItemSummary(mediaItem)}"
            )
            publishState()
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(
                LOG_TAG_CONTROLLER,
                "Controller onPlayerError | code=${error.errorCodeName}, message=${error.message}",
                error
            )
            publishState()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            Log.d(
                LOG_TAG_CONTROLLER,
                "Controller onEvents | events=$events, playbackState=${playerStateToString(player.playbackState)}, isPlaying=${player.isPlaying}, current=${safeMediaItemSummary(player.currentMediaItem)}"
            )
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
                    Log.d(
                        LOG_TAG_CONTROLLER,
                        "MediaController connected, current=${safeMediaItemSummary(it.currentMediaItem)}"
                    )
                    publishState()
                    startPositionUpdates()
                }
            },
            mainExecutor
        )
    }

    fun playEpisode(episode: PodcastEpisode, playRequestId: String) {
        ensureServiceStarted()
        val activeController = controller ?: return
        Log.d(
            LOG_TAG_CONTROLLER,
            "playEpisode called | playRequestId=$playRequestId, title=${episode.title}, audioUrl=${episode.audioUrl}, imageUrl=null, feedUrl=unknown"
        )

        val currentUri = activeController.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUri == episode.audioUrl) {
            Log.d(
                LOG_TAG_CONTROLLER,
                "Requested episode already current -> calling play() | playRequestId=$playRequestId, currentUri=$currentUri"
            )
            activeController.play()
            return
        }

        val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtist("FreePod")
            .setAlbumTitle("FreePod")
            .setDescription(episode.description)
            .setExtras(
                Bundle().apply {
                    putString(EXTRA_PLAY_REQUEST_ID, playRequestId)
                    putString(EXTRA_FEED_URL, null)
                    putString(EXTRA_PODCAST_TITLE, null)
                    putString(EXTRA_IMAGE_URL, null)
                }
            )
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(episode.audioUrl)
            .setMediaId(episode.audioUrl)
            .setMediaMetadata(mediaMetadata)
            .build()
        Log.d(
            LOG_TAG_CONTROLLER,
            "Built MediaItem | playRequestId=$playRequestId, uri=${mediaItem.localConfiguration?.uri}, mediaId=${mediaItem.mediaId}, metadata=${safeMediaMetadataSummary(mediaItem.mediaMetadata)}"
        )

        activeController.setMediaItem(mediaItem)
        Log.d(LOG_TAG_CONTROLLER, "setMediaItem() sent to service | playRequestId=$playRequestId")
        Log.d(LOG_TAG_CONTROLLER, "Calling prepare() | playRequestId=$playRequestId")
        activeController.prepare()
        Log.d(LOG_TAG_CONTROLLER, "Calling playWhenReady=true | playRequestId=$playRequestId")
        activeController.playWhenReady = true
        Log.d(LOG_TAG_CONTROLLER, "Calling play() | playRequestId=$playRequestId")
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
            playbackState = playerStateToString(activeController.playbackState),
            currentPositionMs = activeController.currentPosition.coerceAtLeast(0L),
            durationMs = activeController.duration.takeIf { it > 0 } ?: 0L,
            title = activeController.mediaMetadata.title?.toString().orEmpty(),
            description = activeController.mediaMetadata.description?.toString(),
            currentMediaId = activeController.currentMediaItem?.mediaId,
            currentMediaItemSummary = safeMediaItemSummary(activeController.currentMediaItem)
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
        Log.d(LOG_TAG_CONTROLLER, "ensureServiceStarted -> startForegroundService(${PlaybackService::class.java.simpleName})")
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
    val playbackState: String = "IDLE",
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val title: String = "",
    val description: String? = null,
    val currentMediaId: String? = null,
    val currentMediaItemSummary: String = "mediaItem=null"
)
